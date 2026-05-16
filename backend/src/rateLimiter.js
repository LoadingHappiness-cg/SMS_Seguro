export function createRateLimiter({
  windowMs = 60_000,
  maxRequests = 60,
  now = () => Date.now()
} = {}) {
  const buckets = new Map();

  return {
    check(key) {
      const currentTime = now();
      pruneExpiredBuckets(buckets, currentTime, windowMs);

      const bucket = buckets.get(key);
      if (!bucket || currentTime >= bucket.resetAt) {
        buckets.set(key, { count: 1, resetAt: currentTime + windowMs });
        return { allowed: true, remaining: maxRequests - 1 };
      }

      if (bucket.count >= maxRequests) {
        return {
          allowed: false,
          remaining: 0,
          retryAfterSeconds: Math.max(1, Math.ceil((bucket.resetAt - currentTime) / 1000))
        };
      }

      bucket.count += 1;
      return { allowed: true, remaining: maxRequests - bucket.count };
    }
  };
}

function pruneExpiredBuckets(buckets, currentTime, windowMs) {
  if (buckets.size === 0) return;

  for (const [key, value] of buckets.entries()) {
    if (value.resetAt + windowMs < currentTime) {
      buckets.delete(key);
    }
  }
}
