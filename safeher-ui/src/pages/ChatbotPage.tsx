import { useState, useRef, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { aiApi } from '@/api/ai.api'
import { placesApi } from '@/api/places.api'
import { queryKeys } from '@/lib/queryClient'
import { Spinner } from '@/components/ui'
import { ScoreBadge } from '@/components/ui/ScoreBadge'
import { ConfidenceBadge } from '@/components/chat/ConfidenceBadge'
import { SourceChips } from '@/components/chat/SourceChips'
import { CATEGORY_ICONS, CATEGORY_LABELS } from '@/utils'
import type { ChatConfidence, ChatSource } from '@/types'
import clsx from 'clsx'

interface DisplayMessage {
  role: 'user' | 'assistant'
  content: string
  confidence?: ChatConfidence
  sources?: ChatSource[]
}

const PLACE_SUGGESTIONS = [
  'Is this place safe at night?',
  'What do people say about the lighting?',
  'Is it safe to go alone?',
  'What should I be aware of?',
]

const GENERAL_SUGGESTIONS = [
  'Which areas are unsafe at night in Bangalore?',
  'Is Blue Tokai Cafe in Kolkata well lit?',
  'What should I know about walking alone in a new city?',
  'Which metro stations feel safest at night?',
]

function ShieldIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  )
}

