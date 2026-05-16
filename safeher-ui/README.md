# SafeHer UI

React + TypeScript frontend for the SafeHer platform.

## Tech stack

| Library | Purpose |
|---|---|
| React 18 + TypeScript | UI framework |
| Vite | Build tool + dev server |
| React Router v6 | Routing + protected routes |
| TanStack Query v5 | Server state, caching, pagination |
| Zustand | Auth state (persisted to localStorage) |
| Axios | HTTP client + JWT interceptor with auto-refresh |
| React Hook Form + Zod | Forms with validation matching backend constraints |
| Leaflet + React-Leaflet | Map with safety-scored place pins |
| Tailwind CSS | Styling |

## Project structure

```
src/
├── api/            # Axios service modules per domain
│   ├── axios.ts    # Base instance + JWT interceptor + auto-refresh
│   ├── auth.api.ts
│   ├── places.api.ts
│   ├── ratings.api.ts
│   └── users.api.ts
├── store/
│   └── auth.store.ts   # Zustand auth slice (persisted)
├── lib/
│   └── queryClient.ts  # TanStack Query config + query key factory
├── types/
│   └── index.ts        # All TypeScript types mirroring backend DTOs
├── utils/
│   └── index.ts        # Score colors, category labels, tag lists, formatters
├── hooks/
│   └── useDebounce.ts
├── components/
│   ├── ui/             # Button, Input, Textarea, Select, Toggle, Tag, Avatar, etc.
│   ├── layout/Navbar.tsx
│   ├── auth/ProtectedRoute.tsx
│   ├── map/MapView.tsx         # Leaflet map with color-coded safety pins
│   ├── place/PlaceCard.tsx
│   └── rating/
│       ├── ReviewCard.tsx
│       ├── StarPicker.tsx
│       └── ScoreDistributionBar.tsx
└── pages/
    ├── HomePage.tsx        # Map + nearby places + category filters
    ├── PlaceDetailPage.tsx # Safety score, distribution, tags, reviews
    ├── WriteReviewPage.tsx # Star picker + tag selector + anonymous toggle
    ├── SearchPage.tsx      # Keyword + category + city search
    ├── ProfilePage.tsx     # My reviews, my places, settings
    ├── AddPlacePage.tsx    # Add new place with geo detection
    └── AuthPages.tsx       # Login + Register
```

## Getting started

```bash
# Install
npm install

# Copy env
cp .env.example .env

# Start (requires backend on :8080)
npm run dev
```

App runs at **http://localhost:3000**

## Key design decisions

**All API calls go to port 8080 (API Gateway)** — Vite proxies `/api` to `localhost:8080` in dev. In production, point `VITE_API_URL` at the gateway.

**JWT auto-refresh** — The Axios interceptor queues failed 401 requests, refreshes the token once, then retries all queued requests. No request is lost during a token rotation.

**TanStack Query key factory** — Every query has a structured key in `queryClient.ts`. Cache invalidation after mutations is surgical — only the affected resource's queries are invalidated.

**Score color system:**
- 4.0–5.0 → green (`bg-brand-400`)
- 2.5–3.9 → amber
- 1.0–2.4 → red
- 0 (no ratings) → gray

**Anonymous reviews** — The toggle on the review form maps to the backend `anonymous` flag. The `ReviewCard` component automatically renders an anonymous avatar and "Anonymous" name when `anonymous=true`.

## Adding Google OAuth later

1. Add `spring-boot-starter-oauth2-client` to Auth Service
2. Add a "Continue with Google" button to `LoginPage.tsx`
3. Redirect to `${VITE_API_URL}/v1/auth/oauth2/google`
4. Handle the redirect back with the JWT in query params
