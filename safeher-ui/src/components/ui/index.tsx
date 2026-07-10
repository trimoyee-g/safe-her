import React from 'react'
import clsx from 'clsx'

// ── Button ────────────────────────────────────────────────────────────────────

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  fullWidth?: boolean
}

const btnVariant = {
  primary:   'bg-brand-400 hover:bg-brand-200 text-brand-950 border-transparent',
  secondary: 'bg-gray-900 hover:bg-gray-800 text-gray-200 border-gray-700',
  ghost:     'bg-transparent hover:bg-gray-800 text-gray-300 border-transparent',
  danger:    'bg-red-600 hover:bg-red-500 text-white border-transparent',
}
const btnSize = {
  sm: 'px-3 py-1.5 text-xs rounded-md',
  md: 'px-4 py-2 text-sm rounded-lg',
  lg: 'px-6 py-3 text-base rounded-xl',
}

export function Button({
  variant = 'primary', size = 'md', loading, fullWidth, children, className, disabled, ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      disabled={disabled || loading}
      className={clsx(
        'inline-flex items-center justify-center gap-2 font-medium border transition-colors focus:outline-none focus:ring-2 focus:ring-brand-400 focus:ring-offset-1 focus:ring-offset-gray-950 disabled:opacity-50 disabled:cursor-not-allowed',
        btnVariant[variant],
        btnSize[size],
        fullWidth && 'w-full',
        className
      )}
    >
      {loading && <Spinner size={size === 'sm' ? 'sm' : 'md'} />}
      {children}
    </button>
  )
}

// ── Input ─────────────────────────────────────────────────────────────────────

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, className, id, ...props }, ref) => {
    const inputId = id ?? label?.toLowerCase().replace(/\s+/g, '-')
    return (
      <div className="flex flex-col gap-1">
        {label && (
          <label htmlFor={inputId} className="text-sm font-medium text-gray-300">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={clsx(
            'w-full px-3 py-2 text-sm border rounded-lg bg-gray-900 text-gray-100 placeholder-gray-500',
            'focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent',
            'disabled:bg-gray-900/50 disabled:text-gray-600 disabled:cursor-not-allowed',
            error ? 'border-red-500' : 'border-gray-700',
            className
          )}
          {...props}
        />
        {error && <p className="text-xs text-red-400">{error}</p>}
        {hint && !error && <p className="text-xs text-gray-500">{hint}</p>}
      </div>
    )
  }
)
Input.displayName = 'Input'

// ── Textarea ──────────────────────────────────────────────────────────────────

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
  hint?: string
}

export const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, error, hint, className, id, ...props }, ref) => {
    const inputId = id ?? label?.toLowerCase().replace(/\s+/g, '-')
    return (
      <div className="flex flex-col gap-1">
        {label && (
          <label htmlFor={inputId} className="text-sm font-medium text-gray-300">{label}</label>
        )}
        <textarea
          ref={ref}
          id={inputId}
          className={clsx(
            'w-full px-3 py-2 text-sm border rounded-lg bg-gray-900 text-gray-100 placeholder-gray-500 resize-none',
            'focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent',
            error ? 'border-red-500' : 'border-gray-700',
            className
          )}
          {...props}
        />
        {error && <p className="text-xs text-red-400">{error}</p>}
        {hint && !error && <p className="text-xs text-gray-500">{hint}</p>}
      </div>
    )
  }
)
Textarea.displayName = 'Textarea'

// ── Spinner ───────────────────────────────────────────────────────────────────

export function Spinner({ size = 'md', className }: { size?: 'sm' | 'md' | 'lg'; className?: string }) {
  const sz = { sm: 'w-3 h-3', md: 'w-5 h-5', lg: 'w-8 h-8' }
  return (
    <svg
      className={clsx('animate-spin text-current', sz[size], className)}
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  )
}

// ── Tag ───────────────────────────────────────────────────────────────────────

interface TagProps {
  label: string
  positive?: boolean
  selected?: boolean
  onClick?: () => void
  size?: 'sm' | 'md'
}

