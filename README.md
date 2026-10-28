# Wi-Fi Captive Portal - Backend Service

A Modular Monolith Spring Boot 3 backend for enterprise Wi-Fi Captive Portal management.

## Tech Stack
- **Java 17**
- **Spring Boot 3.3.4** (Web, Data JPA, Security, Actuator, Validation)
- **PostgreSQL / H2**
- **Flyway** for database migrations
- **Stateless JWT** authentication for Admin Dashboard
- **Decoupled Gateway Integration** (`GatewayAdapter` interface + `MockGatewayAdapter`)

## Getting Started

### 1. Environment Configuration
Copy `.env.example` to `.env` or set environment variables:
```bash
cp .env.example .env
```

### 2. Database
Run PostgreSQL with Docker:
```bash
docker compose up -d
```
Or run directly with the zero-dependency in-memory H2 profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### 3. Run the Backend
```bash
mvn spring-boot:run
```
The API server will start on `http://localhost:8080`.

### 4. Run Tests
```bash
mvn clean test
```

### Initial Admin Credentials (Local Bootstrap)
- **Username**: `admin`
- **Password**: `admin123`
