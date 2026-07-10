import type { PlaceRatingSummary } from '@/types'
import clsx from 'clsx'

interface ScoreDistributionBarProps {
  summary: PlaceRatingSummary
}

const BAR_COLORS = ['', 'bg-red-500', 'bg-orange-400', 'bg-amber-400', 'bg-brand-300', 'bg-brand-500']

export function ScoreDistributionBar({ summary }: ScoreDistributionBarProps) {
  const max = Math.max(...Object.values(summary.scoreDistribution), 1)

  return (
    <div className="space-y-1.5" role="img" aria-label="Score distribution">
      {[5, 4, 3, 2, 1].map((score) => {
        const count = summary.scoreDistribution[score] ?? 0
        const pct = max > 0 ? (count / max) * 100 : 0
        return (
          <div key={score} className="flex items-center gap-2">
            <span className="text-xs text-gray-500 w-3 text-right">{score}</span>
            <div className="flex-1 bg-gray-800 rounded-full h-2 overflow-hidden">
              <div
                className={clsx('h-2 rounded-full transition-all', BAR_COLORS[score])}
                style={{ width: `${pct}%` }}
              />
            </div>
            <span className="text-xs text-gray-500 w-5 text-right">{count}</span>
          </div>
        )
      })}
    </div>
  )
}
