import { Router } from 'express';
import { geminiService } from '../geminiService.js';
import { authenticate } from '../middleware/auth.js';
import { rateLimiter } from '../middleware/rateLimiter.js';
import { config } from '../config.js';

const router = Router();

/**
 * POST /api/ai/chat
 * Primary chat endpoint for Nila AI Android app
 */
router.post('/chat', authenticate, rateLimiter, async (req, res, next) => {
  try {
    const { prompt, conversationContext } = req.body || {};

    // Validate prompt
    if (typeof prompt !== 'string' || !prompt.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Prompt is required and must be a non-empty string.'
      });
    }

    const trimmedPrompt = prompt.trim();
    if (trimmedPrompt.length > config.maxPromptLength) {
      return res.status(400).json({
        success: false,
        message: `Prompt exceeds maximum allowed length of ${config.maxPromptLength} characters.`
      });
    }

    // Validate conversationContext if present
    if (conversationContext !== undefined && !Array.isArray(conversationContext)) {
      return res.status(400).json({
        success: false,
        message: 'conversationContext must be an array of message objects.'
      });
    }

    const result = await geminiService.generateChatResponse(trimmedPrompt, conversationContext);

    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

export default router;
