import clsx from 'clsx'

const LABELS = ['', 'Felt very unsafe', 'Felt unsafe', 'Felt okay', 'Felt safe', 'Felt very safe']
const COLORS = ['', 'text-red-400', 'text-orange-400', 'text-amber-400', 'text-brand-300', 'text-brand-200']
const BG     = ['', 'bg-red-500/15 border-red-400', 'bg-orange-500/15 border-orange-400',
                    'bg-amber-500/15 border-amber-400', 'bg-brand-400/15 border-brand-400', 'bg-brand-400/20 border-brand-300']

interface StarPickerProps {
  value: number
  onChange: (score: number) => void
  error?: string
}

export function StarPicker({ value, onChange, error }: StarPickerProps) {
  return (
    <div>
      <div className="flex justify-center gap-3" role="radiogroup" aria-label="Safety score">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            type="button"
            role="radio"
            aria-checked={value === n}
            aria-label={`${n} – ${LABELS[n]}`}
            onClick={() => onChange(n)}
            className={clsx(
              'flex flex-col items-center gap-1 transition-transform active:scale-95'
            )}
          >
            <div className={clsx(
              'w-12 h-12 rounded-2xl border-2 flex items-center justify-center transition-all',
              value === n
                ? BG[n]
                : 'bg-gray-900 border-gray-700 hover:border-gray-600'
            )}>
              <StarIcon
                filled={n <= value}
                className={clsx('w-6 h-6 transition-colors', n <= value ? COLORS[value] : 'text-gray-600')}
              />
            </div>
            <span className={clsx('text-xs font-medium', n === value ? COLORS[n] : 'text-gray-500')}>
              {n}
            </span>
          </button>
        ))}
      </div>
      {value > 0 && (
        <p className={clsx('text-center text-sm font-medium mt-2', COLORS[value])}>
          {LABELS[value]}
        </p>
      )}
      {error && <p className="text-center text-xs text-red-400 mt-1">{error}</p>}
    </div>
  )
}

function StarIcon({ filled, className }: { filled: boolean; className?: string }) {
  return filled ? (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  ) : (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
    </svg>
  )
}
