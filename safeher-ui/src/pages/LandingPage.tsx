import { Link } from 'react-router-dom'
import { Button } from '@/components/ui'
import { CATEGORY_ICONS, CATEGORY_LABELS } from '@/utils'
import type { PlaceCategory } from '@/types'

// ── Icons ──────────────────────────────────────────────────────────────────────

function ShieldIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  )
}

function MapIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
    </svg>
  )
}

function ChatIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
    </svg>
  )
}

function StarIcon({ className, filled }: { className?: string; filled?: boolean }) {
  return filled ? (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
    </svg>
  ) : (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
    </svg>
  )
}

function EyeOffIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
    </svg>
  )
}

function CheckCircleIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  )
}

// ── Map mock ───────────────────────────────────────────────────────────────────

function ScorePin({ score, safe, style, delay = '' }: {
  score: string; safe: boolean; style: React.CSSProperties; delay?: string
}) {
  return (
    <div
      className={`absolute flex flex-col items-center group cursor-default animate-fade-in ${delay}`}
      style={style}
    >
      <div className={`
        flex items-center gap-1 px-2 py-1 rounded-full text-xs font-semibold border
        transition-transform duration-200 group-hover:scale-110
        ${safe
          ? 'bg-brand-50 text-brand-600 border-brand-100'
          : 'bg-amber-50 text-amber-700 border-amber-100'}
      `}>
        <StarIcon className="w-2.5 h-2.5" filled />
        {score}
      </div>
      <div className={`w-px h-2 ${safe ? 'bg-brand-200' : 'bg-amber-200'}`} />
      <div className={`w-1.5 h-1.5 rounded-full animate-pulse ${safe ? 'bg-brand-400' : 'bg-amber-400'}`} />
    </div>
  )
}

function MiniCard({ icon, name, sub, score, safe }: {
  icon: string; name: string; sub: string; score: string; safe: boolean
}) {
  return (
    <div className="flex items-center gap-2.5 px-3 py-2 bg-white rounded-lg border border-gray-100 transition-colors duration-150 hover:bg-brand-50/50 hover:border-brand-100">
      <div className={`w-7 h-7 rounded-lg flex items-center justify-center text-sm flex-shrink-0 ${safe ? 'bg-brand-50' : 'bg-amber-50'}`}>
        {icon}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium text-gray-800 truncate">{name}</p>
        <p className="text-xs text-gray-400">{sub}</p>
      </div>
      <span className={`text-xs font-semibold px-1.5 py-0.5 rounded-md ${safe ? 'bg-brand-50 text-brand-600' : 'bg-amber-50 text-amber-700'}`}>
        {score}
      </span>
    </div>
  )
}

function MapMock() {
  return (
    <div className="animate-float w-full max-w-xs border border-gray-200 rounded-2xl overflow-hidden bg-gray-50 select-none shadow-sm">
      <div className="bg-white border-b border-gray-100 px-3 py-2.5 flex items-center gap-2">
        <div className="flex-1 bg-gray-100 rounded-lg px-3 py-1.5 flex items-center gap-2">
          <svg className="w-3 h-3 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <span className="text-xs text-gray-400">Search a place near you…</span>
        </div>
      </div>

      <div className="relative h-44 overflow-hidden bg-[#e8ede4]">
        <div className="absolute inset-0 grid grid-cols-5 grid-rows-4 opacity-30">
          {Array.from({ length: 20 }).map((_, i) => (
            <div key={i} className="border border-gray-300" />
          ))}
        </div>
        <div className="absolute inset-0">
          <div className="absolute top-1/2 left-0 right-0 h-px bg-white opacity-60" />
          <div className="absolute top-0 bottom-0 left-1/3 w-px bg-white opacity-60" />
          <div className="absolute top-0 bottom-0 left-2/3 w-px bg-white opacity-40" />
          <div className="absolute top-1/4 left-0 right-0 h-px bg-white opacity-30" />
        </div>
        <ScorePin score="4.8" safe style={{ top: 20, left: 32 }}  delay="delay-300" />
        <ScorePin score="2.4" safe={false} style={{ top: 52, right: 36 }} delay="delay-500" />
        <ScorePin score="4.3" safe style={{ bottom: 32, left: 90 }} delay="delay-700" />
        <ScorePin score="3.8" safe style={{ bottom: 18, right: 60 }} delay="delay-1000" />
      </div>

      <div className="bg-gray-50 p-3 flex flex-col gap-1.5 border-t border-gray-100">
        <p className="text-xs font-medium text-gray-500 px-1 mb-0.5">4 nearby places</p>
        <MiniCard icon="🏪" name="New Market" sub="Market · 12 reviews" score="4.8" safe />
        <MiniCard icon="🚉" name="Howrah Station" sub="Transit · 8 reviews" score="2.4" safe={false} />
      </div>
    </div>
  )
}

