# medic-security

Shared code for checking who is calling. This module is a library, not a running service. Every service that needs to
know the caller uses it.

Identity is the only service that creates a token. This library only reads one.

## What a request goes through

For each request, in order:

1. `JwtRequestFilter` runs before the controller and reads the `Authorization: Bearer <token>` header.
2. `JwtSupport` checks the signature and the expiry, and builds a `UserPrincipal` from the claims.
3. Two Redis checks run:
    - `TokenBlacklistService` rejects the token if its `jti` was stored at logout.
    - `TokenVersionService` rejects the token if its `ver` claim does not match the version stored for that user.
      Identity raises that version on a password change and on deactivation, so old tokens stop working.
4. The controller runs. `AuthenticatedUser` is how it asks for the current user's id, role, name, and patient or
   doctor id.

## When the answer is 401

An expired token, a bad signature, a revoked token, or a stale version returns a 401 JSON error from
`UnauthorizedResponseWriter`. A missing token is also 401, through `JwtAuthenticationEntryPoint`, not 403.

## What each service still decides

This library checks the token. Each service keeps its own security rules: which paths are public, and which roles may
call which endpoint. That way identity can leave registration and login open while booking requires a token for
everything.

## Internal calls

`InternalApiKeyFilter` applies only to paths that start with `/internal`. The caller must send an `X-Internal-Key`
header that equals `INTERNAL_API_KEY`. The key is compared in constant time and is never logged.

The gateway does not route `/internal` paths. Booking calls identity on the Docker network and sends this header.

## Settings

| Setting                    | Purpose                                                             |
|----------------------------|---------------------------------------------------------------------|
| `JWT_SECRET`               | Key used to check token signatures                                  |
| `REDIS_HOST`, `REDIS_PORT` | Redis, for the blacklist and token versions                         |
| `INTERNAL_API_KEY`         | Key required on `/internal` calls (property `app.internal-api-key`) |

If a service has `/internal` paths but no key is configured, those paths reject every call instead of accepting them.