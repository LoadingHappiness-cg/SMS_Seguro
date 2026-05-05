# SMS Seguro Link Enrichment Backend

Minimal phase-1 backend for remote link enrichment.

## Endpoint

- `POST /api/link-enrich`

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

## Local run

From the repo root:

```bash
/Users/carlosgavela/.nvm/versions/node/v22.22.2/bin/node backend/src/index.js
```

Default port: `8787`

Example request:

```bash
curl -s http://127.0.0.1:8787/api/link-enrich \
  -H 'content-type: application/json' \
  -d '{"host":"example.com"}'
```

## Tests

```bash
/Users/carlosgavela/.nvm/versions/node/v22.22.2/bin/node --test backend/test/*.test.js
```

## Notes

- Only the host/domain is accepted.
- No client IP is persisted.
- Logging is minimal and excludes request bodies and client IPs.
- On resolver failure, the service returns a fail-open response with `risk_delta: 0`.