export function Tag({ label, positive = true, selected, onClick, size = 'md' }: TagProps) {
  const base = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-xs'
  if (selected !== undefined) {
    return (
      <button
        type="button"
        onClick={onClick}
        className={clsx(
          'rounded-full font-medium transition-colors border',
          base,
          selected
            ? 'bg-brand-400/15 text-brand-200 border-brand-400'
            : 'bg-gray-800 text-gray-400 border-transparent hover:bg-gray-700'
        )}
      >
        {label}
      </button>
    )
  }
  return (
    <span className={clsx(
      'rounded-full font-medium',
      base,
      positive ? 'bg-brand-400/15 text-brand-200' : 'bg-red-500/15 text-red-300'
    )}>
      {label}
    </span>
  )
}

// ── Avatar ────────────────────────────────────────────────────────────────────

interface AvatarProps {
  name?: string
  src?: string
  size?: 'sm' | 'md' | 'lg'
  anonymous?: boolean
}

const avatarSize = { sm: 'w-7 h-7 text-xs', md: 'w-9 h-9 text-sm', lg: 'w-12 h-12 text-base' }

export function Avatar({ name, src, size = 'md', anonymous }: AvatarProps) {
  const initials = name
    ? name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase()
    : '?'

  if (anonymous) {
    return (
      <div className={clsx('rounded-full bg-gray-800 flex items-center justify-center flex-shrink-0', avatarSize[size])}>
        <svg className="w-4 h-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
        </svg>
      </div>
    )
  }

  if (src) {
    return <img src={src} alt={name} className={clsx('rounded-full object-cover flex-shrink-0', avatarSize[size])} />
  }

  return (
    <div className={clsx('rounded-full bg-brand-400/20 text-brand-200 flex items-center justify-center font-medium flex-shrink-0', avatarSize[size])}>
      {initials}
    </div>
  )
}

// ── Toggle ────────────────────────────────────────────────────────────────────

interface ToggleProps {
  checked: boolean
  onChange: (checked: boolean) => void
  label?: string
  description?: string
  disabled?: boolean
}

export function Toggle({ checked, onChange, label, description, disabled }: ToggleProps) {
  return (
    <label className={clsx('flex items-center justify-between gap-3', disabled && 'opacity-50 cursor-not-allowed')}>
      {(label || description) && (
        <div className="flex-1">
          {label && <p className="text-sm font-medium text-gray-200">{label}</p>}
          {description && <p className="text-xs text-gray-500 mt-0.5">{description}</p>}
        </div>
      )}
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={clsx(
          'relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-brand-400 focus:ring-offset-1 focus:ring-offset-gray-950',
          checked ? 'bg-brand-400' : 'bg-gray-700'
        )}
      >
        <span className={clsx(
          'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
          checked ? 'translate-x-6' : 'translate-x-1'
        )} />
      </button>
    </label>
  )
}

// ── Select ────────────────────────────────────────────────────────────────────

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  options: { value: string; label: string }[]
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, options, className, id, ...props }, ref) => {
    const inputId = id ?? label?.toLowerCase().replace(/\s+/g, '-')
    return (
      <div className="flex flex-col gap-1">
        {label && <label htmlFor={inputId} className="text-sm font-medium text-gray-300">{label}</label>}
        <select
          ref={ref}
          id={inputId}
          className={clsx(
            'w-full px-3 py-2 text-sm border rounded-lg bg-gray-900 text-gray-100',
            'focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent',
            error ? 'border-red-500' : 'border-gray-700',
            className
          )}
          {...props}
        >
          {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
        {error && <p className="text-xs text-red-400">{error}</p>}
      </div>
    )
  }
)
Select.displayName = 'Select'

// ── Empty state ───────────────────────────────────────────────────────────────

export function EmptyState({ title, description, action }: {
  title: string
  description?: string
  action?: React.ReactNode
}) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center px-4">
      <div className="w-16 h-16 bg-gray-900 rounded-full flex items-center justify-center mb-4">
        <svg className="w-8 h-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
            d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h3 className="text-base font-medium text-gray-100 mb-1">{title}</h3>
      {description && <p className="text-sm text-gray-500 mb-4 max-w-xs">{description}</p>}
      {action}
    </div>
  )
}

// ── Error state ───────────────────────────────────────────────────────────────

export function ErrorState({ message, retry }: { message?: string; retry?: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center px-4">
      <div className="w-16 h-16 bg-red-500/10 rounded-full flex items-center justify-center mb-4">
        <svg className="w-8 h-8 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <p className="text-sm text-gray-400 mb-3">{message ?? 'Something went wrong'}</p>
      {retry && <Button variant="secondary" size="sm" onClick={retry}>Try again</Button>}
    </div>
  )
}
