import { config } from '../config.js';

/**
 * Authentication Middleware for client requests
 * If NILA_BACKEND_AUTH_TOKEN is configured in environment, validates header.
 * If not set, allows requests to pass through.
 */
export function authenticate(req, res, next) {
  if (!config.backendAuthToken) {
    // No backend token enforcement configured
    return next();
  }

  const authHeader = req.headers['authorization'];
  const apiKeyHeader = req.headers['x-api-key'];

  let token = null;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    token = authHeader.substring(7).trim();
  } else if (apiKeyHeader) {
    token = String(apiKeyHeader).trim();
  }

  if (!token || token !== config.backendAuthToken) {
    return res.status(401).json({
      success: false,
      message: 'Unauthorized request: Missing or invalid authentication token.'
    });
  }

  next();
}
