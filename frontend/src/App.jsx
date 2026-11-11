
import Auth from './Auth'
import { useState, useEffect, useCallback } from 'react'
import {
  LayoutDashboard, Wallet, ArrowLeftRight, ReceiptText,
  ShieldCheck, Settings, Bell, Search, Plus,
  ArrowDownLeft, ArrowUpRight, Send, ChevronDown,
  CreditCard, Menu, X, LogOut, RefreshCw
} from 'lucide-react'
import './App.css'

const API = 'http://localhost:8080'

const money = value =>
  `₹${Number(value || 0).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`

const dateTime = value =>
  value ? new Date(value).toLocaleString('en-IN') : '—'

function App() {
  const [activePage, setActivePage] = useState('Overview')
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [currentUser, setCurrentUser] = useState(null)

  const [accounts, setAccounts] = useState([])
  const [accountsLoading, setAccountsLoading] = useState(false)
  const [accountsError, setAccountsError] = useState('')

  const [createLoading, setCreateLoading] = useState(false)
  const [createMessage, setCreateMessage] = useState('')
  const [createError, setCreateError] = useState('')

  const [depositAccount, setDepositAccount] = useState('')
  const [depositAmount, setDepositAmount] = useState('')
  const [depositLoading, setDepositLoading] = useState(false)
  const [depositMessage, setDepositMessage] = useState('')
  const [depositError, setDepositError] = useState('')

  const [withdrawAccount, setWithdrawAccount] = useState('')
  const [withdrawAmount, setWithdrawAmount] = useState('')
  const [withdrawLoading, setWithdrawLoading] = useState(false)
  const [withdrawMessage, setWithdrawMessage] = useState('')
  const [withdrawError, setWithdrawError] = useState('')

  const [senderAccount, setSenderAccount] = useState('')
  const [receiverAccount, setReceiverAccount] = useState('')
  const [transferAmount, setTransferAmount] = useState('')
  const [transferLoading, setTransferLoading] = useState(false)
  const [transferMessage, setTransferMessage] = useState('')
  const [transferError, setTransferError] = useState('')

  const [transactions, setTransactions] = useState([])
  const [transactionsLoading, setTransactionsLoading] = useState(false)
  const [transactionsError, setTransactionsError] = useState('')
  const [selectedTransactionAccount, setSelectedTransactionAccount] = useState('')

  const [auditData, setAuditData] = useState([])
  const [pendingRequests, setPendingRequests] = useState([])
const [requestsLoading, setRequestsLoading] = useState(false)
const [requestsError, setRequestsError] = useState('')
const [requestMessage, setRequestMessage] = useState('')
  const [auditLoading, setAuditLoading] = useState(false)
  const [auditError, setAuditError] = useState('')

  const [refreshing, setRefreshing] = useState(false)

 const navigation = [
  { name: 'Overview', icon: LayoutDashboard },
  { name: 'My Accounts', icon: Wallet },
  ...(currentUser?.role === 'ADMIN'
    ? [{ name: 'Account Requests', icon: Bell }]
    : []),
  { name: 'Transactions', icon: ArrowLeftRight },
  { name: 'Statements', icon: ReceiptText },
  { name: 'Auditor Panel', icon: ShieldCheck }
]

  const actions = [
    { name: 'Deposit', icon: ArrowDownLeft, color: 'green' },
    { name: 'Withdraw', icon: ArrowUpRight, color: 'orange' },
    { name: 'Transfer', icon: Send, color: 'blue' }
  ]

  const Icon =
    navigation.find(item => item.name === activePage)?.icon ||
    LayoutDashboard

  const totalBalance = accounts.reduce(
    (total, account) => total + Number(account.balance || 0),
    0
  )

  const accountCount = accounts.length

  const authHeaders = useCallback(() => ({
    Authorization: currentUser?.authorization || ''
  }), [currentUser])

  // Load accounts
  const loadAccounts = useCallback(async () => {
    if (!currentUser || currentUser.role !== 'ACCOUNT_HOLDER') {
      setAccounts([])
      return
    }

    setAccountsLoading(true)
    setAccountsError('')

    try {
      const response = await fetch(`${API}/api/accounts`, {
        headers: authHeaders()
      })

      const data = await response.json().catch(() => [])

      if (!response.ok) {
        throw new Error(data.message || data.error || 'Unable to load accounts.')
      }

      setAccounts(Array.isArray(data) ? data : [])
    } catch (error) {
      setAccountsError(error.message)
    } finally {
      setAccountsLoading(false)
    }
  }, [currentUser, authHeaders])

  useEffect(() => {
    if (currentUser) loadAccounts()
  }, [currentUser, loadAccounts])

  // Load transaction history for the selected account
  const loadTransactions = useCallback(async (accountNumber) => {
    if (!currentUser || !accountNumber) {
      setTransactions([])
      return
    }

    setTransactionsLoading(true)
    setTransactionsError('')

    try {
      const response = await fetch(
        `${API}/api/transactions/history/${encodeURIComponent(accountNumber)}`,
        { headers: authHeaders() }
      )

      const data = await response.json().catch(() => [])

      if (!response.ok) {
        throw new Error(data.message || data.error || 'Unable to load transaction history.')
      }

      setTransactions(Array.isArray(data) ? data : [])
    } catch (error) {
      setTransactionsError(error.message)
      setTransactions([])
    } finally {
      setTransactionsLoading(false)
    }
  }, [currentUser, authHeaders])

  // Select the first account automatically
  useEffect(() => {
    if (accounts.length > 0 && !selectedTransactionAccount) {
      setSelectedTransactionAccount(accounts[0].accountNumber)
    }
  }, [accounts, selectedTransactionAccount])

  // Load history when the selected account or page changes
  useEffect(() => {
    if (
      currentUser &&
      selectedTransactionAccount &&
      ['Transactions', 'Statements'].includes(activePage)
    ) {
      loadTransactions(selectedTransactionAccount)
    }
  }, [
    currentUser,
    selectedTransactionAccount,
    activePage,
    loadTransactions
  ])

  // Load auditor information
  const loadAuditData = useCallback(async () => {
    if (!currentUser) return

    setAuditLoading(true)
    setAuditError('')
    setAuditData([])

    try {
      const response = await fetch(`${API}/api/transactions/audit`, {
        headers: authHeaders()
      })

      const data = await response.json().catch(() => [])

      if (!response.ok) {
        throw new Error(
          data.message || data.error ||
          'Unable to load audit report. Check your auditor permissions.'
        )
      }

      setAuditData(Array.isArray(data) ? data : data ? [data] : [])
    } catch (error) {
      setAuditError(error.message)
    } finally {
      setAuditLoading(false)
    }
  }, [currentUser, authHeaders])
  // Load pending account requests (ADMIN only)
  const loadPendingRequests = useCallback(async () => {
    if (currentUser?.role !== 'ADMIN') return

    setRequestsLoading(true)
    setRequestsError('')
    setRequestMessage('')

    try {
      const response = await fetch(
        `${API}/api/account-requests/pending`,
        { headers: authHeaders() }
      )

      const data = await response.json().catch(() => [])

      if (!response.ok) {
        throw new Error(
          data.message || data.error ||
          'Unable to load pending account requests.'
        )
      }

      setPendingRequests(Array.isArray(data) ? data : [])
    } catch (error) {
      setRequestsError(error.message)
    } finally {
      setRequestsLoading(false)
    }
  }, [currentUser, authHeaders])
  useEffect(() => {
    if (currentUser && activePage === 'Auditor Panel') {
      loadAuditData()
    }
  }, [currentUser, activePage, loadAuditData])
useEffect(() => {
  if (currentUser?.role === 'ADMIN' &&
      activePage === 'Account Requests') {
    loadPendingRequests()
  }
}, [currentUser, activePage, loadPendingRequests])
  // Refresh all account data
  const handleRefresh = async () => {
    setRefreshing(true)
    await loadAccounts()

    if (selectedTransactionAccount) {
      await loadTransactions(selectedTransactionAccount)
    }

    setRefreshing(false)
  }

    // Approve an account request (ADMIN only)
  const handleApproveRequest = async (requestId) => {
    try {
      const response = await fetch(
        `${API}/api/account-requests/${requestId}/approve`,
        {
          method: 'PUT',
          headers: authHeaders()
        }
      )

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data.message || data.error || 'Approval failed.')
      }

      setRequestMessage(
        `Request approved! New account: ${data.accountNumber}`
      )

      await loadPendingRequests()
    } catch (error) {
      setRequestsError(error.message)
    }
  }

  // Reject an account request (ADMIN only)
  const handleRejectRequest = async (requestId) => {
    const reason = window.prompt('Enter the rejection reason:')

    if (!reason || !reason.trim()) return

    try {
      const response = await fetch(
        `${API}/api/account-requests/${requestId}/reject`,
        {
          method: 'PUT',
          headers: {
            ...authHeaders(),
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ reason: reason.trim() })
        }
      )

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data.message || data.error || 'Rejection failed.')
      }

      setRequestMessage('Account request rejected successfully.')

      await loadPendingRequests()
    } catch (error) {
      setRequestsError(error.message)
    }
  }
  // Submit account request for admin approval
  const handleCreateAccount = async () => {
    setCreateLoading(true)
    setCreateMessage('')
    setCreateError('')

    try {
      const response = await fetch(`${API}/api/account-requests`, {
        method: 'POST',
        headers: authHeaders()
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(
          data.message || data.error || 'Account request failed.'
        )
      }

      setCreateMessage(
        'Account request submitted successfully! Waiting for admin approval.'
      )
    } catch (error) {
      setCreateError(error.message)
    } finally {
      setCreateLoading(false)
    }
  }

  // Deposit money
  const handleDeposit = async event => {
    event.preventDefault()
    setDepositMessage('')
    setDepositError('')

    const amount = Number(depositAmount)

    if (!depositAccount) {
      setDepositError('Please select an account.')
      return
    }

    if (
      !depositAmount ||
      !Number.isFinite(amount) ||
      amount <= 0 ||
      !/^\d+(\.\d{1,2})?$/.test(depositAmount)
    ) {
      setDepositError('Enter a positive amount with at most two decimal places.')
      return
    }

    setDepositLoading(true)

    try {
      const response = await fetch(`${API}/api/transactions/deposit`, {
        method: 'POST',
        headers: {
          ...authHeaders(),
          'Content-Type': 'application/json',
          'Idempotency-Key': crypto.randomUUID()
        },
        body: JSON.stringify({
          accountNumber: depositAccount,
          amount: depositAmount
        })
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data.message || data.error || 'Deposit failed.')
      }

      await loadAccounts()

      setDepositMessage(
        `Deposit successful! Transaction ID: ${data.transactionId || '—'}`
      )
      setDepositAmount('')
    } catch (error) {
      setDepositError(error.message)
    } finally {
      setDepositLoading(false)
    }
  }

  // Withdraw money
  const handleWithdraw = async event => {
    event.preventDefault()
    setWithdrawMessage('')
    setWithdrawError('')

    const amount = Number(withdrawAmount)

    if (!withdrawAccount) {
      setWithdrawError('Please select an account.')
      return
    }

    if (
      !withdrawAmount ||
      !Number.isFinite(amount) ||
      amount <= 0 ||
      !/^\d+(\.\d{1,2})?$/.test(withdrawAmount)
    ) {
      setWithdrawError('Enter a positive amount with at most two decimal places.')
      return
    }

    const selectedAccount = accounts.find(
      account => account.accountNumber === withdrawAccount
    )

    if (!selectedAccount || amount > Number(selectedAccount.balance)) {
      setWithdrawError('Insufficient balance.')
      return
    }

    setWithdrawLoading(true)

    try {
      const response = await fetch(`${API}/api/transactions/withdraw`, {
        method: 'POST',
        headers: {
          ...authHeaders(),
          'Content-Type': 'application/json',
          'Idempotency-Key': crypto.randomUUID()
        },
        body: JSON.stringify({
          accountNumber: withdrawAccount,
          amount: withdrawAmount
        })
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data.message || data.error || 'Withdrawal failed.')
      }

      await loadAccounts()

      setWithdrawMessage(
        `Withdrawal successful! Transaction ID: ${data.transactionId || '—'}`
      )
      setWithdrawAmount('')
    } catch (error) {
      setWithdrawError(error.message)
    } finally {
      setWithdrawLoading(false)
    }
  }

  // Transfer money
  const handleTransfer = async event => {
    event.preventDefault()
    setTransferMessage('')
    setTransferError('')

    const amount = Number(transferAmount)

    if (!senderAccount || !receiverAccount) {
      setTransferError('Please select both sender and receiver accounts.')
      return
    }

    if (senderAccount === receiverAccount) {
      setTransferError('Sender and receiver accounts must be different.')
      return
    }

    if (
      !transferAmount ||
      !Number.isFinite(amount) ||
      amount <= 0 ||
      !/^\d+(\.\d{1,2})?$/.test(transferAmount)
    ) {
      setTransferError('Enter a positive amount with at most two decimal places.')
      return
    }

    const sender = accounts.find(
      account => account.accountNumber === senderAccount
    )

    if (!sender || amount > Number(sender.balance)) {
      setTransferError('Insufficient balance in the sender account.')
      return
    }

    setTransferLoading(true)

    try {
      const response = await fetch(`${API}/api/transactions/transfer`, {
        method: 'POST',
        headers: {
          ...authHeaders(),
          'Content-Type': 'application/json',
          'Idempotency-Key': crypto.randomUUID()
        },
        body: JSON.stringify({
          senderAccountNumber: senderAccount,
          receiverAccountNumber: receiverAccount,
          amount: transferAmount
        })
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data.message || data.error || 'Transfer failed.')
      }

      await loadAccounts()

      setTransferMessage(
        `Transfer successful! Sender transaction ID: ${data.senderTransactionId || '—'}`
      )
      setTransferAmount('')
    } catch (error) {
      setTransferError(error.message)
    } finally {
      setTransferLoading(false)
    }
  }

  const openPage = page => {
    setActivePage(page)
    setSidebarOpen(false)
  }

  const renderTransactionRows = () => {
    if (transactionsLoading) {
      return <p>Loading transactions...</p>
    }

    if (transactionsError) {
      return <p style={{ color: '#d64545' }}>{transactionsError}</p>
    }

    if (transactions.length === 0) {
      return (
        <div className="empty-activity">
          <div className="empty-art"><ReceiptText size={25} /></div>
          <strong>No transactions found</strong>
          <p>Transactions for this account will appear here.</p>
        </div>
      )
    }

    return (
      <div style={{ width: '100%', overflowX: 'auto' }}>
        <table style={tableStyle}>
          <thead>
            <tr>
              <th style={thStyle}>Transaction ID</th>
              <th style={thStyle}>Type</th>
              <th style={thStyle}>Debit</th>
              <th style={thStyle}>Credit</th>
              <th style={thStyle}>Balance</th>
              <th style={thStyle}>Status</th>
              <th style={thStyle}>Date</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((transaction, index) => (
              <tr key={transaction.transactionId || index}>
                <td style={tdStyle}>{transaction.transactionId || '—'}</td>
                <td style={tdStyle}>{transaction.type || '—'}</td>
                <td style={tdStyle}>{money(transaction.debit)}</td>
                <td style={tdStyle}>{money(transaction.credit)}</td>
                <td style={tdStyle}>{money(transaction.runningBalance)}</td>
                <td style={tdStyle}>
                  <span style={{
                    color: transaction.status === 'SUCCESS' ? '#15945b' : '#d97706',
                    fontWeight: 700
                  }}>
                    {transaction.status || '—'}
                  </span>
                </td>
                <td style={tdStyle}>{dateTime(transaction.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )
  }

  if (!currentUser) {
    return <Auth onAuthenticated={setCurrentUser} />
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="brand">
          <div className="brand-mark">L</div>
          <div className="brand-name">
            <strong>BalanceVault</strong>
            <span>FINANCIAL PLATFORM</span>
          </div>
          <button className="mobile-close" onClick={() => setSidebarOpen(false)}>
            <X size={20} />
          </button>
        </div>

        <div className="workspace-label">WORKSPACE</div>

        <nav className="navigation">
          {navigation.map(({ name, icon: NavIcon }) => (
            <button
              key={name}
              className={`nav-link ${activePage === name ? 'selected' : ''}`}
              onClick={() => openPage(name)}
            >
              <NavIcon size={19} strokeWidth={1.8} />
              <span>{name}</span>
              {name === 'Auditor Panel' && <span className="nav-lock">PRO</span>}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="security-card">
            <div className="security-icon"><ShieldCheck size={20} /></div>
            <strong>Security first</strong>
            <p>Your financial activity is protected by secure access controls.</p>
          </div>

          <button className="nav-link settings-link" onClick={() => openPage('Settings')}>
            <Settings size={19} />
            <span>Settings</span>
          </button>

          <div className="user-profile">
            <div className="user-avatar">U</div>
            <div className="user-details">
              <strong>{currentUser.username}</strong>
              <span>Personal workspace</span>
            </div>
            <button
              title="Log out"
              onClick={() => setCurrentUser(null)}
              style={{ marginLeft: 'auto', background: 'transparent', border: 0, color: '#8998af', cursor: 'pointer' }}
            >
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>

      {sidebarOpen && (
        <div className="sidebar-overlay" onClick={() => setSidebarOpen(false)} />
      )}

      <main className="main-area">
        <header className="top-header">
          <div className="header-left">
            <button className="mobile-menu" onClick={() => setSidebarOpen(true)}>
              <Menu size={22} />
            </button>
            <div>
              <div className="breadcrumb">Workspace <span>/</span> {activePage}</div>
              <h2>{activePage}</h2>
            </div>
          </div>

          <div className="header-actions">
            <div className="connection-status"><span /> Secure connection</div>
            <button className="icon-button" aria-label="Search">
              <Search size={19} />
            </button>
            <button className="icon-button notification-button" aria-label="Notifications">
              <Bell size={19} />
              <i />
            </button>
            <div className="header-user">U <ChevronDown size={14} /></div>
          </div>
        </header>

        {/* OVERVIEW */}
        {activePage === 'Overview' ? (
          <div className="dashboard-content">
            <section className="welcome-row">
              <div>
                <div className="eyebrow">YOUR FINANCES, SIMPLIFIED</div>
                <h1>Welcome back<span>.</span></h1>
                <p>Here's an overview of your financial activity.</p>
              </div>
              <button className="primary-button" onClick={() => openPage('My Accounts')}>
                <Plus size={18} /> Manage accounts
              </button>
            </section>

            <section className="metrics-grid">
              <div className="balance-panel">
                <div className="balance-heading">
                  <span>Total account balance</span>
                  <Wallet size={19} />
                </div>
                <div className="balance-value">
                  {accountsLoading ? 'Loading...' : accountsError ? 'Unavailable' : money(totalBalance)}
                </div>
                <p className="balance-caption">
                  Combined balance across your BalanceVault accounts.
                </p>
                <div className="balance-bottom">
                  <span><i /> Account overview</span>
                  <span>{accountCount} account(s)</span>
                </div>
              </div>

              <div className="metric-card">
                <div className="metric-top">
                  <span>My accounts</span>
                  <div className="metric-icon purple"><CreditCard size={19} /></div>
                </div>
                <h2>{accountsLoading ? '...' : accountCount}</h2>
                <p>{accountsError || 'Your accounts'}</p>
                <button onClick={() => openPage('My Accounts')}>View accounts →</button>
              </div>

              <div className="metric-card">
                <div className="metric-top">
                  <span>Transactions</span>
                  <div className="metric-icon blue"><ArrowLeftRight size={19} /></div>
                </div>
                <h2>{transactions.length || '—'}</h2>
                <p>Selected account activity</p>
                <button onClick={() => openPage('Transactions')}>View activity →</button>
              </div>
            </section>

            <section className="content-section">
              <div className="section-title">
                <div>
                  <h3>Quick actions</h3>
                  <p>Manage your money with ease.</p>
                </div>
              </div>
              <div className="quick-actions">
                {actions.map(({ name, icon: ActionIcon, color }) => (
                  <button key={name} className="quick-action" onClick={() => openPage(name)}>
                    <div className={`quick-icon ${color}`}><ActionIcon size={21} /></div>
                    <div className="quick-text">
                      <strong>{name} money</strong>
                      <span>{name === 'Transfer' ? 'Send money securely' : `${name} your account`}</span>
                    </div>
                    <span className="quick-arrow">↗</span>
                  </button>
                ))}
              </div>
            </section>

            <section className="content-section activity-section">
              <div className="section-title">
                <div>
                  <h3>Recent activity</h3>
                  <p>Keep track of your latest transactions.</p>
                </div>
                <button className="text-button" onClick={() => openPage('Transactions')}>
                  View all <ArrowLeftRight size={15} />
                </button>
              </div>
              <div className="empty-activity">
                <div className="empty-art"><ReceiptText size={25} /></div>
                <strong>Open Transactions to view your activity</strong>
                <p>Select an account to see its latest transaction history.</p>
              </div>
            </section>

            <div className="bottom-note">
              <ShieldCheck size={16} />
              <span>BalanceVault helps make every money movement traceable and verifiable.</span>
            </div>
          </div>

        /* MY ACCOUNTS */
        ) : activePage === 'My Accounts' ? (
          <div className="page-content">
            <div className="page-card">
              <div className="page-icon"><Wallet size={27} /></div>
              <h2>My Accounts</h2>
              <p>Manage and view your BalanceVault accounts.</p>

             <button className="primary-button" onClick={handleCreateAccount} disabled={createLoading}>
  {createLoading ? 'Submitting...' : <><Plus size={17} /> Request New Account</>}
</button>

              {createMessage && <p style={{ color: 'green', marginTop: 15 }}>{createMessage}</p>}
              {createError && <p style={{ color: 'red', marginTop: 15 }}>{createError}</p>}
              {accountsError && <p style={{ color: 'red', marginTop: 15 }}>{accountsError}</p>}

              {accountsLoading ? <p>Loading your accounts...</p> : (
                accounts.length === 0 ? <p>You don't have any accounts yet.</p> : (
                  <div style={{ width: '100%', marginTop: 20 }}>
                    {accounts.map(account => (
                      <div key={account.accountNumber} style={accountCardStyle}>
                        <h3>Account Details</h3>
                        <p><strong>Account Number:</strong> {account.accountNumber}</p>
                        <p><strong>Available Balance:</strong> {money(account.balance)}</p>
                        <p><strong>Created At:</strong> {dateTime(account.createdAt)}</p>
                      </div>
                    ))}
                  </div>
                )
              )}

              <button className="primary-button" style={{ marginTop: 25 }} onClick={handleRefresh} disabled={refreshing}>
                <RefreshCw size={16} /> {refreshing ? 'Refreshing...' : 'Refresh accounts'}
              </button>
            </div>
          </div>

        /* DEPOSIT */
        ) : activePage === 'Deposit' ? (
          <div className="page-content">
            <div className="page-card">
              <div className="page-icon"><ArrowDownLeft size={27} /></div>
              <h2>Deposit Money</h2>
              <p>Add money to one of your BalanceVault accounts.</p>

              <form onSubmit={handleDeposit} style={formStyle}>
                <label style={labelStyle}>Select Account</label>
                <select value={depositAccount} onChange={e => setDepositAccount(e.target.value)} required style={fieldStyle}>
                  <option value="">Choose an account</option>
                  {accounts.map(account => (
                    <option key={account.accountNumber} value={account.accountNumber}>
                      {account.accountNumber} — {money(account.balance)}
                    </option>
                  ))}
                </select>

                <label style={labelStyle}>Deposit Amount (₹)</label>
                <input type="number" value={depositAmount} onChange={e => setDepositAmount(e.target.value)} placeholder="Enter amount" min="0.01" step="0.01" required style={fieldStyle} />

                {depositMessage && <p style={{ color: 'green' }}>{depositMessage}</p>}
                {depositError && <p style={{ color: 'red' }}>{depositError}</p>}

                <button type="submit" className="primary-button" disabled={depositLoading}>
                  {depositLoading ? 'Processing...' : 'Confirm Deposit'}
                </button>
              </form>
            </div>
          </div>

        /* WITHDRAW */
        ) : activePage === 'Withdraw' ? (
          <div className="page-content">
            <div className="page-card">
              <div className="page-icon"><ArrowUpRight size={27} /></div>
              <h2>Withdraw Money</h2>
              <p>Withdraw money from your account securely.</p>

              <form onSubmit={handleWithdraw} style={formStyle}>
                <label style={labelStyle}>Select Account</label>
                <select value={withdrawAccount} onChange={e => setWithdrawAccount(e.target.value)} required style={fieldStyle}>
                  <option value="">Choose an account</option>
                  {accounts.map(account => (
                    <option key={account.accountNumber} value={account.accountNumber}>
                      {account.accountNumber} — {money(account.balance)}
                    </option>
                  ))}
                </select>

                <label style={labelStyle}>Withdrawal Amount (₹)</label>
                <input type="number" value={withdrawAmount} onChange={e => setWithdrawAmount(e.target.value)} placeholder="Enter amount" min="0.01" step="0.01" required style={fieldStyle} />

                {withdrawMessage && <p style={{ color: 'green' }}>{withdrawMessage}</p>}
                {withdrawError && <p style={{ color: 'red' }}>{withdrawError}</p>}

                <button type="submit" className="primary-button" disabled={withdrawLoading}>
                  {withdrawLoading ? 'Processing...' : 'Confirm Withdrawal'}
                </button>
              </form>
            </div>
          </div>

        /* TRANSFER */
        ) : activePage === 'Transfer' ? (
          <div className="page-content">
            <div className="page-card">
              <div className="page-icon"><Send size={27} /></div>
              <h2>Transfer Money</h2>
              <p>Transfer money securely between BalanceVault accounts.</p>

              <form onSubmit={handleTransfer} style={formStyle}>
                <label style={labelStyle}>From Account</label>
                <select value={senderAccount} onChange={e => setSenderAccount(e.target.value)} required style={fieldStyle}>
                  <option value="">Choose sender account</option>
                  {accounts.map(account => (
                    <option key={account.accountNumber} value={account.accountNumber}>
                      {account.accountNumber} — {money(account.balance)}
                    </option>
                  ))}
                </select>

                <label style={labelStyle}>To Account</label>
                <input value={receiverAccount} onChange={e => setReceiverAccount(e.target.value)} placeholder="Enter receiver account number" required style={fieldStyle} />

                <label style={labelStyle}>Transfer Amount (₹)</label>
                <input type="number" value={transferAmount} onChange={e => setTransferAmount(e.target.value)} placeholder="Enter amount" min="0.01" step="0.01" required style={fieldStyle} />

                {transferMessage && <p style={{ color: 'green' }}>{transferMessage}</p>}
                {transferError && <p style={{ color: 'red' }}>{transferError}</p>}

                <button type="submit" className="primary-button" disabled={transferLoading}>
                  {transferLoading ? 'Processing...' : 'Confirm Transfer'}
                </button>
              </form>
            </div>
          </div>

        /* TRANSACTIONS */
        ) : activePage === 'Transactions' ? (
          <div className="page-content">
            <div className="page-card" style={{ alignItems: 'stretch', textAlign: 'left' }}>
              <div className="page-icon"><ArrowLeftRight size={27} /></div>
              <h2 style={{ textAlign: 'center' }}>Transaction History</h2>
              <p style={{ textAlign: 'center' }}>View money movements for your account.</p>

              <label style={labelStyle}>Select Account</label>
              <select value={selectedTransactionAccount} onChange={e => setSelectedTransactionAccount(e.target.value)} style={fieldStyle}>
                <option value="">Choose an account</option>
                {accounts.map(account => (
                  <option key={account.accountNumber} value={account.accountNumber}>
                    {account.accountNumber}
                  </option>
                ))}
              </select>

              <button className="primary-button" onClick={() => loadTransactions(selectedTransactionAccount)} disabled={!selectedTransactionAccount || transactionsLoading} style={{ alignSelf: 'center', margin: '12px 0 22px' }}>
                <RefreshCw size={16} /> Refresh history
              </button>

              {renderTransactionRows()}
            </div>
          </div>

        /* STATEMENTS */
        ) : activePage === 'Statements' ? (
          <div className="page-content">
            <div className="page-card" style={{ alignItems: 'stretch', textAlign: 'left' }}>
              <div className="page-icon"><ReceiptText size={27} /></div>
              <h2 style={{ textAlign: 'center' }}>Account Statement</h2>
              <p style={{ textAlign: 'center' }}>Review debit, credit and running balance entries.</p>

              <label style={labelStyle}>Select Account</label>
              <select value={selectedTransactionAccount} onChange={e => setSelectedTransactionAccount(e.target.value)} style={fieldStyle}>
                <option value="">Choose an account</option>
                {accounts.map(account => (
                  <option key={account.accountNumber} value={account.accountNumber}>
                    {account.accountNumber}
                  </option>
                ))}
              </select>

              <div style={{ marginTop: 20 }}>
                {renderTransactionRows()}
              </div>
            </div>
          </div>

        /* AUDITOR PANEL */
                /* ACCOUNT REQUESTS */
        ) : activePage === 'Account Requests' &&
             currentUser?.role === 'ADMIN' ? (
          <div className="page-content">
            <div className="page-card" style={{
              alignItems: 'stretch',
              textAlign: 'left'
            }}>
              <div className="page-icon">
                <Bell size={27} />
              </div>

              <h2 style={{ textAlign: 'center' }}>
                Pending Account Requests
              </h2>

              <p style={{ textAlign: 'center' }}>
                Review account opening requests.
              </p>

              <button
                className="primary-button"
                onClick={loadPendingRequests}
                disabled={requestsLoading}
                style={{ alignSelf: 'center', marginBottom: 20 }}
              >
                <RefreshCw size={16} />
                {requestsLoading ? 'Loading...' : 'Refresh Requests'}
              </button>

              {requestMessage && (
                <p style={{ color: 'green' }}>{requestMessage}</p>
              )}

              {requestsError && (
                <p style={{ color: 'red' }}>{requestsError}</p>
              )}

              {requestsLoading && <p>Loading requests...</p>}

              {!requestsLoading && !requestsError &&
                pendingRequests.length === 0 && (
                  <p style={{ textAlign: 'center' }}>
                    No pending account requests.
                  </p>
                )}

              {pendingRequests.map(request => (
                <div
                  key={request.requestId}
                  style={{
                    ...accountCardStyle,
                    marginBottom: 15
                  }}
                >
                  <h3>Request #{request.requestId}</h3>
                  <p><strong>Username:</strong> {request.username}</p>
                  <p><strong>Email:</strong> {request.email}</p>
                  <p><strong>Status:</strong> {request.status}</p>
                  <p>
                    <strong>Requested:</strong>{' '}
                    {dateTime(request.requestedAt)}
                  </p>

                  <div style={{
                    display: 'flex',
                    gap: 12,
                    flexWrap: 'wrap'
                  }}>
                    <button
                      className="primary-button"
                      onClick={() =>
                        handleApproveRequest(request.requestId)
                      }
                    >
                      Approve
                    </button>

                    <button
                      className="primary-button"
                      onClick={() =>
                        handleRejectRequest(request.requestId)
                      }
                    >
                      Reject
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : activePage === 'Auditor Panel' ? (
          <div className="page-content">
            <div className="page-card" style={{ alignItems: 'stretch', textAlign: 'left' }}>
              <div className="page-icon"><ShieldCheck size={27} /></div>
              <h2 style={{ textAlign: 'center' }}>Auditor Panel</h2>
              <p style={{ textAlign: 'center' }}>Review the audit report from the secure backend.</p>

              <button className="primary-button" onClick={loadAuditData} disabled={auditLoading} style={{ alignSelf: 'center', marginBottom: 20 }}>
                <RefreshCw size={16} /> {auditLoading ? 'Loading...' : 'Refresh audit report'}
              </button>

              {auditLoading && <p>Loading audit report...</p>}
              {auditError && <p style={{ color: 'red' }}>{auditError}</p>}

              {!auditLoading && !auditError && auditData.length === 0 && (
                <p>No audit records were returned.</p>
              )}

              {auditData.length > 0 && (
                <div style={{ width: '100%', overflowX: 'auto' }}>
                  <table style={tableStyle}>
                    <thead>
                      <tr>
                        {Object.keys(auditData[0]).map(key => (
                          <th key={key} style={thStyle}>{key}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {auditData.map((record, index) => (
                        <tr key={record.id || record.transactionId || index}>
                          {Object.keys(auditData[0]).map(key => (
                            <td key={key} style={tdStyle}>
                              {typeof record[key] === 'object'
                                ? JSON.stringify(record[key])
                                : String(record[key] ?? '—')}
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

        /* SETTINGS / OTHER */
        ) : (
          <div className="page-content">
            <div className="page-card">
              <div className="page-icon"><Icon size={27} /></div>
              <h2>{activePage}</h2>
              <p>This section is not available yet.</p>
              <button className="primary-button" onClick={() => openPage('Overview')}>
                Back to overview
              </button>
            </div>
          </div>
        )}

        <footer className="app-footer">
          <span>© 2026 BalanceVault</span>
          <span>Secure <b>·</b> Reliable <b>·</b> Transparent</span>
        </footer>
      </main>
    </div>
  )
}

const formStyle = {
  width: '100%',
  maxWidth: '430px',
  display: 'flex',
  flexDirection: 'column',
  textAlign: 'left',
  gap: '10px',
  marginTop: '15px'
}

const labelStyle = {
  display: 'block',
  width: '100%',
  textAlign: 'left',
  fontSize: '13px',
  fontWeight: 600,
  color: '#344054',
  marginTop: '12px'
}

const fieldStyle = {
  display: 'block',
  width: '100%',
  padding: '12px',
  borderRadius: '8px',
  border: '1px solid #d1d5db',
  background: '#fff',
  boxSizing: 'border-box',
  fontSize: '13px'
}

const accountCardStyle = {
  padding: '20px',
  marginTop: '15px',
  border: '1px solid #e5e7eb',
  borderRadius: '12px',
  textAlign: 'left',
  width: '100%',
  boxSizing: 'border-box'
}

const tableStyle = {
  width: '100%',
  borderCollapse: 'collapse',
  fontSize: '12px',
  textAlign: 'left'
}

const thStyle = {
  padding: '12px',
  background: '#f4f6fb',
  color: '#526078',
  borderBottom: '1px solid #e5e7eb',
  whiteSpace: 'nowrap'
}

const tdStyle = {
  padding: '12px',
  color: '#526078',
  borderBottom: '1px solid #edf0f5',
  whiteSpace: 'nowrap'
}

export default App

