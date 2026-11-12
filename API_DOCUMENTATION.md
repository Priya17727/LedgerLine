# BalanceVault API Documentation

## Base URL
http://localhost:8080

## Authentication
- Authentication uses HTTP Basic.
- Use the registered username and password.
- Passwords are stored using BCrypt hashing.
- All endpoints require authentication except POST /api/auth/register.
- Role restrictions are enforced by the backend.

## Roles
- ACCOUNT_HOLDER: Account and transaction operations.
- ADMIN: Account request approval and rejection.
- AUDITOR: Audit reports and audit logs.

## 1. Authentication

### Register
POST /api/auth/register

Public endpoint.

Request:
```json
{
  "username": "user123",
  "email": "user@example.com",
  "password": "password123"
}
```

Validation:
- Username: 3 to 30 characters.
- Valid email required.
- Password: minimum 8 characters.

Success: 201 Created.

### Current User
GET /api/auth/me

Authentication required.

Returns the authenticated username and role.

## 2. Accounts

All endpoints require ACCOUNT_HOLDER.

### List My Accounts
GET /api/accounts

Returns the logged-in user's accounts.

### Get Account Balance
GET /api/accounts/{accountNumber}/balance

Returns the account number and balance.
Users can access only their own accounts.

## 3. Account Requests

### Submit Account Request
POST /api/account-requests

Role: ACCOUNT_HOLDER

No request body required.

Success: 201 Created.

### View My Requests
GET /api/account-requests/my

Role: ACCOUNT_HOLDER

Returns the user's account requests and statuses.

### View Pending Requests
GET /api/account-requests/pending

Role: ADMIN

Returns pending requests.

### Approve Request
PUT /api/account-requests/{requestId}/approve

Role: ADMIN

Approves a pending request and creates an account.

### Reject Request
PUT /api/account-requests/{requestId}/reject

Role: ADMIN

Request:
```json
{
  "reason": "Request rejected"
}
```

## 4. Transactions

All transaction endpoints require ACCOUNT_HOLDER.

Every money movement request requires this header:

Idempotency-Key: a-unique-request-key

Use a unique key for each new transaction. Reusing the same key with a different request is rejected.

### Deposit
POST /api/transactions/deposit

Request:
```json
{
  "accountNumber": "LLXXXXXXXXXXXX",
  "amount": "100.00"
}
```

Success: 201 Created.

### Withdraw
POST /api/transactions/withdraw

Request:
```json
{
  "accountNumber": "LLXXXXXXXXXXXX",
  "amount": "50.00"
}
```

### Transfer
POST /api/transactions/transfer

Request:
```json
{
  "senderAccountNumber": "LLXXXXXXXXXXXX",
  "receiverAccountNumber": "LLYYYYYYYYYYYY",
  "amount": "25.00"
}
```

### Transaction History
GET /api/transactions/history/{accountNumber}

Role: ACCOUNT_HOLDER

Returns transaction history for the specified account belonging to the authenticated user.

## 5. Auditor Endpoints

### Transaction Audit Report
GET /api/transactions/audit

Role: AUDITOR

Returns the transaction audit report.

### Audit Logs
GET /api/audit-logs

Role: AUDITOR

Returns audit log records.

## 6. Security and Financial Controls

- HTTP Basic authentication.
- BCrypt password hashing.
- Role-based authorization.
- Double-entry ledger accounting.
- Idempotency-key protection.
- Account locking for transaction safety.
- Overdraft prevention.
- Transactional financial operations.
- Audit logging.

## 7. Running Locally

Backend:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:
```powershell
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:5173
Backend: http://localhost:8080

Configure DB_PASSWORD in the backend environment before starting the application.

## 8. Notes

- Request and response examples are illustrative.
- Use valid account numbers returned by the application.
- Exact error responses depend on backend validation and exception handling.
- Production deployment requires HTTPS, database least-privilege roles, rate limiting, and secrets management.
