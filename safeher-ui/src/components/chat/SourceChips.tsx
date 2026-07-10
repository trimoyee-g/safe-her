import { Link } from 'react-router-dom'
import type { ChatSource } from '@/types'

function CheckIcon() {
  return (
    <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
    </svg>
  )
}

function ExternalLinkIcon() {
  return (
    <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
    </svg>
  )
}

const chipClass = 'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10.5px] font-medium transition-colors'

export function SourceChips({ sources }: { sources: ChatSource[] }) {
  if (!sources.length) return null

  return (
    <div className="flex flex-wrap gap-1.5">
      {sources.map((s, i) =>
        s.kind === 'review' ? (
          s.placeId ? (
            <Link key={i} to={`/place/${s.placeId}`} className={`${chipClass} bg-brand-400/15 text-brand-200 hover:bg-brand-400/25`}>
              <CheckIcon />
              {s.label}
            </Link>
          ) : (
            <span key={i} className={`${chipClass} bg-brand-400/15 text-brand-200`}>
              <CheckIcon />
              {s.label}
            </span>
          )
        ) : (
          <a
            key={i}
            href={s.url}
            target="_blank"
            rel="noopener noreferrer"
            className={`${chipClass} bg-gray-800 text-gray-400 hover:bg-gray-700 hover:text-gray-200`}
          >
            <ExternalLinkIcon />
            {s.label}
          </a>
        )
      )}
    </div>
  )
}
