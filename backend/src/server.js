import http from 'node:http';

import { validateHostInput } from './hostValidation.js';
import { createLinkEnrichmentService, safeResponse } from './linkEnrichmentService.js';
import { createRateLimiter } from './rateLimiter.js';

const MAX_BODY_BYTES = 1024;

export function createApp({
  logger = console,
  enrichHost = createLinkEnrichmentService(),
  rateLimiter = createRateLimiter()
} = {}) {
  return http.createServer(async (request, response) => {
    const startedAt = Date.now();
    const path = getPathname(request.url);

    try {
      if (path === '/health' && request.method === 'GET') {
        writeJson(response, 200, {
          status: 'ok'
        });
        return;
      }

      if (path !== '/api/link-enrich') {
        writeJson(response, 404, { error: 'not_found' });
        return;
      }

      if (request.method !== 'POST') {
        writeJson(response, 405, { error: 'method_not_allowed' });
        return;
      }

      const rateLimit = rateLimiter.check(getClientAddress(request));
      if (!rateLimit.allowed) {
        response.setHeader('retry-after', String(rateLimit.retryAfterSeconds));
        writeJson(response, 429, { error: 'rate_limited' });
        return;
      }

      const payload = await readJsonBody(request);
      if (!isValidPayloadShape(payload)) {
        writeJson(response, 400, { error: 'invalid_host' });
        return;
      }

      const hostValidation = validateHostInput(payload.host);
      if (!hostValidation.ok) {
        writeJson(response, 400, { error: 'invalid_host' });
        return;
      }

      const enrichment = await safelyEnrich(enrichHost, hostValidation.host, logger);
      writeJson(response, 200, enrichment);
    } catch (error) {
      if (error?.message === 'invalid_json' || error?.message === 'body_too_large') {
        writeJson(response, 400, { error: 'invalid_json' });
      } else {
        logger.error?.('link-enrich unexpected_error', { code: error?.message ?? 'unknown_error' });
        writeJson(response, 200, safeResponse());
      }
    } finally {
      logger.info?.('link-enrich request', {
        method: request.method,
        path,
        statusCode: response.statusCode,
        durationMs: Date.now() - startedAt
      });
    }
  });
}

async function safelyEnrich(enrichHost, host, logger) {
  try {
    return await enrichHost(host);
  } catch (error) {
    logger.warn?.('link-enrich failed_open', { code: error?.message ?? 'unknown_error' });
    return safeResponse();
  }
}

function isValidPayloadShape(payload) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return false;
  }

  const keys = Object.keys(payload);
  return keys.length === 1 && keys[0] === 'host';
}

async function readJsonBody(request) {
  const chunks = [];
  let bytesRead = 0;

  for await (const chunk of request) {
    bytesRead += chunk.length;
    if (bytesRead > MAX_BODY_BYTES) {
      throw new Error('body_too_large');
    }
    chunks.push(chunk);
  }

  if (chunks.length === 0) {
    throw new Error('invalid_json');
  }

  try {
    return JSON.parse(Buffer.concat(chunks).toString('utf8'));
  } catch {
    throw new Error('invalid_json');
  }
}

function writeJson(response, statusCode, payload) {
  response.statusCode = statusCode;
  response.setHeader('content-type', 'application/json; charset=utf-8');
  response.end(JSON.stringify(payload));
}

function getPathname(url) {
  if (!url) return '/';

  try {
    return new URL(url, 'http://localhost').pathname;
  } catch {
    return url;
  }
}

function getClientAddress(request) {
  return request.socket?.remoteAddress ?? 'unknown';
}
