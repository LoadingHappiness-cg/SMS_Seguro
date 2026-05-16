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

Copy the example file before deployment:

```bash
cp backend/.env.example backend/.env
```

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

## Docker Compose

Minimal controlled deployment package:

```bash
cd backend
cp .env.example .env
docker compose up -d --build
```

Check service status:

```bash
docker compose ps
curl -s http://127.0.0.1:8787/health
```

Stop:

```bash
docker compose down
```

## Reverse proxy / subdomain

Recommended:

- Use `api.smsseguro.loadinghappiness.pt` as the public hostname for this controlled deployment.
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
- proxy pass target: `http://127.0.0.1:8787`

Recommended proxy headers:

- `Host`
- `X-Forwarded-Proto`
- `X-Forwarded-For`

Operational note:

- The backend uses the socket remote address only for short-lived in-memory rate limiting.
- Do not persist raw client IPs in proxy or app logs longer than your standard short operational window.

## Android endpoint configuration

The Android app remote enrichment URL is configurable and is not hardcoded.

Current debug/internal enablement path:

```bash
./gradlew assembleDebug \
  -PSMS_SEGURO_REMOTE_ENRICHMENT_DEBUG_ENABLED=true \
  -PSMS_SEGURO_REMOTE_ENRICHMENT_DEBUG_BASE_URL=https://api.smsseguro.loadinghappiness.pt \
  -PSMS_SEGURO_REMOTE_ENRICHMENT_DEBUG_TRACE=true
```

That value is compiled into `BuildConfig.REMOTE_ENRICHMENT_BASE_URL` for the debug build only. Normal builds remain off by default.

## Controlled production-like validation steps

Backend:

```bash
cd backend
cp .env.example .env
docker compose up -d --build
curl -s https://api.smsseguro.loadinghappiness.pt/health
curl -s https://api.smsseguro.loadinghappiness.pt/api/link-enrich \
  -H 'content-type: application/json' \
  -d '{"host":"example.com"}'
```

Android debug build pointed at the real endpoint:

```bash
./gradlew assembleDebug \
  -PSMS_SEGURO_REMOTE_ENRICHMENT_DEBUG_ENABLED=true \
  -PSMS_SEGURO_REMOTE_ENRICHMENT_DEBUG_BASE_URL=https://api.smsseguro.loadinghappiness.pt \
  -PSMS_SEGURO_REMOTE_ENRICHMENT_DEBUG_TRACE=true
```

Optional smoke injection on a connected debug device:

```bash
adb shell am broadcast \
  -n com.smsguard/.debug.DebugSmokeInjectionReceiver \
  -a com.smsguard.debug.SMOKE_INJECT \
  --es sender Benign \
  --es text 'https://example.com/info?id=2' \
  --es source debug_smoke_benign
```

## Tests

```bash
node --test backend/test/*.test.js
```

## Notes

- Only the host/domain is accepted.
- No client IP is persisted.
- Rate limiting is short-lived in-memory only and does not write to disk.
- Logging is minimal and excludes request bodies, full URLs, and client IPs.
- On resolver failure, the service returns a fail-open response with `risk_delta: 0`.
- No AbuseIPDB, ASN, domain-age, or other new signals are included in this phase.
