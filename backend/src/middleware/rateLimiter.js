import { config } from '../config.js';

const clientRequests = new Map();

// Periodic cleanup of stale client records (every 5 minutes)
setInterval(() => {
  const now = Date.now();
  for (const [key, records] of clientRequests.entries()) {
    const valid = records.filter(ts => now - ts < config.rateLimitWindowMs);
    if (valid.length === 0) {
      clientRequests.delete(key);
    } else {
      clientRequests.set(key, valid);
    }
  }
}, 5 * 60 * 1000).unref();

export function rateLimiter(req, res, next) {
  const clientKey = req.ip || req.headers['x-forwarded-for'] || 'default-client';
  const now = Date.now();

  const timestamps = clientRequests.get(clientKey) || [];
  const recent = timestamps.filter(ts => now - ts < config.rateLimitWindowMs);

  if (recent.length >= config.rateLimitMaxRequests) {
    return res.status(429).json({
      success: false,
      message: 'Rate limit exceeded. Please wait a moment before sending more messages.'
    });
  }

  recent.push(now);
  clientRequests.set(clientKey, recent);
  next();
}
