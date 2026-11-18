# medic-web

Shared HTTP pieces. Controllers in the services return the same error JSON and the same page JSON because both shapes are defined here.

## Error and page bodies

`ErrorResponse` is the body of a handled API error: timestamp, HTTP status, reason phrase, message, path, and optional field errors. `PageResponse` is a page of results: the items, page index, size, totals, and whether this page is first or last.

## Correlation id

`CorrelationIdFilter` puts the request's correlation id into the logging context. If the caller did not send one, the filter creates it. `CorrelationIdRestClientInterceptor` copies that id onto outbound calls, which is how booking's call to identity keeps the same id. `MdcTaskDecorator` copies the logging context onto worker threads so a notification email still logs the id of the request that caused it.

The key name itself lives in `medic-common` as `CorrelationId`. This module is the servlet filter and the HTTP client hook.
