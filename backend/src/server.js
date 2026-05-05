import http from 'node:http';

import { validateHostInput } from './hostValidation.js';
import { createLinkEnrichmentService, safeResponse } from './linkEnrichmentService.js';

const MAX_BODY_BYTES = 1024;

export function createApp({
  logger = console,
  enrichHost = createLinkEnrichmentService()
} = {}) {
  return http.createServer(async (request, response) => {
    const startedAt = Date.now();

    try {
      if (request.url !== '/api/link-enrich') {
        writeJson(response, 404, { error: 'not_found' });
        return;
      }

      if (request.method !== 'POST') {
        writeJson(response, 405, { error: 'method_not_allowed' });
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
        logger.error?.('link-enrich unexpected_error', { message: error?.message });
        writeJson(response, 200, safeResponse());
      }
    } finally {
      logger.info?.('link-enrich request', {
        method: request.method,
        path: request.url,
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
    logger.error?.('link-enrich failed_open', { message: error?.message });
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
