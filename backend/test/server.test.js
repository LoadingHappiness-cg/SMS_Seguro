import test from 'node:test';
import assert from 'node:assert/strict';

import { createApp } from '../src/server.js';
import { createRateLimiter } from '../src/rateLimiter.js';

async function startTestServer(overrides = {}) {
  const app = createApp({
    logger: { info() {}, error() {} },
    enrichHost: overrides.enrichHost ?? (async () => ({
      dns_blocked: true,
      dns_provider: 'quad9',
      resolved_ip_count: 2,
      ip_reputation_score: null,
      risk_delta: 45,
      reasons: ['remote_dns_blocked']
    })),
    rateLimiter: overrides.rateLimiter
  });

  await new Promise((resolve) => app.listen(0, '127.0.0.1', resolve));
  const address = app.address();

  return {
    app,
    baseUrl: `http://127.0.0.1:${address.port}`
  };
}

test('rejects non-post methods', async () => {
  const { app, baseUrl } = await startTestServer();

  try {
    const response = await fetch(`${baseUrl}/api/link-enrich`);
    assert.equal(response.status, 405);
  } finally {
    app.close();
  }
});

test('serves health endpoint', async () => {
  const { app, baseUrl } = await startTestServer();

  try {
    const response = await fetch(`${baseUrl}/health`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { status: 'ok' });
  } finally {
    app.close();
  }
});

test('rejects invalid payloads', async () => {
  const { app, baseUrl } = await startTestServer();

  try {
    const response = await fetch(`${baseUrl}/api/link-enrich`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ host: 'Example.com', extra: true })
    });

    assert.equal(response.status, 400);
    assert.deepEqual(await response.json(), {
      error: 'invalid_host'
    });
  } finally {
    app.close();
  }
});

test('returns the enrichment contract for a valid host', async () => {
  const { app, baseUrl } = await startTestServer();

  try {
    const response = await fetch(`${baseUrl}/api/link-enrich`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ host: 'example.com' })
    });

    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), {
      dns_blocked: true,
      dns_provider: 'quad9',
      resolved_ip_count: 2,
      ip_reputation_score: null,
      risk_delta: 45,
      reasons: ['remote_dns_blocked']
    });
  } finally {
    app.close();
  }
});

test('fails open when remote enrichment throws', async () => {
  const { app, baseUrl } = await startTestServer({
    enrichHost: async () => {
      throw new Error('resolver unavailable');
    }
  });

  try {
    const response = await fetch(`${baseUrl}/api/link-enrich`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ host: 'example.com' })
    });

    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), {
      dns_blocked: false,
      dns_provider: 'quad9',
      resolved_ip_count: 0,
      ip_reputation_score: null,
      risk_delta: 0,
      reasons: []
    });
  } finally {
    app.close();
  }
});

test('rate limits repeated requests from the same client address', async () => {
  let now = 1_000;
  const { app, baseUrl } = await startTestServer({
    rateLimiter: createRateLimiter({
      windowMs: 60_000,
      maxRequests: 1,
      now: () => now
    })
  });

  try {
    const firstResponse = await fetch(`${baseUrl}/api/link-enrich`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ host: 'example.com' })
    });
    assert.equal(firstResponse.status, 200);

    const secondResponse = await fetch(`${baseUrl}/api/link-enrich`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ host: 'example.com' })
    });
    assert.equal(secondResponse.status, 429);
    assert.equal(secondResponse.headers.get('retry-after'), '60');
    assert.deepEqual(await secondResponse.json(), { error: 'rate_limited' });

    now += 61_000;
    const thirdResponse = await fetch(`${baseUrl}/api/link-enrich`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ host: 'example.com' })
    });
    assert.equal(thirdResponse.status, 200);
  } finally {
    app.close();
  }
});