export function ChatbotPage() {
  const { placeId } = useParams<{ placeId?: string }>()
  const navigate = useNavigate()

  const [messages, setMessages] = useState<DisplayMessage[]>([])
  const [input, setInput] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const { data: place } = useQuery({
    queryKey: queryKeys.places.detail(placeId!),
    queryFn: () => placesApi.getById(placeId!),
    enabled: !!placeId,
  })

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const { mutate: sendMessage, isPending } = useMutation({
    mutationFn: (message: string) =>
      aiApi.chat({
        message,
        placeIds: placeId ? [placeId] : undefined,
        history: messages.map(({ role, content }) => ({ role, content })),
      }),
    onSuccess: (response) => {
      setMessages(prev => [
        ...prev,
        {
          role: 'assistant',
          content: response.message,
          confidence: response.confidence,
          sources: response.sources,
        },
      ])
    },
    onError: () => {
      setMessages(prev => [
        ...prev,
        { role: 'assistant', content: "I'm having trouble connecting right now. Please try again." },
      ])
    },
  })

  const handleSend = (text?: string) => {
    const message = (text ?? input).trim()
    if (!message || isPending) return

    setMessages(prev => [...prev, { role: 'user', content: message }])
    setInput('')
    sendMessage(message)
    inputRef.current?.focus()
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const suggestions = place ? PLACE_SUGGESTIONS : GENERAL_SUGGESTIONS

  return (
    <div className="flex flex-col h-[calc(100vh-56px)] max-w-2xl mx-auto bg-gray-950">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-gray-800">
        <button
          onClick={() => navigate(placeId ? `/place/${placeId}` : '/')}
          className="text-gray-500 hover:text-gray-300 transition-colors"
          aria-label="Go back"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        <div className="flex items-center gap-2 flex-1 min-w-0">
          <div className="w-8 h-8 bg-brand-400 rounded-lg flex items-center justify-center flex-shrink-0">
            <ShieldIcon className="w-4 h-4 text-brand-950" />
          </div>
          <div className="min-w-0">
            <p className="text-sm font-medium text-gray-100">SafeGuide</p>
            <p className="text-xs text-gray-500 truncate">
              {place ? `Asking about ${place.name}` : 'AI safety assistant'}
            </p>
          </div>
        </div>

        {place && (
          <div className="flex items-center gap-2 flex-shrink-0">
            <span className="text-sm">{CATEGORY_ICONS[place.category]}</span>
            <ScoreBadge score={place.safetyScore} size="sm" />
          </div>
        )}
      </div>

      {/* Place context pill */}
      {place && (
        <div className="px-4 py-2 bg-brand-400/10 border-b border-brand-400/20">
          <div className="flex items-center gap-2 text-xs text-brand-200">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span className="font-medium truncate">{place.name}</span>
            <span className="text-brand-400/60">·</span>
            <span>{CATEGORY_LABELS[place.category]}</span>
            <span className="text-brand-400/60">·</span>
            <span>{place.totalRatings} reviews</span>
          </div>
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
        {/* Welcome message */}
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center py-8">
            <div className="w-14 h-14 bg-brand-400/15 rounded-2xl flex items-center justify-center mb-4">
              <ShieldIcon className="w-7 h-7 text-brand-300" />
            </div>
            <h2 className="text-base font-semibold text-gray-100 mb-1">SafeGuide</h2>
            <p className="text-sm text-gray-400 max-w-xs mb-6">
              Ask me anything about safety at{' '}
              {place ? <span className="font-medium text-gray-200">{place.name}</span> : 'any place or area'}.
              I'll ground my answer in real reviews where we have them, and say clearly when I don't.
            </p>

            {/* Suggestion chips */}
            <div className="flex flex-wrap justify-center gap-2">
              {suggestions.map(s => (
                <button
                  key={s}
                  onClick={() => handleSend(s)}
                  className="text-xs px-3 py-1.5 bg-gray-900 border border-gray-700 rounded-full text-gray-300 hover:border-brand-400 hover:text-brand-200 hover:bg-brand-400/10 transition-colors"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Chat messages */}
        {messages.map((msg, i) => (
          <div
            key={i}
            className={clsx('flex', msg.role === 'user' ? 'justify-end' : 'justify-start')}
          >
            {msg.role === 'assistant' ? (
              <div className="flex items-start max-w-xs sm:max-w-sm">
                <div className="w-7 h-7 bg-brand-400 rounded-lg flex items-center justify-center mr-2 mt-0.5 flex-shrink-0">
                  <ShieldIcon className="w-3.5 h-3.5 text-brand-950" />
                </div>
                <div className="flex flex-col gap-1.5 min-w-0">
                  {msg.confidence && <ConfidenceBadge confidence={msg.confidence} />}
                  <div className="rounded-2xl rounded-tl-sm px-4 py-2.5 text-sm leading-relaxed bg-gray-900 border border-gray-800 text-gray-200">
                    {msg.content.split('\n').map((line, j) => (
                      <span key={j}>
                        {line}
                        {j < msg.content.split('\n').length - 1 && <br />}
                      </span>
                    ))}
                  </div>
                  {msg.sources && msg.sources.length > 0 && <SourceChips sources={msg.sources} />}
                </div>
              </div>
            ) : (
              <div className="max-w-xs sm:max-w-sm rounded-2xl px-4 py-2.5 text-sm leading-relaxed bg-gray-800 text-gray-100 rounded-tr-sm">
                {msg.content}
              </div>
            )}
          </div>
        ))}

        {/* Typing indicator */}
        {isPending && (
          <div className="flex justify-start">
            <div className="w-7 h-7 bg-brand-400 rounded-lg flex items-center justify-center mr-2 flex-shrink-0">
              <ShieldIcon className="w-3.5 h-3.5 text-brand-950" />
            </div>
            <div className="bg-gray-900 border border-gray-800 rounded-2xl rounded-tl-sm px-4 py-3">
              <div className="flex items-center gap-1">
                <div className="w-1.5 h-1.5 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                <div className="w-1.5 h-1.5 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                <div className="w-1.5 h-1.5 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* Emergency notice */}
      <div className="px-4 py-1.5 bg-red-500/10 border-t border-red-900/40">
        <p className="text-xs text-red-300 text-center">
          For emergencies call <strong>112</strong> (India) or <strong>100</strong> (Police)
        </p>
      </div>

      {/* Input */}
      <div className="px-4 py-3 bg-gray-950 border-t border-gray-800">
        <div className="flex items-center gap-2">
          <input
            ref={inputRef}
            type="text"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={place ? 'Ask about safety here…' : 'Ask about any place or area…'}
            disabled={isPending}
            className="flex-1 px-4 py-2.5 text-sm border border-gray-700 rounded-xl bg-gray-900 text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-400 focus:bg-gray-900 disabled:opacity-50 transition"
          />
          <button
            onClick={() => handleSend()}
            disabled={!input.trim() || isPending}
            className="w-10 h-10 bg-brand-400 hover:bg-brand-200 disabled:bg-gray-800 disabled:text-gray-600 text-brand-950 rounded-xl flex items-center justify-center transition-colors flex-shrink-0"
            aria-label="Send message"
          >
            {isPending ? (
              <Spinner size="sm" />
            ) : (
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
