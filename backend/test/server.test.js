import test from 'node:test';
import assert from 'node:assert/strict';

import { createApp } from '../src/server.js';

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
    }))
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
