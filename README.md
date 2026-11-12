# BalanceVault

A secure money movement platform built around a verifiable double-entry ledger.

## Overview

BalanceVault demonstrates secure financial transactions, account management, role-based authorization, and transaction traceability.

## Features

- User registration and authentication
- Role-based access: ACCOUNT_HOLDER, ADMIN, AUDITOR
- Account requests and administrator approval
- Deposits, withdrawals, and transfers
- Double-entry ledger accounting
- Idempotency protection against duplicate transactions
- Account locking for concurrent transaction safety
- Overdraft prevention
- Transaction audit logging
- Account statements and balance tracking

## Technology Stack

- Frontend: React, Vite, JavaScript, CSS
- Backend: Java, Spring Boot, Spring Security
- Database: PostgreSQL
- Persistence: Spring Data JPA, Hibernate
- Build tool: Maven

## Project Structure

- frontend/ - React application
- backend/ - Spring Boot REST API
- THREATS.md - Security threat model
- PROMPTS.md - AI development assistance record

## Prerequisites

- Java 21 or later
- Node.js and npm
- PostgreSQL

## Run the Backend

From the backend directory, configure DB_PASSWORD and run:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend URL: http://localhost:8080

## Run the Frontend

From the frontend directory:

```powershell
npm install
npm run dev
```

Frontend URL: http://localhost:5173

## Run Tests

From the backend directory:

```powershell
.\mvnw.cmd test
```

## Security

- BCrypt password hashing
- Role-based authorization
- Restricted administrator and auditor operations
- Idempotency protection
- Transactional financial operations
- Account locking and overdraft validation

Production deployment should additionally configure HTTPS, secrets management, database least-privilege roles, rate limiting, and health monitoring.

## Project Goal

BalanceVault aims to make money movement accurate, authorized, and traceable.

## API Documentation
See [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) for API endpoints, authentication, roles, and request examples.
