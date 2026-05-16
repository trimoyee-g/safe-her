import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/api/auth.api'
import { useAuthStore } from '@/store/auth.store'
import { Input, Button } from '@/components/ui'

// ── Login ─────────────────────────────────────────────────────────────────────

const loginSchema = z.object({
  identifier: z.string().min(1, 'Email or username is required'),
  password:   z.string().min(1, 'Password is required'),
})
type LoginForm = z.infer<typeof loginSchema>

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string })?.from ?? '/'
  const login = useAuthStore(s => s.login)

  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  })

  const { mutate, isPending, error } = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      login(data)
      navigate(from, { replace: true })
    },
  })

  const errMsg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message

  return (
    <div className="min-h-[calc(100vh-56px)] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="w-12 h-12 bg-brand-400 rounded-2xl flex items-center justify-center mx-auto mb-3">
            <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <h1 className="text-xl font-semibold text-gray-900">Welcome back</h1>
          <p className="text-sm text-gray-500 mt-1">Sign in to your SafeHer account</p>
        </div>

        <form onSubmit={handleSubmit(d => mutate(d))} className="flex flex-col gap-4">
          <Input
            label="Email or username"
            placeholder="you@example.com"
            autoComplete="email"
            {...register('identifier')}
            error={errors.identifier?.message}
          />
          <div>
            <Input
              label="Password"
              type="password"
              placeholder="••••••••"
              autoComplete="current-password"
              {...register('password')}
              error={errors.password?.message}
            />
            <div className="flex justify-end mt-1">
              <Link to="/forgot-password" className="text-xs text-brand-600 hover:text-brand-700">
                Forgot password?
              </Link>
            </div>
          </div>

          {errMsg && (
            <p className="text-sm text-red-600 text-center">{errMsg}</p>
          )}

          <Button type="submit" loading={isPending} fullWidth size="lg">
            Sign in
          </Button>
        </form>

        <div className="mt-4 border-t border-gray-100 pt-4 text-center">
          <p className="text-sm text-gray-500">
            No account?{' '}
            <Link to="/register" className="text-brand-600 font-medium hover:text-brand-700">
              Register
            </Link>
          </p>
        </div>

        <div className="mt-3 text-center">
          <Link to="/" className="text-xs text-gray-400 hover:text-gray-600">
            Browse as guest →
          </Link>
        </div>
      </div>
    </div>
  )
}

// ── Register ──────────────────────────────────────────────────────────────────

const registerSchema = z.object({
  username:    z.string().min(3, 'At least 3 characters').max(50).regex(/^[a-zA-Z0-9_.-]+$/, 'Letters, digits, _ . - only'),
  email:       z.string().email('Invalid email'),
  password:    z.string().min(8, 'At least 8 characters')
    .regex(/(?=.*[a-z])/, 'Needs a lowercase letter')
    .regex(/(?=.*[A-Z])/, 'Needs an uppercase letter')
    .regex(/(?=.*\d)/, 'Needs a digit')
    .regex(/(?=.*[@$!%*?&#^()_+\-=])/, 'Needs a special character'),
  displayName: z.string().max(100).optional(),
  city:        z.string().max(100).optional(),
})
type RegisterForm = z.infer<typeof registerSchema>

export function RegisterPage() {
  const navigate = useNavigate()
  const login = useAuthStore(s => s.login)

  const { register, handleSubmit, formState: { errors } } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  })

  const { mutate, isPending, error } = useMutation({
    mutationFn: authApi.register,
    onSuccess: (data) => {
      login(data)
      navigate('/', { replace: true })
    },
  })

  const errMsg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message

  return (
    <div className="min-h-[calc(100vh-56px)] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="w-12 h-12 bg-brand-400 rounded-2xl flex items-center justify-center mx-auto mb-3">
            <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <h1 className="text-xl font-semibold text-gray-900">Join SafeHer</h1>
          <p className="text-sm text-gray-500 mt-1">Help make communities safer for everyone</p>
        </div>

        <form onSubmit={handleSubmit(d => mutate(d))} className="flex flex-col gap-4">
          <Input label="Username" placeholder="yourname"
            autoComplete="username" {...register('username')} error={errors.username?.message} />
          <Input label="Email" type="email" placeholder="you@example.com"
            autoComplete="email" {...register('email')} error={errors.email?.message} />
          <Input label="Password" type="password" placeholder="••••••••"
            autoComplete="new-password" {...register('password')} error={errors.password?.message}
            hint="Min 8 chars with uppercase, lowercase, digit and special character" />
          <Input label="Display name (optional)" placeholder="How you'll appear on reviews"
            {...register('displayName')} error={errors.displayName?.message} />
          <Input label="City (optional)" placeholder="Kolkata"
            {...register('city')} error={errors.city?.message} />

          {errMsg && <p className="text-sm text-red-600 text-center">{errMsg}</p>}

          <Button type="submit" loading={isPending} fullWidth size="lg">
            Create account
          </Button>
        </form>

        <div className="mt-4 border-t border-gray-100 pt-4 text-center">
          <p className="text-sm text-gray-500">
            Already have an account?{' '}
            <Link to="/login" className="text-brand-600 font-medium hover:text-brand-700">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
