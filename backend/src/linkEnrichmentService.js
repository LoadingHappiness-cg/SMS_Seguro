import { createQuad9Resolver } from './quad9Resolver.js';

export function createLinkEnrichmentService({
  resolver = createQuad9Resolver()
} = {}) {
  return async function enrichHost(host) {
    try {
      const result = await resolver.lookupHost(host);
      return buildResponse(result);
    } catch {
      return safeResponse();
    }
  };
}

function buildResponse({ dnsBlocked, resolvedIpCount }) {
  const reasons = [];
  let riskDelta = 0;

  if (dnsBlocked) {
    reasons.push('remote_dns_blocked');
    riskDelta += 45;
  }

  if (!dnsBlocked && resolvedIpCount >= 6) {
    reasons.push('remote_resolved_ip_count_unusual');
    riskDelta += 5;
  }

  return {
    dns_blocked: dnsBlocked,
    dns_provider: 'quad9',
    resolved_ip_count: resolvedIpCount,
    ip_reputation_score: null,
    risk_delta: Math.min(riskDelta, 50),
    reasons
  };
}

export function safeResponse() {
  return {
    dns_blocked: false,
    dns_provider: 'quad9',
    resolved_ip_count: 0,
    ip_reputation_score: null,
    risk_delta: 0,
    reasons: []
  };
}
