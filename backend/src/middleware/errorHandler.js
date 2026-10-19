/**
 * Central Error Handler Middleware
 * Strictly ensures only clean JSON responses are returned to clients.
 * Never returns HTML, plain text, raw stack traces, or provider secrets.
 */
export function errorHandler(err, req, res, next) {
  // Handle JSON parse errors from express.json()
  if (err instanceof SyntaxError && err.status === 400 && 'body' in err) {
    return res.status(400).json({
      success: false,
      message: 'Invalid JSON payload received in request body.'
    });
  }

  // Handle payload too large
  if (err.type === 'entity.too.large') {
    return res.status(413).json({
      success: false,
      message: 'Request payload exceeds maximum allowed size.'
    });
  }

  // Log error message safely without secrets
  const sanitizedMsg = err.message ? err.message.replace(/key=[a-zA-Z0-9_\-]+/gi, 'key=[REDACTED]') : 'Unknown error';
  console.error(`[ERROR] [${new Date().toISOString()}] ${req.method} ${req.path} - ${sanitizedMsg}`);

  // Return clean client-safe error
  const statusCode = err.isOperational ? (err.statusCode || 500) : 500;
  const clientMessage = statusCode === 500 
    ? 'AI service is temporarily unavailable.' 
    : (err.isOperational ? err.message : 'An unexpected error occurred.');

  res.status(statusCode).json({
    success: false,
    message: clientMessage
  });
}

/**
 * 404 Not Found Handler
 */
export function notFoundHandler(req, res) {
  res.status(404).json({
    success: false,
    message: `Endpoint ${req.method} ${req.path} not found.`
  });
}
