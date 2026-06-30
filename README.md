# DODO PHARMACY

DODO PHARMACY is a professional pharmacy management system built as a modern full-stack application. It supports inventory, supplier management, prescriptions, sales, purchase orders, user administration, and dashboard reporting.

## Project Overview

This repository includes:

- `rxpharma-backend`: a Spring Boot REST API with JWT-based security, PostgreSQL persistence, and Flyway-managed database migrations.
- `rxpharma-frontend`: a React + Vite single-page application that provides the pharmacy user interface.

## Architecture

![DODO PHARMACY Architecture](./rxpharma_Architecture.png)

DODO PHARMACYis implemented with a layered client-server architecture:

- **Frontend**: React + Vite SPA delivering pharmacy workflows and secure API interactions.
- **API layer**: Spring Boot controllers exposing REST endpoints under `/api/**`.
- **Security layer**: Spring Security enforces authentication, JWT validation, and role-based authorization.
- **Service layer**: business logic for stock, prescriptions, sales, suppliers, purchase orders, users, and dashboard metrics.
- **Persistence layer**: Spring Data JPA repositories manage domain entities.
- **Database layer**: PostgreSQL stores transactional data, and Flyway manages schema migration versions.

## Key Features

- JWT-based authentication and stateless API sessions
- Role-based authorization for administrators, pharmacists, cashiers, and supplier managers
- Drug inventory management with stock and expiry checks
- Supplier lifecycle management and purchase order tracking
- Prescription creation, dispensing, cancellation, and search
- Sales registration, invoice retrieval, and financial tracking
- Dashboard metrics for operational visibility
- PostgreSQL persistence with Flyway migration support

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | React, Vite, Tailwind CSS, Axios, React Router |
| Backend | Java 21, Spring Boot 3.4.5, Spring Web, Spring Security |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Migrations | Flyway |
| Authentication | JWT, BCrypt |
| Tooling | Maven Wrapper, Docker Compose, ESLint |

## Repository Structure

```text
rxpharma/
├── README.md
├── rxpharma_Architecture.png
├── docs/
├── rxpharma-backend/
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/rxpharma/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── entity/
│       │   ├── exception/
│       │   ├── repository/
│       │   ├── security/
│       │   └── service/
│       └── main/resources/
│           ├── application.properties
│           └── db/migration/
└── rxpharma-frontend/
    ├── package.json
    └── src/
```

## Backend API Overview

| Module | Path | Purpose |
| --- | --- | --- |
| Authentication | `/api/auth` | Login, register, forgot/reset password |
| Users | `/api/users` | User administration and role management |
| Drugs | `/api/drugs` | Inventory CRUD, stock and expiry alerts |
| Suppliers | `/api/suppliers` | Supplier CRUD and status management |
| Prescriptions | `/api/prescriptions` | Prescription lifecycle and dispensing |
| Sales | `/api/sales` | Sales, sale items, invoices |
| Purchase Orders | `/api/purchase-orders` | Orders, delivery, cancellation |
| Dashboard | `/api/dashboard/stats` | Operational summary metrics |

## Prerequisites

- Java 21
- Node.js and npm
- Docker and Docker Compose
- Git

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd rxpharma
```

### 2. Start the database

```bash
cd rxpharma-backend
docker compose up -d
```

Default PostgreSQL settings are configured for local development on port `5435`.

### 3. Run the backend

```bash
cd rxpharma-backend
./mvnw spring-boot:run
```

The backend service starts at `http://localhost:8083`.

### 4. Run the frontend

Open a second terminal:

```bash
cd rxpharma-frontend
npm install
npm run dev
```

The frontend development server typically runs at `http://localhost:5173`.

## Common Commands

### Backend

```bash
cd rxpharma-backend
./mvnw test
./mvnw spring-boot:run
docker compose up -d
docker compose down
```

### Frontend

```bash
cd rxpharma-frontend
npm install
npm run dev
npm run build
npm run lint
npm run preview
```

## Configuration Notes

- The backend secures APIs with JWT tokens and BCrypt password hashing.
- Only `/api/auth/**` is public; all other endpoints require authentication.
- Database schema changes are managed via Flyway migrations in `rxpharma-backend/src/main/resources/db/migration`.
- Update the JWT secret and production database credentials before deploying.

## Architecture Summary

RxPharma is a modern two-tier application where a React frontend consumes a Spring Boot REST API.

The frontend is responsible for user workflows and API calls, while the backend handles authentication, authorization, business rules, and persistence. The backend applies Spring Security for JWT validation, service classes for domain logic, and Spring Data JPA for PostgreSQL storage. Flyway ensures database schema changes are repeatable and version controlled.

## License

This repository is provided for development and evaluation purposes.