// ── Feature card ───────────────────────────────────────────────────────────────

function FeatureCard({ icon, iconBg, title, description, delay = '' }: {
  icon: React.ReactNode
  iconBg: string
  title: string
  description: string
  delay?: string
}) {
  return (
    <div className={`
      group flex flex-col gap-3 p-5 bg-white border border-gray-100 rounded-xl cursor-default
      transition-all duration-300 hover:-translate-y-1.5 hover:shadow-lg hover:shadow-gray-100 hover:border-brand-100
      animate-fade-in-up ${delay}
    `}>
      <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 transition-transform duration-300 group-hover:scale-110 ${iconBg}`}>
        {icon}
      </div>
      <div>
        <h3 className="text-sm font-semibold text-gray-900 mb-1 transition-colors duration-200 group-hover:text-brand-800">{title}</h3>
        <p className="text-sm text-gray-500 leading-relaxed">{description}</p>
      </div>
    </div>
  )
}

// ── Step ───────────────────────────────────────────────────────────────────────

function Step({ number, title, description, delay = '' }: {
  number: number; title: string; description: string; delay?: string
}) {
  return (
    <div className={`group flex flex-col gap-3 p-4 rounded-xl cursor-default transition-colors duration-200 hover:bg-brand-50/60 animate-fade-in-up ${delay}`}>
      <div className="w-8 h-8 rounded-full bg-brand-50 flex items-center justify-center flex-shrink-0 transition-all duration-200 group-hover:bg-brand-100 group-hover:scale-110">
        <span className="text-sm font-semibold text-brand-600">{number}</span>
      </div>
      <div>
        <h3 className="text-sm font-semibold text-gray-900 mb-1 transition-colors duration-200 group-hover:text-brand-800">{title}</h3>
        <p className="text-sm text-gray-500 leading-relaxed">{description}</p>
      </div>
    </div>
  )
}

// ── Categories ─────────────────────────────────────────────────────────────────

const FEATURED_CATEGORIES: PlaceCategory[] = [
  'TRANSIT_STATION', 'PARK', 'RESTAURANT', 'MARKET',
  'SHOPPING_MALL', 'STREET', 'COLLEGE', 'HOSPITAL',
  'CAFE', 'ATM', 'GYM', 'HOTEL',
]

// ── Main component ─────────────────────────────────────────────────────────────

export function LandingPage() {
  return (
    <div className="bg-white">

      {/* ── Hero ──────────────────────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 lg:py-24">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">

          {/* Text — staggered fade-in-up */}
          <div className="flex flex-col gap-6">
            <span className="animate-fade-in inline-flex items-center gap-1.5 text-xs font-medium text-brand-600 bg-brand-50 px-3 py-1.5 rounded-full w-fit transition-colors duration-200 hover:bg-brand-100">
              <ShieldIcon className="w-3.5 h-3.5" />
              Built for women, powered by community
            </span>

            <h1 className="animate-fade-in-up delay-100 text-4xl lg:text-5xl font-bold text-gray-900 leading-tight tracking-tight">
              Know where it's safe<br />
              <span className="text-brand-400">before you go</span>
            </h1>

            <p className="animate-fade-in-up delay-200 text-base text-gray-500 leading-relaxed max-w-md">
              Real safety ratings and reviews for the places you visit every day —
              contributed by women who've been there. Browse the map, read what others
              experienced, and share your own.
            </p>

            <div className="animate-fade-in-up delay-300 flex items-center gap-3 flex-wrap">
              <div className="transition-transform duration-150 hover:scale-[1.03] active:scale-[0.97]">
                <Link to="/map">
                  <Button size="lg">
                    <MapIcon className="w-4 h-4" />
                    Explore the map
                  </Button>
                </Link>
              </div>
              <div className="transition-transform duration-150 hover:scale-[1.03] active:scale-[0.97]">
                <Link to="/register">
                  <Button variant="secondary" size="lg">
                    Create account
                  </Button>
                </Link>
              </div>
            </div>

            <p className="animate-fade-in delay-500 text-xs text-gray-400">
              No account needed to browse. Sign up to post ratings.
            </p>
          </div>

          {/* Visual — slides in from right then floats */}
          <div className="hidden lg:flex justify-center animate-slide-in-right delay-200">
            <MapMock />
          </div>
        </div>
      </section>

      {/* ── Features ──────────────────────────────────────────────────────── */}
      <section className="border-t border-gray-100 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
          <div className="mb-10 animate-fade-in-up">
            <p className="text-xs font-semibold text-brand-600 uppercase tracking-wide mb-2">Features</p>
            <h2 className="text-2xl font-bold text-gray-900">Everything you need, right on the map</h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <FeatureCard
              delay=""
              iconBg="bg-brand-50"
              icon={<MapIcon className="w-5 h-5 text-brand-600" />}
              title="Safety map"
              description="Browse places near you with safety scores shown directly on the map. Filter by category and search by radius."
            />
            <FeatureCard
              delay="delay-100"
              iconBg="bg-purple-50"
              icon={<ChatIcon className="w-5 h-5 text-purple-600" />}
              title="AI safety chatbot"
              description='Ask natural language questions like "Is this park safe at night?" and get answers grounded in real community reviews.'
            />
            <FeatureCard
              delay="delay-200"
              iconBg="bg-pink-50"
              icon={<EyeOffIcon className="w-5 h-5 text-pink-600" />}
              title="Post anonymously — your choice"
              description="When writing a review, you can choose to post it anonymously. Your name is never shown without your permission."
            />
            <FeatureCard
              delay="delay-300"
              iconBg="bg-amber-50"
              icon={<CheckCircleIcon className="w-5 h-5 text-amber-600" />}
              title="AI moderation"
              description="AI agents automatically detect and suppress fake reviews and coordinated attacks, keeping safety data trustworthy."
            />
          </div>
        </div>
      </section>

      {/* ── How it works ──────────────────────────────────────────────────── */}
      <section className="border-t border-gray-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
          <div className="mb-10 animate-fade-in-up">
            <p className="text-xs font-semibold text-brand-600 uppercase tracking-wide mb-2">How it works</p>
            <h2 className="text-2xl font-bold text-gray-900">Three steps to safer decisions</h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 lg:gap-4">
            <Step delay=""        number={1} title="Search any place"        description="Find a transit stop, park, market, or street by name — or browse the map around your current location." />
            <Step delay="delay-150" number={2} title="Read real reviews"      description="See safety scores, structured tags like 'well-lit' or 'isolated', and honest descriptions from women who've actually been there." />
            <Step delay="delay-300" number={3} title="Share your experience"  description="Rate a place, add tags, and write a review. Post under your name or anonymously — the choice is always yours." />
          </div>
        </div>
      </section>

      {/* ── Categories ────────────────────────────────────────────────────── */}
      <section className="border-t border-gray-100 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <p className="text-sm font-semibold text-gray-700 mb-4 animate-fade-in-up">
            Safety ratings across every kind of space you navigate
          </p>
          <div className="flex flex-wrap gap-2 animate-fade-in-up delay-100">
            {FEATURED_CATEGORIES.map(cat => (
              <Link
                key={cat}
                to={`/map?category=${cat}`}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white border border-gray-200 rounded-full text-sm text-gray-600 transition-all duration-150 hover:border-brand-300 hover:text-brand-600 hover:bg-brand-50 hover:scale-105 active:scale-95"
              >
                <span>{CATEGORY_ICONS[cat]}</span>
                <span>{CATEGORY_LABELS[cat]}</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ───────────────────────────────────────────────────────────── */}
      <section className="border-t border-gray-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
          <div className="animate-fade-in-up bg-brand-50 border border-brand-100 rounded-2xl px-8 py-10 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 transition-shadow duration-300 hover:shadow-lg hover:shadow-brand-50">
            <div className="flex flex-col gap-2 max-w-lg">
              <h2 className="text-xl font-bold text-brand-900">Your review helps the next woman decide</h2>
              <p className="text-sm text-brand-700 leading-relaxed">
                Every rating makes the map more accurate. You can post with your name or stay anonymous —
                what matters is that the experience gets shared.
              </p>
            </div>
            <div className="flex flex-col gap-2 flex-shrink-0">
              <div className="transition-transform duration-150 hover:scale-[1.03] active:scale-[0.97]">
                <Link to="/register">
                  <Button size="lg" className="w-full sm:w-auto">
                    Get started — it's free
                  </Button>
                </Link>
              </div>
              <Link
                to="/map"
                className="text-xs text-brand-600 hover:text-brand-800 text-center transition-all duration-150 hover:tracking-wide"
              >
                Or just browse the map →
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* ── Footer ────────────────────────────────────────────────────────── */}
      <footer className="border-t border-gray-100 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2 group cursor-default">
            <div className="w-5 h-5 bg-brand-400 rounded-md flex items-center justify-center transition-transform duration-200 group-hover:scale-110">
              <ShieldIcon className="w-3 h-3 text-white" />
            </div>
            <span className="text-sm font-semibold text-gray-700">SafeHer</span>
            <span className="text-xs text-gray-400 ml-1">· Built with care for safer communities</span>
          </div>
          <div className="flex items-center gap-5 text-xs text-gray-400">
            <span className="hover:text-gray-600 cursor-pointer transition-colors duration-150">Open source</span>
            <span className="hover:text-gray-600 cursor-pointer transition-colors duration-150">Privacy</span>
            <span className="hover:text-gray-600 cursor-pointer transition-colors duration-150">Terms</span>
          </div>
        </div>
      </footer>
    </div>
  )
}
