import clsx from 'clsx'
import { scoreColor, formatScore } from '@/utils'

interface ScoreBadgeProps {
  score: number
  size?: 'sm' | 'md' | 'lg' | 'xl'
  showLabel?: boolean
  className?: string
}

const sizeClasses = {
  sm:  'w-8 h-8 text-xs font-semibold rounded-md',
  md:  'w-10 h-10 text-sm font-semibold rounded-lg',
  lg:  'w-14 h-14 text-xl font-semibold rounded-xl',
  xl:  'w-20 h-20 text-3xl font-semibold rounded-2xl',
}

export function ScoreBadge({ score, size = 'md', className }: ScoreBadgeProps) {
  return (
    <div
      className={clsx(
        'flex items-center justify-center flex-shrink-0',
        sizeClasses[size],
        scoreColor(score),
        className
      )}
      aria-label={`Safety score: ${formatScore(score)}`}
    >
      {formatScore(score)}
    </div>
  )
}
