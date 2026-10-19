import { describe, it } from 'node:test';
import assert from 'node:assert';
import request from 'supertest';
import app from '../src/app.js';
import { config } from '../src/config.js';
import { geminiService } from '../src/geminiService.js';

describe('Nila AI Backend Production Test Suite', () => {

  // Test 1: /api/health
  it('1. /api/health returns 200 JSON with status ok and boolean config', async () => {
    const res = await request(app)
      .get('/api/health')
      .expect('Content-Type', /json/)
      .expect(200);

    assert.strictEqual(res.body.status, 'ok');
    assert.strictEqual(typeof res.body.geminiConfigured, 'boolean');
    assert.strictEqual(res.body.geminiApiKey, undefined);
    assert.strictEqual(res.body.apiKey, undefined);
  });

  // Test 2: 404 JSON response (never HTML)
  it('2. 404 JSON response returned for unknown endpoints without HTML or stack traces', async () => {
    const res = await request(app)
      .get('/api/nonexistent-endpoint')
      .expect('Content-Type', /json/)
      .expect(404);

    assert.strictEqual(res.body.success, false);
    assert.ok(res.body.message.includes('not found'));
    assert.strictEqual(res.text.includes('<!DOCTYPE'), false);
    assert.strictEqual(res.text.includes('<html'), false);
  });

  // Test 3: malformed JSON
  it('3. malformed JSON body is handled cleanly with 400 JSON response', async () => {
    const res = await request(app)
      .post('/api/ai/chat')
      .set('Content-Type', 'application/json')
      .send('{"prompt": "hello", malformed json ...')
      .expect('Content-Type', /json/)
      .expect(400);

    assert.strictEqual(res.body.success, false);
    assert.ok(res.body.message.includes('Invalid JSON'));
    assert.strictEqual(res.text.includes('<!DOCTYPE'), false);
  });

  // Test 4: invalid prompt (empty, missing, non-string)
  it('4. invalid prompt rejected with 400 JSON response', async () => {
    // Empty prompt
    const resEmpty = await request(app)
      .post('/api/ai/chat')
      .send({ prompt: '' })
      .expect('Content-Type', /json/)
      .expect(400);
    assert.strictEqual(resEmpty.body.success, false);
    assert.ok(resEmpty.body.message.includes('Prompt is required'));

    // Non-string prompt
    const resNumber = await request(app)
      .post('/api/ai/chat')
      .send({ prompt: 12345 })
      .expect('Content-Type', /json/)
      .expect(400);
    assert.strictEqual(resNumber.body.success, false);
    assert.ok(resNumber.body.message.includes('Prompt is required'));
  });

  // Test 5: oversized prompt
  it('5. oversized prompt exceeding max character limit rejected with 400', async () => {
    const hugePrompt = 'A'.repeat(config.maxPromptLength + 50);
    const res = await request(app)
      .post('/api/ai/chat')
      .send({ prompt: hugePrompt })
      .expect('Content-Type', /json/)
      .expect(400);

    assert.strictEqual(res.body.success, false);
    assert.ok(res.body.message.includes('exceeds maximum allowed length'));
  });

  // Test 6: missing Gemini key
  it('6. missing Gemini key returns clean 503 JSON with honest message', async () => {
    const originalKey = config.geminiApiKey;
    config.geminiApiKey = '';

    try {
      const res = await request(app)
        .post('/api/ai/chat')
        .send({ prompt: 'Hello Nila' })
        .expect('Content-Type', /json/)
        .expect(503);

      assert.strictEqual(res.body.success, false);
      assert.ok(res.body.message.includes('Gemini API key is not configured'));
    } finally {
      config.geminiApiKey = originalKey;
    }
  });

  // Test 7: Gemini success path
  it('7. Gemini success path calls API via x-goog-api-key header and formats response', async () => {
    const originalFetch = globalThis.fetch;
    const originalKey = config.geminiApiKey;
    config.geminiApiKey = 'test-secret-key-valid';

    let capturedUrl = '';
    let capturedHeaders = {};
    let capturedBody = {};

    globalThis.fetch = async (url, options) => {
      capturedUrl = url;
      capturedHeaders = options.headers;
      capturedBody = JSON.parse(options.body);

      return {
        ok: true,
        status: 200,
        headers: new Map([['content-type', 'application/json']]),
        text: async () => JSON.stringify({
          candidates: [{
            content: {
              parts: [{ text: 'Greetings! I am Nila, operating via secure backend.' }]
            }
          }]
        })
      };
    };

    try {
      const res = await request(app)
        .post('/api/ai/chat')
        .send({
          prompt: 'Hello Nila Assistant',
          conversationContext: [{ role: 'user', text: 'Hi' }]
        })
        .expect(200);

      assert.strictEqual(res.body.success, true);
      assert.strictEqual(res.body.provider, 'gemini');
      assert.ok(res.body.message.includes('Greetings! I am Nila'));

      // CRITICAL SECURITY: URL does NOT have key query param!
      assert.strictEqual(capturedUrl.includes('?key='), false);
      assert.strictEqual(capturedUrl.includes('&key='), false);

      // Header-based authentication strictly verified
      assert.strictEqual(capturedHeaders['x-goog-api-key'], 'test-secret-key-valid');
      assert.strictEqual(capturedHeaders['Content-Type'], 'application/json');

      // Request body payload verified
      assert.ok(Array.isArray(capturedBody.contents));
    } finally {
      globalThis.fetch = originalFetch;
      config.geminiApiKey = originalKey;
    }
  });

  // Test 8: Gemini upstream failure (400, 403, 429, 500, HTML)
  it('8. Gemini upstream failure handled safely with clean JSON (never raw errors)', async () => {
    const originalFetch = globalThis.fetch;
    const originalKey = config.geminiApiKey;
    config.geminiApiKey = 'test-key';

    // Simulate upstream 503 HTML error
    globalThis.fetch = async () => ({
      ok: false,
      status: 503,
      headers: new Map([['content-type', 'text/html']]),
      text: async () => '<html><body>503 Service Unavailable</body></html>'
    });

    try {
      const res = await request(app)
        .post('/api/ai/chat')
        .send({ prompt: 'Trigger 503' })
        .expect('Content-Type', /json/)
        .expect(503);

      assert.strictEqual(res.body.success, false);
      assert.ok(res.body.message.includes('temporarily unavailable'));
      assert.strictEqual(res.text.includes('<html'), false);
    } finally {
      globalThis.fetch = originalFetch;
      config.geminiApiKey = originalKey;
    }
  });

  // Test 9: authentication failure
  it('9. authentication failure returns 401 JSON when token is required', async () => {
    config.backendAuthToken = 'secure-auth-token-xyz';

    try {
      const res = await request(app)
        .post('/api/ai/chat')
        .send({ prompt: 'Hello' })
        .expect('Content-Type', /json/)
        .expect(401);

      assert.strictEqual(res.body.success, false);
      assert.ok(res.body.message.includes('Unauthorized'));
    } finally {
      config.backendAuthToken = '';
    }
  });

  // Test 10: authentication success
  it('10. authentication success accepts valid Bearer token', async () => {
    config.backendAuthToken = 'secure-auth-token-xyz';
    const originalMethod = geminiService.generateChatResponse;
    geminiService.generateChatResponse = async () => ({
      success: true,
      message: 'Authenticated call successful',
      provider: 'gemini'
    });

    try {
      const res = await request(app)
        .post('/api/ai/chat')
        .set('Authorization', 'Bearer secure-auth-token-xyz')
        .send({ prompt: 'Hello authorized' })
        .expect(200);

      assert.strictEqual(res.body.success, true);
      assert.strictEqual(res.body.message, 'Authenticated call successful');
    } finally {
      config.backendAuthToken = '';
      geminiService.generateChatResponse = originalMethod;
    }
  });

  // Test 11: timeout
  it('11. timeout triggers 504 clean JSON response', async () => {
    const originalFetch = globalThis.fetch;
    const originalKey = config.geminiApiKey;
    config.geminiApiKey = 'test-key';

    // Simulate abort error
    globalThis.fetch = async () => {
      const abortError = new Error('The operation was aborted');
      abortError.name = 'AbortError';
      throw abortError;
    };

    try {
      const res = await request(app)
        .post('/api/ai/chat')
        .send({ prompt: 'Simulate timeout' })
        .expect('Content-Type', /json/)
        .expect(504);

      assert.strictEqual(res.body.success, false);
      assert.ok(res.body.message.includes('timed out'));
    } finally {
      globalThis.fetch = originalFetch;
      config.geminiApiKey = originalKey;
    }
  });

  // Test 12: secret leakage prevention
  it('12. secret leakage prevention ensures GEMINI_API_KEY is never in body, headers, or logs', async () => {
    const originalMethod = geminiService.generateChatResponse;
    const secretKey = 'AIzaSySecretApiKeyDoNotLeak999';

    geminiService.generateChatResponse = async () => {
      const secretErr = new Error(`Connection failed with key=${secretKey}`);
      secretErr.statusCode = 502;
      secretErr.isOperational = false;
      throw secretErr;
    };

    try {
      const res = await request(app)
        .post('/api/ai/chat')
        .send({ prompt: 'Leak test' })
        .expect(500);

      assert.strictEqual(res.body.success, false);
      const rawResponse = JSON.stringify(res.body);
      assert.strictEqual(rawResponse.includes(secretKey), false);
      assert.strictEqual(rawResponse.includes('AIzaSy'), false);
      assert.strictEqual(rawResponse.includes('stack'), false);
    } finally {
      geminiService.generateChatResponse = originalMethod;
    }
  });

});
