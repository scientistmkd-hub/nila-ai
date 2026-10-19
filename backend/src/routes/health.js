import { Router } from 'express';
import { config } from '../config.js';

const router = Router();

/**
 * GET /api/health
 * Public health check endpoint indicating server and provider status
 */
router.get('/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    geminiConfigured: config.isGeminiConfigured,
    service: 'nila-ai-backend',
    version: '1.0.0',
    model: config.geminiModel,
    timestamp: new Date().toISOString()
  });
});

export default router;
