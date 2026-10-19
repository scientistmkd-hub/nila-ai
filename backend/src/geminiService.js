import { config } from './config.js';

class GeminiService {
  /**
   * Generates a chat response from Gemini API using header-based authentication.
   * Never puts the API key in the URL or query string.
   * Handles timeouts, retries, upstream error codes, and formats clean JSON output.
   * 
   * @param {string} prompt - User prompt text
   * @param {Array<Object>} conversationContext - List of previous turns [{ role: 'user'|'model', text: '...' }]
   * @returns {Promise<{ success: boolean, message: string, provider: string }>}
   */
  async generateChatResponse(prompt, conversationContext = []) {
    if (!config.isGeminiConfigured) {
      const err = new Error('Gemini API key is not configured on the backend server.');
      err.statusCode = 503;
      err.isOperational = true;
      throw err;
    }

    // Build Gemini contents array
    const contents = [];

    // Add recent context (up to last 10 messages)
    if (Array.isArray(conversationContext)) {
      for (const msg of conversationContext.slice(-10)) {
        if (!msg || typeof msg !== 'object') continue;
        const role = (msg.role && (msg.role.toLowerCase() === 'user' || msg.role.toLowerCase() === 'human')) ? 'user' : 'model';
        const text = (msg.text || msg.content || '').trim();
        if (text) {
          contents.push({
            role,
            parts: [{ text }]
          });
        }
      }
    }

    // Add current user prompt
    contents.push({
      role: 'user',
      parts: [{ text: prompt.trim() }]
    });

    const requestPayload = {
      systemInstruction: {
        parts: [{ text: 'You are Nila, an intelligent, respectful, concise personal AI assistant. Keep responses clear and helpful.' }]
      },
      contents,
      generationConfig: {
        maxOutputTokens: 2048
      }
    };

    // SECURE: Target URL does NOT contain API key in query string
    const targetUrl = `${config.geminiApiUrl}/${config.geminiModel}:generateContent`;

    let lastError = null;
    const maxAttempts = 2;

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        console.log(`[AI] Dispatching request to Gemini model=${config.geminiModel} (attempt ${attempt}/${maxAttempts}, promptLen=${prompt.length})`);

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), config.requestTimeoutMs);

        let response;
        try {
          response = await fetch(targetUrl, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
              // SECURE: API key passed strictly via standard HTTP header
              'x-goog-api-key': config.geminiApiKey
            },
            body: JSON.stringify(requestPayload),
            signal: controller.signal
          });
        } finally {
          clearTimeout(timeoutId);
        }

        // Validate HTTP Status Codes
        if (!response.ok) {
          const status = response.status;
          let rawBody = '';

          try {
            rawBody = await response.text();
          } catch (_) {
            rawBody = '';
          }

          // Parse error message if JSON, otherwise sanitize raw text
          let errorDetail = '';
          try {
            const errJson = JSON.parse(rawBody);
            errorDetail = errJson.error?.message || '';
          } catch (_) {
            errorDetail = rawBody.replace(/<[^>]*>?/gm, '').slice(0, 100);
          }

          // Strict redaction: strip any accidental keys or secrets
          errorDetail = errorDetail.replace(/key=[a-zA-Z0-9_\-]+/gi, 'key=[REDACTED]');

          console.warn(`[AI] Gemini returned HTTP ${status}: ${errorDetail.slice(0, 100)}`);

          // Map specific status codes
          switch (status) {
            case 400: {
              const err = new Error('Invalid request format sent to AI provider.');
              err.statusCode = 400;
              err.isOperational = true;
              throw err;
            }
            case 401: {
              const err = new Error('AI provider authentication failed. Check backend GEMINI_API_KEY configuration.');
              err.statusCode = 502;
              err.isOperational = true;
              throw err;
            }
            case 403: {
              const err = new Error('Access denied by AI provider. Verify API key permissions and project quotas.');
              err.statusCode = 502;
              err.isOperational = true;
              throw err;
            }
            case 404: {
              const err = new Error(`AI model '${config.geminiModel}' is not found or unsupported.`);
              err.statusCode = 502;
              err.isOperational = true;
              throw err;
            }
            case 408: {
              const err = new Error('AI provider request timed out.');
              err.statusCode = 504;
              err.isOperational = true;
              throw err;
            }
            case 409: {
              const err = new Error('AI provider resource conflict occurred.');
              err.statusCode = 502;
              err.isOperational = true;
              throw err;
            }
            case 413: {
              const err = new Error('Request payload exceeds AI provider maximum size limit.');
              err.statusCode = 413;
              err.isOperational = true;
              throw err;
            }
            case 415: {
              const err = new Error('Unsupported media format submitted to AI provider.');
              err.statusCode = 415;
              err.isOperational = true;
              throw err;
            }
            case 422: {
              const err = new Error('AI provider was unable to process the prompt content.');
              err.statusCode = 422;
              err.isOperational = true;
              throw err;
            }
            case 429: {
              const err = new Error('AI service rate limit reached. Please wait a moment and try again.');
              err.statusCode = 429;
              err.isOperational = true;
              throw err;
            }
            case 500:
            case 502:
            case 503:
            case 504: {
              if (attempt < maxAttempts) {
                // Transient failure: wait 1 second and retry
                console.log(`[AI] Transient HTTP ${status} error, retrying attempt ${attempt + 1}...`);
                await new Promise(r => setTimeout(r, 1000));
                continue;
              }
              const err = new Error('AI service is temporarily unavailable. Please try again shortly.');
              err.statusCode = status === 504 ? 504 : 503;
              err.isOperational = true;
              throw err;
            }
            default: {
              const err = new Error('AI service returned an unexpected status code.');
              err.statusCode = 502;
              err.isOperational = true;
              throw err;
            }
          }
        }

        // Validate Content-Type
        const contentType = response.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
          console.error(`[AI] Invalid content-type received from Gemini: ${contentType}`);
          const err = new Error('Invalid response format received from AI provider.');
          err.statusCode = 502;
          err.isOperational = true;
          throw err;
        }

        // Safely parse JSON response
        let data;
        let rawText = '';
        try {
          rawText = await response.text();
          if (rawText.trim().startsWith('<') || rawText.toLowerCase().includes('<!doctype html')) {
            throw new Error('Received HTML instead of JSON');
          }
          data = JSON.parse(rawText);
        } catch (jsonErr) {
          console.error('[AI] Failed to parse JSON response from Gemini');
          const err = new Error('Malformed response received from AI provider.');
          err.statusCode = 502;
          err.isOperational = true;
          throw err;
        }

        // Extract response candidate text
        const candidates = data.candidates;
        if (!Array.isArray(candidates) || candidates.length === 0) {
          const finishReason = data.promptFeedback?.blockReason || 'BLOCKED';
          const err = new Error(`Request could not be processed by AI provider (Reason: ${finishReason}).`);
          err.statusCode = 422;
          err.isOperational = true;
          throw err;
        }

        const firstCandidate = candidates[0];
        const parts = firstCandidate.content?.parts;
        if (!Array.isArray(parts) || parts.length === 0 || !parts[0].text) {
          const err = new Error('Empty response content received from AI provider.');
          err.statusCode = 502;
          err.isOperational = true;
          throw err;
        }

        const generatedText = parts[0].text.trim();
        if (!generatedText) {
          const err = new Error('Empty response content received from AI provider.');
          err.statusCode = 502;
          err.isOperational = true;
          throw err;
        }

        return {
          success: true,
          message: generatedText,
          provider: 'gemini'
        };

      } catch (err) {
        lastError = err;
        if (err.name === 'AbortError') {
          console.error('[AI] Request to Gemini timed out');
          const timeoutErr = new Error('AI provider request timed out. Please try again.');
          timeoutErr.statusCode = 504;
          timeoutErr.isOperational = true;
          throw timeoutErr;
        }

        if (err.isOperational) {
          throw err;
        }

        if (attempt < maxAttempts) {
          await new Promise(r => setTimeout(r, 1000));
          continue;
        }
      }
    }

    console.error(`[AI] Final failure calling Gemini: ${lastError?.message || 'Unknown network failure'}`);
    const finalErr = new Error('Failed to connect to AI service. Please verify network connectivity.');
    finalErr.statusCode = 503;
    finalErr.isOperational = true;
    throw finalErr;
  }
}

export const geminiService = new GeminiService();
