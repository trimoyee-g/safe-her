import { useState, useRef, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { aiApi } from '@/api/ai.api'
import { placesApi } from '@/api/places.api'
import { queryKeys } from '@/lib/queryClient'
import { Button, Spinner } from '@/components/ui'
import { ScoreBadge } from '@/components/ui/ScoreBadge'
import { CATEGORY_ICONS, CATEGORY_LABELS } from '@/utils'
import type { ChatMessage } from '@/types'
import clsx from 'clsx'

const SUGGESTIONS = [
  'Is this place safe at night?',
  'What do people say about the lighting?',
  'Is it safe to go alone?',
  'What should I be aware of?',
]

export function ChatbotPage() {
  const { placeId } = useParams<{ placeId?: string }>()
  const navigate = useNavigate()

  const [messages, setMessages] = useState<ChatMessage[]>([])
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
        history: messages,
      }),
    onSuccess: (response) => {
      setMessages(prev => [
        ...prev,
        { role: 'assistant', content: response.message },
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

  return (
    <div className="flex flex-col h-[calc(100vh-56px)] max-w-2xl mx-auto">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-gray-100 bg-white">
        <button
          onClick={() => navigate(placeId ? `/place/${placeId}` : '/')}
          className="text-gray-400 hover:text-gray-600 transition-colors"
          aria-label="Go back"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        <div className="flex items-center gap-2 flex-1 min-w-0">
          <div className="w-8 h-8 bg-brand-400 rounded-lg flex items-center justify-center flex-shrink-0">
            <svg className="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
            </svg>
          </div>
          <div className="min-w-0">
            <p className="text-sm font-medium text-gray-900">SafeGuide</p>
            <p className="text-xs text-gray-400 truncate">
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
        <div className="px-4 py-2 bg-brand-50 border-b border-brand-100">
          <div className="flex items-center gap-2 text-xs text-brand-700">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span className="font-medium truncate">{place.name}</span>
            <span className="text-brand-400">·</span>
            <span>{CATEGORY_LABELS[place.category]}</span>
            <span className="text-brand-400">·</span>
            <span>{place.totalRatings} reviews</span>
          </div>
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
        {/* Welcome message */}
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center py-8">
            <div className="w-14 h-14 bg-brand-50 rounded-2xl flex items-center justify-center mb-4">
              <svg className="w-7 h-7 text-brand-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round"
                  d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <h2 className="text-base font-semibold text-gray-800 mb-1">SafeGuide</h2>
            <p className="text-sm text-gray-500 max-w-xs mb-6">
              Ask me anything about safety at{' '}
              {place ? <span className="font-medium text-gray-700">{place.name}</span> : 'any place'}.
              I'll answer based on real reviews from the community.
            </p>

            {/* Suggestion chips */}
            <div className="flex flex-wrap justify-center gap-2">
              {SUGGESTIONS.map(s => (
                <button
                  key={s}
                  onClick={() => handleSend(s)}
                  className="text-xs px-3 py-1.5 bg-white border border-gray-200 rounded-full text-gray-600 hover:border-brand-400 hover:text-brand-700 transition-colors"
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
            {msg.role === 'assistant' && (
              <div className="w-7 h-7 bg-brand-400 rounded-lg flex items-center justify-center mr-2 mt-0.5 flex-shrink-0">
                <svg className="w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round"
                    d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
            )}
            <div
              className={clsx(
                'max-w-xs sm:max-w-sm rounded-2xl px-4 py-2.5 text-sm leading-relaxed',
                msg.role === 'user'
                  ? 'bg-brand-400 text-white rounded-tr-sm'
                  : 'bg-white border border-gray-100 text-gray-800 rounded-tl-sm shadow-sm'
              )}
            >
              {msg.content.split('\n').map((line, j) => (
                <span key={j}>
                  {line}
                  {j < msg.content.split('\n').length - 1 && <br />}
                </span>
              ))}
            </div>
          </div>
        ))}

        {/* Typing indicator */}
        {isPending && (
          <div className="flex justify-start">
            <div className="w-7 h-7 bg-brand-400 rounded-lg flex items-center justify-center mr-2 flex-shrink-0">
              <svg className="w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round"
                  d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <div className="bg-white border border-gray-100 rounded-2xl rounded-tl-sm px-4 py-3 shadow-sm">
              <div className="flex items-center gap-1">
                <div className="w-1.5 h-1.5 bg-gray-300 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                <div className="w-1.5 h-1.5 bg-gray-300 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                <div className="w-1.5 h-1.5 bg-gray-300 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* Emergency notice */}
      <div className="px-4 py-1.5 bg-red-50 border-t border-red-100">
        <p className="text-xs text-red-600 text-center">
          For emergencies call <strong>112</strong> (India) or <strong>100</strong> (Police)
        </p>
      </div>

      {/* Input */}
      <div className="px-4 py-3 bg-white border-t border-gray-100">
        <div className="flex items-center gap-2">
          <input
            ref={inputRef}
            type="text"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask about safety here..."
            disabled={isPending}
            className="flex-1 px-4 py-2.5 text-sm border border-gray-200 rounded-xl bg-gray-50 focus:outline-none focus:ring-2 focus:ring-brand-400 focus:bg-white disabled:opacity-50 transition"
          />
          <button
            onClick={() => handleSend()}
            disabled={!input.trim() || isPending}
            className="w-10 h-10 bg-brand-400 hover:bg-brand-600 disabled:bg-gray-200 text-white rounded-xl flex items-center justify-center transition-colors flex-shrink-0"
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
