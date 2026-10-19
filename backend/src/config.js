import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load environment variables from .env file if present
dotenv.config({ path: path.resolve(__dirname, '../.env') });
// Also check root .env
dotenv.config({ path: path.resolve(__dirname, '../../.env') });

export const config = {
  port: parseInt(process.env.PORT || '8080', 10),
  nodeEnv: process.env.NODE_ENV || 'development',
  
  // Gemini Configuration
  // NEVER log or expose this value
  geminiApiKey: (process.env.GEMINI_API_KEY || '').trim(),
  geminiModel: process.env.GEMINI_MODEL || 'gemini-2.5-flash',
  geminiApiUrl: 'https://generativelanguage.googleapis.com/v1beta/models',
  
  // Optional backend authentication token for Android clients
  backendAuthToken: (process.env.NILA_BACKEND_AUTH_TOKEN || '').trim(),
  
  // Request limits
  requestTimeoutMs: parseInt(process.env.REQUEST_TIMEOUT_MS || '30000', 10),
  maxPromptLength: parseInt(process.env.MAX_PROMPT_LENGTH || '16000', 10),
  
  // Rate limiting (requests per minute per client)
  rateLimitWindowMs: 60 * 1000,
  rateLimitMaxRequests: parseInt(process.env.RATE_LIMIT_MAX_PER_MINUTE || '30', 10),

  // CORS configuration
  allowedOrigins: (process.env.ALLOWED_ORIGINS || '*').split(',').map(s => s.trim()),

  get isGeminiConfigured() {
    return Boolean(this.geminiApiKey && this.geminiApiKey !== 'MY_GEMINI_API_KEY' && this.geminiApiKey.length > 5);
  }
};
