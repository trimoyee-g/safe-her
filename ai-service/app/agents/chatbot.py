"""Agent 5: Safety Chatbot (LangGraph Agentic RAG)

State machine:
  START
    ↓
  generate  ─── calls Ollama with full place context + conversation history
    ↓
  grade     ─── lightweight grader checks if response is grounded in context
    ↓ GROUNDED or max retries reached
  END
    ↓ NOT_GROUNDED (up to MAX_RETRIES times)
  (back to generate)

The grader uses phi3:mini (fast) for near-zero overhead.
If no specific place context exists, the grader always passes (general advice is fine).
"""
import logging
from typing import List, Optional, TypedDict
from uuid import UUID

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_ollama import ChatOllama
from langgraph.graph import END, START, StateGraph

from app.config import get_settings
from app.clients import place_client, rating_client
from app.models.requests import ChatMessage
from app.models.responses import ChatResponse
from app import vector_store

logger = logging.getLogger(__name__)

MAX_HISTORY_TURNS = 10
MAX_RETRIES = 2

_SYSTEM_PROMPT = """You are SafeGuide, a friendly and knowledgeable safety assistant on safeher –
a platform helping people, especially women, make informed decisions about
the safety of public places.

You have access to real crowdsourced safety reviews and ratings from our platform.
When answering questions:

1. Ground your answers in the review data provided. Quote specific concerns reviewers mentioned.
2. Be honest about uncertainty. If there isn't enough data, say so.
3. Give practical, actionable advice (which exit to use, what time is safer, what to be aware of).
4. Never downplay genuine safety concerns to seem positive.
5. If you don't have data about a place, encourage the user to add a review.
6. Keep responses concise – 2–4 short paragraphs maximum.
7. For emergencies, always direct users to call 100 (India police) or 112 (emergency).

Context about the places relevant to this conversation:
{place_context}"""

_GRADER_PROMPT = """Does this response stay grounded in the provided context?
Answer GROUNDED if:
  - the response gives general safety advice (no context needed), OR
  - the specific claims are supported by the context below.
Answer NOT_GROUNDED only if the response makes specific factual claims
clearly absent from or contradicted by the context.

Context (excerpt):
{context}

Response:
{response}

Reply with exactly one word: GROUNDED or NOT_GROUNDED"""


# ── LangGraph state ───────────────────────────────────────────────────────────

class _State(TypedDict):
    messages: List[dict]   # list of {role, content}
    place_context: str
    generation: str
    retries: int


# ── Graph nodes ───────────────────────────────────────────────────────────────

async def _generate(state: _State) -> _State:
    settings = get_settings()
    system_text = _SYSTEM_PROMPT.format(place_context=state["place_context"])

    lc_messages = [SystemMessage(content=system_text)]
    for m in state["messages"]:
        if m["role"] == "user":
            lc_messages.append(HumanMessage(content=m["content"]))
        else:
            lc_messages.append(AIMessage(content=m["content"]))

    llm = ChatOllama(
        model=settings.ollama_model_chatbot,
        base_url=settings.ollama_url,
        num_predict=512,
        temperature=0.1,
    )
    try:
        response = await llm.ainvoke(lc_messages)
        return {**state, "generation": response.content, "retries": state.get("retries", 0) + 1}
    except Exception as e:
        logger.error(f"Chatbot generation failed: {e}")
        return {**state, "generation": "I'm temporarily unavailable. Please try again.", "retries": 99}


async def _grade(state: _State) -> str:
    """Conditional edge: returns 'end' or 'retry'."""
    if state.get("retries", 0) >= MAX_RETRIES:
        return "end"

    # No specific place data → general advice is always fine
    if "No specific places" in state["place_context"]:
        return "end"

    settings = get_settings()
    grader_llm = ChatOllama(
        model=settings.ollama_model_moderation,  # phi3:mini – fast
        base_url=settings.ollama_url,
        num_predict=5,
        temperature=0.0,
    )
    prompt_text = _GRADER_PROMPT.format(
        context=state["place_context"][:600],
        response=state["generation"][:400],
    )
    try:
        result = await grader_llm.ainvoke([HumanMessage(content=prompt_text)])
        if "NOT_GROUNDED" in result.content.upper():
            logger.debug("Response graded NOT_GROUNDED – retrying generation")
            return "retry"
    except Exception as e:
        logger.warning(f"Grader failed (defaulting to GROUNDED): {e}")

    return "end"


# ── Build graph (once at module load) ─────────────────────────────────────────

def _build_graph():
    g = StateGraph(_State)
    g.add_node("generate", _generate)
    g.add_edge(START, "generate")
    g.add_conditional_edges("generate", _grade, {"end": END, "retry": "generate"})
    return g.compile()


_graph = _build_graph()


# ── Public API ────────────────────────────────────────────────────────────────

async def chat(
    message: str,
    history: Optional[List[ChatMessage]],
    place_ids: Optional[List[UUID]],
    caller_user_id: Optional[UUID],
) -> ChatResponse:
    if not place_ids:
        place_ids = await vector_store.search_places(message)

    place_context = await _build_place_context(place_ids)

    history = history or []
    trimmed = history[-(MAX_HISTORY_TURNS * 2):]
    messages = [{"role": m.role.lower(), "content": m.content} for m in trimmed]
    messages.append({"role": "user", "content": message})

    initial: _State = {
        "messages": messages,
        "place_context": place_context,
        "generation": "",
        "retries": 0,
    }

    final = await _graph.ainvoke(initial)

    logger.debug(f"Chatbot response for user [{caller_user_id}]: {len(final['generation'])} chars")
    return ChatResponse(
        message=final["generation"].strip(),
        suggestedPlaceIds=place_ids or [],
    )


async def _build_place_context(place_ids: Optional[List[UUID]]) -> str:
    if not place_ids:
        return "No specific places referenced yet. Answer based on general safety principles."

    parts = []
    for place_id in place_ids:
        try:
            place = await place_client.get_place(place_id)
            ratings_data = await rating_client.get_ratings_by_place(place_id, "NEWEST", 0, 20)
            reviews = ratings_data.get("content", [])

            section = [
                f"\n## {place.get('name')} ({place.get('category')})",
                f"Location: {place.get('city')}, {place.get('country')}",
                f"Safety score: {place.get('safetyScore', 0):.1f}/5 "
                f"based on {place.get('totalRatings', 0)} reviews",
                "",
            ]

            snippets = []
            for r in reviews:
                body = (r.get("body") or "").strip()
                if not body:
                    continue
                tags = r.get("tags") or []
                tag_str = f"[{', '.join(tags)}] " if tags else ""
                snippet = body[:250] + "…" if len(body) > 250 else body
                snippets.append(f"- Score {r.get('score')}/5 {tag_str}: {snippet}")
                if len(snippets) >= 10:
                    break

            section.append("Recent reviews:\n" + "\n".join(snippets) if snippets
                           else "No detailed review text available yet.")
            parts.append("\n".join(section))

        except Exception as e:
            logger.warning(f"Could not fetch context for place [{place_id}]: {e}")
            parts.append(f"\n## Place {place_id}: data unavailable")

    return "\n".join(parts)
