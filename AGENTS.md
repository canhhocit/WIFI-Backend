# WI-FI CAPTIVE PORTAL - BACKEND ENGINEERING RULES

## 1. Working Principles
- Understand the existing codebase before making changes.
- Read relevant files, trace dependencies, and identify the root cause before implementing a fix.
- Prefer simple, maintainable, production-oriented solutions over clever or unnecessarily complex code.
- Explain important trade-offs when multiple solutions are possible.
- Do not rewrite working code merely to match personal preferences.

## 2. Code Quality & Architecture
- Maintain the **Modular Monolith** architecture:
  - `authentication`: Admin login, JWT tokens, Spring Security config, Password encoder.
  - `locations`: Wi-Fi location management CRUD, timezone, gateway mapping, 3-tier admin tree.
  - `portal`: Guest login API, portal config inquiry, validation.
  - `sessions`: Session creation, state machine (`PENDING`, `AUTHORIZED`, `REVOKED`, `EXPIRED`, `FAILED`), cleanup scheduler.
  - `devices`: Device / MAC metadata tracking, hardware assets (ONT, Router, AP).
  - `analytics`: Location-based and time-based metrics aggregation.
  - `audit`: Immutable audit logging of administrative and session actions.
  - `gateway`: `GatewayAdapter` boundary interface, `MockGatewayAdapter`, `OpnsenseGatewayAdapter`.
  - `common`: Global exception handling, error codes, base DTOs, response wrappers.
- Use Java 17 and Spring Boot 3 conventions.
- Layered Separation: Controllers (HTTP & DTO validation only) $\rightarrow$ Services (business logic & transactions) $\rightarrow$ Repositories (Spring Data JPA).

## 3. Security & Boundaries
- Never trust client-supplied MAC addresses, IP addresses, location IDs, or authorization status.
- DTOs and Bean Validation (`@Valid`, `@NotBlank`, etc.) must be used for all request endpoints.
- Preserve Spring Security configuration with JWT and BCrypt password hashing.
- Distinguish application authentication from actual network authorization (gateway permit).
- Return explicit success/failure results; never assume authorization succeeded if the gateway operation failed.
- NEVER modify an already-applied Flyway migration. Create new versioned migrations (`V4__...sql`, etc.) for schema updates.
- Use UTC timestamps for all session and audit records.

## 4. Verification & Testing
- Run relevant unit, controller, and integration tests with `mvn test` after making changes.
- Never claim tests passed without executing them.

## 5. Task Reports & Deliverables
- After completing any coding, bug fix, feature update, or audit task:
  1. Always write a comprehensive task report to:
     `D:\Download\AMZ\Wi-Fi-Captive-Portal\docs\report/`
  2. Naming format: `{năm}_{tháng}_{ngày}_{Nội dung task}.md`
  3. Commit and push changes to `https://github.com/canhhocit/WIFI-Backend.git` (nhánh `main`).
