import test from 'node:test';
import assert from 'node:assert/strict';

import { loadConfig } from '../src/config.js';

test('loads defaults when env vars are absent', () => {
  assert.deepEqual(loadConfig({}), {
    port: 8787,
    host: '0.0.0.0',
    quad9TimeoutMs: 800,
    rateLimitWindowMs: 60_000,
    rateLimitMaxRequests: 60,
    logLevel: 'info'
  });
});

test('loads explicit env overrides', () => {
  assert.deepEqual(loadConfig({
    PORT: '8080',
    HOST: '127.0.0.1',
    QUAD9_TIMEOUT_MS: '900',
    RATE_LIMIT_WINDOW_MS: '30000',
    RATE_LIMIT_MAX_REQUESTS: '15',
    LOG_LEVEL: 'warn'
  }), {
    port: 8080,
    host: '127.0.0.1',
    quad9TimeoutMs: 900,
    rateLimitWindowMs: 30_000,
    rateLimitMaxRequests: 15,
    logLevel: 'warn'
  });
});

test('rejects invalid integer env values', () => {
  assert.throws(() => loadConfig({ PORT: '99999' }), /invalid_integer_env/);
  assert.throws(() => loadConfig({ RATE_LIMIT_MAX_REQUESTS: '0' }), /invalid_integer_env/);
});
