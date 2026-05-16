import { createApp } from './server.js';
import { loadConfig } from './config.js';
import { createLogger } from './logger.js';
import { createLinkEnrichmentService } from './linkEnrichmentService.js';
import { createQuad9Resolver } from './quad9Resolver.js';
import { createRateLimiter } from './rateLimiter.js';

const config = loadConfig(process.env);
const logger = createLogger({ level: config.logLevel });
const app = createApp({
  logger,
  enrichHost: createLinkEnrichmentService({
    resolver: createQuad9Resolver({
      timeoutMs: config.quad9TimeoutMs
    })
  }),
  rateLimiter: createRateLimiter({
    windowMs: config.rateLimitWindowMs,
    maxRequests: config.rateLimitMaxRequests
  })
});

app.listen(config.port, config.host, () => {
  logger.info('backend listening', {
    host: config.host,
    port: config.port
  });
});
