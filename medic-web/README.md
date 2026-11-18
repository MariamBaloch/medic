# medic-web

Shared HTTP code for the services. This module is a library, not a running service. A service picks it up through
auto-configuration. The gateway does not use it: the gateway has its own correlation-id filter and depends only on
`medic-common`.

## Error and page bodies

Every service returns the same error JSON and the same page JSON, because both shapes are defined here.

- **`ErrorResponse`** is the body of a handled API error: timestamp, HTTP status, reason phrase, message, path, and
  optional field errors.
- **`PageResponse`** is a page of results: the items, page index, size, totals, and whether this page is the first or
  the last.

## Correlation id

A correlation id is one id that follows a request through every service, so its log lines can be found together.

- **`CorrelationIdFilter`** reads the id from the request and puts it into the logging context (the MDC). If the caller
  did not send one, the filter creates it. It also returns the id in the response header and clears the logging context
  when the request ends.
- **`CorrelationIdRestClientInterceptor`** copies that id onto outbound calls. This is how booking's call to identity
  keeps the same id.
- **`MdcTaskDecorator`** copies the logging context onto worker threads, so a notification email still logs the id of
  the request that caused it.

The key name itself lives in `medic-common` as `CorrelationId`. This module provides the servlet filter and the HTTP
client hook.