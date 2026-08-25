# smartdental-backend

REST API for **SmartDental**, a dental clinic management system and patient portal. Built with Spring Boot 3,
Spring Security (JWT + Google OAuth2), Spring Data JPA / PostgreSQL, AWS SES, and a pluggable AI clinical-shorthand
translator (OpenAI or Gemini). Pairs with the [`smartdental-frontend`](../smartdental-frontend) React app.

## Architecture overview

```
                        ┌─────────────────────────┐
                        │   smartdental-frontend    │
                        │  React + Vite (Nginx)     │
                        └────────────┬──────────────┘
                                     │ REST / JWT
                        ┌────────────▼──────────────┐
                        │     smartdental-backend     │
                        │  Spring Boot 3 (this repo)  │
                        │                              │
                        │  Controller → Service →      │
                        │  Repository (Spring Data JPA)│
                        └───┬──────────┬──────────┬───┘
                            │          │          │
                    ┌───────▼───┐ ┌────▼────┐ ┌───▼────────┐
                    │ PostgreSQL │ │ AWS SES  │ │ OpenAI /   │
                    │            │ │ (email)  │ │ Gemini API │
                    └────────────┘ └──────────┘ └────────────┘
```

Layering is a standard REST Controller → Service → Repository stack. Security is stateless (JWT bearer tokens);
Google OAuth2 login mints the same JWTs so the frontend has one auth model regardless of provider.

### Feature list

- Email/password **and** Google OAuth2 authentication issuing the same JWT session
- Role-based access control: `ROLE_ADMIN`, `ROLE_DENTIST`, `ROLE_HYGIENIST`, `ROLE_PATIENT`
- Patient EHR: profiles, medical alerts/allergies, insurance details
- 2D odontogram persistence per FDI tooth number (11-48), surfaces, conditions
- Appointment scheduling with dentist double-booking collision detection
- Clinical notes with dentist shorthand + AI-generated plain-English patient summaries
- `/api/v1/translate` — AI clinical shorthand → plain English (OpenAI or Gemini, with a deterministic
  rule-based fallback when no provider is configured or reachable)
- AWS SES transactional HTML emails on appointment create/update/cancel
- Invoicing with line items, insurance coverage, and balance-due calculation
- Append-only audit log for admin visibility
- `.ics` calendar export for patient appointments

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 (LTS) | Build requires a Lombok-compatible JDK; verified on Temurin/Corretto 21. The compiled artifact runs on any JDK 21+, including 25. |
| Maven | 3.9+ | Or use your IDE's bundled Maven |
| Docker & Docker Compose | latest | For one-command local execution |
| PostgreSQL | 16 | Only needed if running without Docker |

## Environment variables

All variables have safe local defaults (see [`.env.example`](.env.example)) except where noted.

| Variable | Description | Local default |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/smartdental` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials | `smartdental` / `smartdental` |
| `JPA_DDL_AUTO` | Hibernate schema strategy | `update` |
| `SERVER_PORT` | HTTP port | `8080` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:5173` |
| `FRONTEND_BASE_URL` | Used for OAuth2 redirect target | `http://localhost:5173` |
| `JWT_SECRET` | HMAC signing key — **no default in the app itself; startup fails without it.** `docker compose up --build` supplies a local-only value for you | *(none — see docker-compose.yml for the local value)* |
| `JWT_ACCESS_EXPIRATION_MS` | Access token TTL | `3600000` (1h) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token TTL | `604800000` (7d) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 app credentials | empty (Google login disabled) |
| `AWS_REGION` | AWS region for SES | `us-east-1` |
| `SES_SENDER_EMAIL` | Verified SES sender identity | `no-reply@smartdental.example.com` |
| `SES_ENABLED` | Toggle real email sending | `false` |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | SES credentials (omit on EC2 to use an IAM role) | empty |
| `AI_PROVIDER` | `openai` or `gemini` | `openai` |
| `AI_API_KEY` | Provider API key | empty (fallback summaries used) |
| `AI_MODEL` | Model name | `gpt-4o-mini` |
| `AI_BASE_URL` | Provider base URL | `https://api.openai.com/v1` |
| `AI_TRANSLATION_ENABLED` | Toggle AI calls vs. rule-based fallback | `true` (app), `false` (docker-compose default) |
| `SEED_DATA_ENABLED` | Populate demo data on empty DB | `true` |

## Local development

### With Docker (recommended)

```bash
cp .env.example .env
docker compose up --build
```

This starts PostgreSQL and the API on `http://localhost:8080`, fully offline (SES and AI calls are disabled by
default so no cloud credentials are required). Demo accounts are seeded automatically — see `DataInitializer`
(all seeded accounts use the password `Password123!`).

### Without Docker

```bash
# Start PostgreSQL locally, then create the database:
createdb smartdental

export DB_USERNAME=smartdental DB_PASSWORD=smartdental
mvn spring-boot:run
```

The API listens on `http://localhost:8080`; health check at `GET /api/v1/health`.

## Testing

```bash
mvn test              # unit + integration tests (H2 in-memory, no external services required)
mvn verify             # same, run in CI
```

Key suites: `RbacSecurityTest` (role enforcement), `TranslateControllerTest` (AI fallback behavior),
`SesMailServiceTest` (email template + SES request shape), `AppointmentServiceTest` (booking collision logic),
`JwtServiceTest` (token issuance/validation).

## API surface (selected)

| Method | Path | Access |
|---|---|---|
| `POST` | `/api/v1/auth/register`, `/api/v1/auth/login` | Public |
| `GET` | `/oauth2/authorization/google` | Public (Google login) |
| `GET/PUT` | `/api/v1/patients/me/profile` | `ROLE_PATIENT` |
| `GET/PUT` | `/api/v1/patients/{id}/odontogram/{tooth}` | Staff |
| `POST` | `/api/v1/appointments` | Patient or staff |
| `POST` | `/api/v1/translate` | Staff |
| `POST` | `/api/v1/clinical-notes` | `ROLE_DENTIST`, `ROLE_HYGIENIST` |
| `GET/POST` | `/api/v1/admin/**` | `ROLE_ADMIN` |

See [`DEPLOYMENT.md`](DEPLOYMENT.md) for production setup on AWS EC2.
