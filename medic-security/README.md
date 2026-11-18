# medic-security

JWT checks used by every service. Identity is the only service that creates a token. This library only reads one.

## What a request goes through

`JwtRequestFilter` runs before the controller. It reads `Authorization: Bearer <token>`. `JwtSupport` checks the signature and builds a `UserPrincipal` from the claims. `AuthenticatedUser` is how a controller asks for the current user id, role, name, or patient and doctor id.

Two Redis checks happen before the controller runs:

- `TokenBlacklistService` rejects the token when its `jti` was stored at logout.
- `TokenVersionService` rejects the token when its `ver` claim does not match the version stored for that user. Identity bumps that version on password change and deactivation.

An expired token, a bad signature, a revoked token, or a stale version returns 401 JSON from `UnauthorizedResponseWriter`. A missing token is also 401, through `JwtAuthenticationEntryPoint`, not 403.

## Internal calls

`InternalApiKeyFilter` applies only to paths that start with `/internal`. The caller must send `X-Internal-Key` equal to `INTERNAL_API_KEY`. The gateway does not route those paths. Booking calls identity on the Docker network and sends this header.
