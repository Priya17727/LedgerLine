
import { useState } from 'react'
import { ShieldCheck, Wallet, ArrowRight, LockKeyhole, User, Mail } from 'lucide-react'
import './Auth.css'

function Auth({ onAuthenticated }) {
  const [isRegister, setIsRegister] = useState(false)
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setMessage('')

    if (username.trim().length < 3) {
      setError('Username must contain at least 3 characters.')
      return
    }

    if (password.length < 8) {
      setError('Password must contain at least 8 characters.')
      return
    }

    setLoading(true)

    try {
      if (isRegister) {
        const response = await fetch('http://localhost:8080/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: username.trim(),
            email: email.trim(),
            password
          })
        })

        const data = await response.json().catch(() => ({}))

        if (!response.ok) {
          throw new Error(data.message || data.error || 'Registration failed. Please check your details.')
        }

        setMessage('Registration successful! You can now sign in.')
        setIsRegister(false)
        setPassword('')
      } else {
        const authorization = 'Basic ' + btoa(`${username.trim()}:${password}`)

        const response = await fetch('http://localhost:8080/api/auth/me', {
          method: 'GET',
          headers: {
            Authorization: authorization
          }
        })

        if (response.status === 401 || response.status === 403) {
          throw new Error('Invalid username or password.')
        }

        if (!response.ok) {
          throw new Error('Unable to verify your login. Please try again.')
        }

        const userData = await response.json()

onAuthenticated({
  username: userData.username,
  role: userData.role,
  password,
  authorization
})
      }
    } catch (err) {
      setError(
        err.message === 'Failed to fetch'
          ? 'Cannot connect to BalanceVault server. Please make sure the backend is running.'
          : err.message
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-brand">
        <div className="auth-brand-icon"><Wallet size={25} /></div>
        <div>
          <strong>BalanceVault</strong>
          <span>FINANCIAL PLATFORM</span>
        </div>
      </div>

      <div className="auth-card">
        <div className="auth-shield">
          <ShieldCheck size={27} />
        </div>

        <h1>{isRegister ? 'Create your account' : 'Welcome back'}</h1>
        <p className="auth-subtitle">
          {isRegister
            ? 'Join BalanceVault and manage your money securely.'
            : 'Sign in to access your financial workspace.'}
        </p>

        {error && <div className="auth-error">{error}</div>}
        {message && <div className="auth-success">{message}</div>}

        <form onSubmit={handleSubmit}>
          <label>Username</label>
          <div className="auth-input-wrap">
            <User size={18} />
            <input
              type="text"
              placeholder="Enter your username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              minLength={3}
              maxLength={30}
              autoComplete="username"
              required
            />
          </div>

          {isRegister && (
            <>
              <label>Email address</label>
              <div className="auth-input-wrap">
                <Mail size={18} />
                <input
                  type="email"
                  placeholder="Enter your email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  autoComplete="email"
                  required
                />
              </div>
            </>
          )}

          <label>Password</label>
          <div className="auth-input-wrap">
            <LockKeyhole size={18} />
            <input
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              required
            />
          </div>

          <button className="auth-submit" type="submit" disabled={loading}>
            {loading
              ? 'Please wait...'
              : isRegister
                ? 'Create account'
                : 'Sign in'}
            {!loading && <ArrowRight size={18} />}
          </button>
        </form>

        <div className="auth-switch">
          {isRegister ? 'Already have an account?' : "Don't have an account?"}
          <button
            type="button"
            onClick={() => {
              setIsRegister(!isRegister)
              setError('')
              setMessage('')
            }}
          >
            {isRegister ? 'Sign in' : 'Create account'}
          </button>
        </div>

        <div className="auth-security">
          <ShieldCheck size={15} />
          Your connection is protected by secure authentication.
        </div>
      </div>

      <footer className="auth-footer">
        © 2026 BalanceVault · Secure · Reliable · Transparent
      </footer>
    </div>
  )
}

export default Auth

