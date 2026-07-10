"""Web Research Agent — bridges the cold-start gap.

When our own review corpus is thin (new city, new place, day-one launch),
this agent searches the open web for whatever public signal exists about a
place or area: news coverage, Google/Maps review snippets that show up in
search results, local forums, blogs. It never asserts a verdict itself —
it returns raw, attributed snippets. The synthesis agent (chatbot.py) is the
only place that turns those snippets + first-party review data into an
answer, and it is required to cite sources and state confidence.

Search backend: DuckDuckGo via `ddgs` — free, no API key, good enough for a
resume-scale demo. Swap in Tavily/Bing/SerpAPI in `_search_sync` for
production-grade recall without changing anything downstream, since callers
only depend on the `WebSource` shape.
"""
import asyncio
import logging
from dataclasses import dataclass
from typing import List, Optional

from app.config import get_settings

logger = logging.getLogger(__name__)


@dataclass
class WebSource:
    title: str
    url: str
    snippet: str


def _search_sync(query: str, max_results: int) -> List[WebSource]:
    """Blocking DuckDuckGo search — always called via asyncio.to_thread."""
    try:
        from ddgs import DDGS
    except ImportError:
        logger.warning("ddgs not installed — web research agent is a no-op")
        return []

    results: List[WebSource] = []
    try:
        with DDGS() as ddgs:
            for r in ddgs.text(query, max_results=max_results):
                title = (r.get("title") or "").strip()
                url = (r.get("href") or r.get("link") or "").strip()
                snippet = (r.get("body") or r.get("snippet") or "").strip()
                if not (title and url and snippet):
                    continue
                results.append(WebSource(title=title, url=url, snippet=snippet))
    except Exception as e:
        logger.warning("Web search failed for query=%r: %s", query, e)
        return []
    return results


async def search(
    place_name: Optional[str],
    city: Optional[str],
    user_message: str,
) -> List[WebSource]:
    """Search the web for safety-relevant signal about a place and/or area.

    Builds one focused query rather than reusing the raw user message verbatim —
    "is X well lit at night reviews safety" retrieves far better results than
    the conversational phrasing a user actually typed.
    """
    settings = get_settings()
    if not settings.web_search_enabled:
        return []

    parts = []
    if place_name and place_name != "Unknown":
        parts.append(place_name)
    if city:
        parts.append(city)
    parts.append("safety reviews well lit at night")
    if not place_name or place_name == "Unknown":
        # No specific place — lean on the user's own phrasing for area-level queries
        parts.append(user_message[:120])
    query = " ".join(parts).strip()

    try:
        results = await asyncio.wait_for(
            asyncio.to_thread(_search_sync, query, settings.web_search_max_results),
            timeout=settings.web_search_timeout_s,
        )
        logger.debug("Web research query=%r -> %d results", query, len(results))
        return results
    except asyncio.TimeoutError:
        logger.warning("Web search timed out for query=%r", query)
        return []
    except Exception as e:
        logger.warning("Web search error for query=%r: %s", query, e)
        return []
