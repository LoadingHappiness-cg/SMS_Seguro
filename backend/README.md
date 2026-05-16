# SMS Seguro Link Enrichment Backend

Minimal phase-1 backend for remote link enrichment, prepared for controlled internal/beta production deployment.

## Endpoints

- `POST /api/link-enrich`
- `GET /health`

Request:

```json
{
  "host": "example.com"
}
```

Response:

```json
{
  "dns_blocked": false,
  "dns_provider": "quad9",
  "resolved_ip_count": 1,
  "ip_reputation_score": null,
  "risk_delta": 0,
  "reasons": []
}
```

Health response:

```json
{
  "status": "ok"
}
```

## Environment variables

Required:

- none

Supported:

- `PORT`
  - default: `8787`
- `HOST`
  - default: `0.0.0.0`
- `QUAD9_TIMEOUT_MS`
  - default: `800`
- `RATE_LIMIT_WINDOW_MS`
  - default: `60000`
- `RATE_LIMIT_MAX_REQUESTS`
  - default: `60`
- `LOG_LEVEL`
  - one of: `error`, `warn`, `info`
  - default: `info`

## Local run

From the repo root:

```bash
node backend/src/index.js
```

Default port: `8787`

Example request:

```bash
curl -s http://127.0.0.1:8787/api/link-enrich \
  -H 'content-type: application/json' \
  -d '{"host":"example.com"}'
```

Health check:

```bash
curl -s http://127.0.0.1:8787/health
```

Example with explicit production-like env config:

```bash
PORT=8787 \
HOST=0.0.0.0 \
QUAD9_TIMEOUT_MS=800 \
RATE_LIMIT_WINDOW_MS=60000 \
RATE_LIMIT_MAX_REQUESTS=60 \
LOG_LEVEL=info \
node backend/src/index.js
```

## Docker

Build:

```bash
docker build -t sms-seguro-link-enrichment ./backend
```

Run:

```bash
docker run --rm -p 8787:8787 \
  -e PORT=8787 \
  -e HOST=0.0.0.0 \
  -e QUAD9_TIMEOUT_MS=800 \
  -e RATE_LIMIT_WINDOW_MS=60000 \
  -e RATE_LIMIT_MAX_REQUESTS=60 \
  -e LOG_LEVEL=info \
  sms-seguro-link-enrichment
```

Container health check:

```bash
curl -s http://127.0.0.1:8787/health
```

## Reverse proxy / subdomain

Recommended:

- Use a dedicated internal/beta-only hostname such as `enrich-beta.smsseguro.example`.
- Terminate TLS at the reverse proxy.
- Forward only:
  - `POST /api/link-enrich`
  - `GET /health`
- Keep request body size small at the proxy layer too, for example `1k` to `4k`.
- Do not add proxy logs that persist request bodies or app client IPs longer than your standard operational window.

Suggested reverse-proxy behavior:

- upstream timeout: `2s` to `3s`
- only allow `POST` and `GET`
- return standard `429` responses unchanged
- disable caching for `/api/link-enrich`

## Tests

```bash
node --test backend/test/*.test.js
```

## Notes

- Only the host/domain is accepted.
- No client IP is persisted.
- Rate limiting is short-lived in-memory only and does not write to disk.
- Logging is minimal and excludes request bodies and client IPs.
- On resolver failure, the service returns a fail-open response with `risk_delta: 0`.
- No AbuseIPDB, ASN, domain-age, or other new signals are included in this phase.
