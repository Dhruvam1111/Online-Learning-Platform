import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import { AuthProvider } from './context/AuthContext.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <AuthProvider>
      <React.Suspense fallback={<main role="status" className="p-6">Loading your workspace…</main>}>
        <App />
      </React.Suspense>
    </AuthProvider>
  </React.StrictMode>,
)

if (import.meta.env.DEV && import.meta.env.VITE_PAYMENT_A11Y_REVIEW === 'true') {
  import('./dev/paymentA11y').then(module => module.enablePaymentAccessibilityReview());
}
