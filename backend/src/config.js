const DEFAULT_PORT = 8787;
const DEFAULT_HOST = '0.0.0.0';
const DEFAULT_QUAD9_TIMEOUT_MS = 800;
const DEFAULT_RATE_LIMIT_WINDOW_MS = 60_000;
const DEFAULT_RATE_LIMIT_MAX_REQUESTS = 60;
const DEFAULT_LOG_LEVEL = 'info';

export function loadConfig(env = process.env) {
  return {
    port: readInt(env.PORT, DEFAULT_PORT, { min: 1, max: 65535 }),
    host: readHost(env.HOST, DEFAULT_HOST),
    quad9TimeoutMs: readInt(env.QUAD9_TIMEOUT_MS, DEFAULT_QUAD9_TIMEOUT_MS, { min: 100, max: 10_000 }),
    rateLimitWindowMs: readInt(env.RATE_LIMIT_WINDOW_MS, DEFAULT_RATE_LIMIT_WINDOW_MS, { min: 1_000, max: 3_600_000 }),
    rateLimitMaxRequests: readInt(env.RATE_LIMIT_MAX_REQUESTS, DEFAULT_RATE_LIMIT_MAX_REQUESTS, { min: 1, max: 10_000 }),
    logLevel: readLogLevel(env.LOG_LEVEL, DEFAULT_LOG_LEVEL)
  };
}

function readInt(value, fallback, { min, max }) {
  if (value == null || value === '') return fallback;

  const parsed = Number.parseInt(value, 10);
  if (!Number.isInteger(parsed) || parsed < min || parsed > max) {
    throw new Error(`invalid_integer_env:${value}`);
  }

  return parsed;
}

function readHost(value, fallback) {
  if (value == null || value === '') return fallback;
  return String(value).trim() || fallback;
}

function readLogLevel(value, fallback) {
  if (value == null || value === '') return fallback;

  const normalized = String(value).trim().toLowerCase();
  if (!['error', 'warn', 'info'].includes(normalized)) {
    throw new Error(`invalid_log_level:${value}`);
  }

  return normalized;
}
