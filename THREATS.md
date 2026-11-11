# BalanceVault — Threat Model

## 1. Overview

BalanceVault is a financial account management application built to support secure account management and traceable money movement.

This document identifies important security threats, their potential impact, and the controls implemented or planned to reduce risk.

## 2. Assets to Protect

- User credentials and personal information
- Account balances
- Transaction records
- Double-entry ledger entries
- Idempotency records
- Audit logs
- Administrative privileges
- Database credentials and configuration secrets

## 3. Trust Boundaries

- Frontend to backend API communication
- Backend application to PostgreSQL
- Account holders to administrative operations
- Application users to auditor-only information
- Environment configuration to application startup

## 4. Threats and Mitigations

### T1 — Unauthorized Access

**Threat:** An attacker attempts to access another user's account or perform restricted operations.

**Impact:** Unauthorized disclosure or modification of financial information.

**Mitigations:**
- Spring Security authentication
- BCrypt password hashing
- Role-based access control
- Server-side authorization checks

### T2 — Privilege Escalation

**Threat:** A normal user attempts to register as an administrator or auditor.

**Impact:** Unauthorized administrative access.

**Mitigations:**
- Public registration assigns the ACCOUNT_HOLDER role.
- Privileged endpoints require appropriate roles.
- First-admin bootstrap is disabled by default.

### T3 — Duplicate Transactions

**Threat:** A client retries a payment request because of a timeout or network failure.

**Impact:** The same financial operation could be processed more than once.

**Mitigations:**
- Idempotency keys
- Stored request records
- Request-content validation when an existing key is reused

### T4 — Concurrent Transaction Race Conditions

**Threat:** Multiple requests attempt to modify the same account simultaneously.

**Impact:** Incorrect balances or lost updates.

**Mitigations:**
- Database transactions
- Pessimistic account locking
- Consistent account locking order for transfers

### T5 — Overdraft and Invalid Amounts

**Threat:** A user attempts to withdraw or transfer more money than the available balance, or submits an invalid amount.

**Impact:** Negative or incorrect account balances.

**Mitigations:**
- Balance validation
- Positive amount validation
- Balance checks while account locks are held

### T6 — Ledger Manipulation

**Threat:** An attacker or application defect attempts to alter financial history or create unbalanced ledger entries.

**Impact:** Financial records become inaccurate or untrustworthy.

**Mitigations:**
- Double-entry ledger records
- Transactional persistence
- Access restrictions on financial operations

**Additional hardening required:** Database-level controls to enforce append-only ledger records and prevent unauthorized direct modifications.

### T7 — Sensitive Information Exposure

**Threat:** Database passwords, authentication details, or personal information are exposed through source control or logs.

**Impact:** Account compromise and unauthorized database access.

**Mitigations:**
- Database password supplied through DB_PASSWORD
- Environment files excluded from Git
- BCrypt password hashing

**Additional hardening required:** Production secret management and review of application logging.

### T8 — Unauthorized Audit Access

**Threat:** An account holder attempts to access administrative audit information.

**Impact:** Exposure of sensitive transaction information.

**Mitigations:**
- Auditor role restrictions
- Server-side endpoint authorization

### T9 — Account Request Abuse

**Threat:** Users repeatedly submit account opening requests or attempt to approve requests without administrative permission.

**Impact:** Request abuse and unauthorized account creation.

**Mitigations:**
- Account request workflow
- Administrative approval endpoint
- Request status validation
- Database locking during approval and rejection

**Additional hardening required:** Strong database-level protection against simultaneous duplicate pending requests.

### T10 — Denial of Service

**Threat:** An attacker sends excessive requests to authentication or transaction endpoints.

**Impact:** Service degradation or unavailability.

**Planned mitigation:** Redis-backed rate limiting, request throttling, and monitoring. Redis rate limiting is not yet implemented.

## 5. Operational Security

- Keep database credentials outside source control.
- Disable unnecessary SQL logging in production.
- Use HTTPS in deployment.
- Restrict database network access.
- Use a least-privilege database account.
- Monitor application errors and suspicious activity.
- Keep dependencies updated.
- Configure health checks for deployment monitoring.

## 6. Remaining Security Work

The following controls require further implementation or verification:

- Redis-based rate limiting
- Least-privilege PostgreSQL roles
- Database-level append-only ledger enforcement
- Automated database-backed concurrency tests
- Automated database-backed idempotency tests
- Production HTTPS and secret management
- Deployment health checks

## 7. Conclusion

BalanceVault uses authentication, role-based authorization, database transactions, account locking, idempotency, and double-entry accounting to reduce important financial application risks.

Security is an ongoing process. Remaining controls must be implemented and tested before production use.
