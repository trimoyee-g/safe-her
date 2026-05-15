# Auth Service — SafeHer Platform

Identity and token management for the safeher platform. Port **8081**.

## Responsibilities

| Concern | Implementation |
|---|---|
| Registration | BCrypt password hashing (strength 12), strong password policy, duplicate checks |
| Login | Email **or** username accepted, brute-force lockout (5 attempts → 15 min lock) |
| JWT | Access token (24 h) + Refresh token (7 days), HMAC-SHA256 signed |
| Refresh rotation | Old refresh token revoked on every use — refresh token rotation |
| Logout | Access token blacklisted in Redis for remaining TTL; refresh token revoked in DB |
| Logout all devices | Revokes all refresh tokens for a user |
| Password change | Requires current password; revokes all sessions after change |
| Forgot/reset password | Secure token stored hashed; 30-min expiry |
| Account deletion | Soft-deactivates; publishes `auth.user.deleted` event |
| User profile creation | Dual write: Feign → User Service + Kafka event (eventual consistency fallback) |
| Token cleanup | Scheduled daily job purges expired refresh tokens |

## API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | No | Register new account |
| POST | `/api/v1/auth/login` | No | Login (email or username) |
| POST | `/api/v1/auth/refresh` | No | Rotate refresh token |
| POST | `/api/v1/auth/logout` | Yes | Logout current device |
| POST | `/api/v1/auth/logout-all` | Yes | Logout all devices |
| POST | `/api/v1/auth/change-password` | Yes | Change password |
| POST | `/api/v1/auth/forgot-password` | No | Request reset email |
| POST | `/api/v1/auth/reset-password` | No | Reset with token |
| DELETE | `/api/v1/auth/account` | Yes | Delete account |
| GET | `/api/v1/auth/validate` | Yes | Validate token |

## JWT Claims

```json
{
  "sub": "username",
  "userId": "uuid",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1234567890,
  "exp": 1234654290
}
```

## Kafka Topics Published

| Topic | Payload | Consumer |
|---|---|---|
| `auth.user.registered` | `AuthUserRegisteredEvent` | User Service (create profile) |
| `auth.user.deleted` | `AuthUserDeletedEvent` | User Service, Rating Service |
| `auth.password.changed` | `AuthPasswordChangedEvent` | Notification Service |

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_USER` | `safeher` | DB username |
| `DB_PASSWORD` | `safeher` | DB password |
| `JWT_SECRET` | (dev value) | Shared with all services |
| `REDIS_HOST` | `localhost` | Redis for token blacklist |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka brokers |
| `EUREKA_URI` | `http://admin:admin@localhost:8761/eureka` | Eureka server |

## Running locally

```bash
# Start Redis (needed for token blacklist)
docker run -d -p 6379:6379 redis:7-alpine

# Start Postgres
docker run -d -p 5432:5432 -e POSTGRES_DB=safeher_auth \
  -e POSTGRES_USER=safeher -e POSTGRES_PASSWORD=safeher postgres:16-alpine

./mvnw spring-boot:run
```
