import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import { Navbar } from '@/components/layout/Navbar'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { HomePage } from '@/pages/HomePage'
import { PlaceDetailPage } from '@/pages/PlaceDetailPage'
import { SearchPage } from '@/pages/SearchPage'
import { WriteReviewPage } from '@/pages/WriteReviewPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { AddPlacePage } from '@/pages/AddPlacePage'
import { ChatbotPage } from '@/pages/ChatbotPage'
import { LoginPage, RegisterPage } from '@/pages/AuthPages'
import { Button } from '@/components/ui'

export default function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen flex flex-col bg-gray-50">
        <Navbar />
        <main className="flex-1">
          <Routes>
            <Route path="/"           element={<HomePage />} />
            <Route path="/place/:id"  element={<PlaceDetailPage />} />
            <Route path="/search"     element={<SearchPage />} />
            <Route path="/login"      element={<LoginPage />} />
            <Route path="/register"   element={<RegisterPage />} />
            <Route path="/chat"       element={<ProtectedRoute><ChatbotPage /></ProtectedRoute>} />
            <Route path="/chat/:placeId" element={<ProtectedRoute><ChatbotPage /></ProtectedRoute>} />
            <Route path="/place/:id/review" element={<ProtectedRoute><WriteReviewPage /></ProtectedRoute>} />
            <Route path="/add-place"  element={<ProtectedRoute><AddPlacePage /></ProtectedRoute>} />
            <Route path="/profile"    element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
            <Route path="/profile/reviews" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
            <Route path="/settings"   element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
            <Route path="*" element={
              <div className="flex items-center justify-center min-h-[60vh]">
                <div className="text-center">
                  <p className="text-6xl font-bold text-gray-100 mb-3">404</p>
                  <p className="text-sm text-gray-500 mb-4">Page not found</p>
                  <Link to="/"><Button variant="secondary" size="sm">Go home</Button></Link>
                </div>
              </div>
            } />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
