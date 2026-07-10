import clsx from 'clsx'
import type { ChatConfidence } from '@/types'

const CONFIDENCE_META: Record<ChatConfidence, { label: string; className: string }> = {
  high:    { label: 'High confidence',           className: 'bg-emerald-500/15 text-emerald-300' },
  medium:  { label: 'Medium confidence',          className: 'bg-amber-500/15 text-amber-300' },
  low:     { label: 'Low confidence · web only',  className: 'bg-red-500/15 text-red-300' },
  no_data: { label: 'No specific data found',     className: 'bg-gray-800 text-gray-400' },
}

export function ConfidenceBadge({ confidence }: { confidence: ChatConfidence }) {
  const meta = CONFIDENCE_META[confidence] ?? CONFIDENCE_META.no_data
  return (
    <span className={clsx('inline-flex items-center w-fit rounded-full px-2 py-0.5 text-[11px] font-medium', meta.className)}>
      {meta.label}
    </span>
  )
}
