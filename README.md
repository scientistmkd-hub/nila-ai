# Nila AI — Architecture & Setup Guide

## Architecture Overview

Nila AI implements a secure client-server architecture:

```
Android Nila AI App
       ↓ HTTPS (Clean JSON)
Nila Backend API (Node.js/Express)
       ↓ HTTPS (x-goog-api-key header)
Google Gemini API (gemini-2.5-flash)
```

### Security Architecture

- **Zero Secrets in Android Client**: The Android APK, Kotlin source, Compose UI, Android resources, and BuildConfig contain NO Gemini API keys.
- **Backend Environment Only**: The `GEMINI_API_KEY` is stored strictly as a server-side environment variable.
- **Header-Based Authentication**: The backend authenticates to Gemini via the `x-goog-api-key` HTTP header. The API key is NEVER passed in URL parameters or query strings.
- **Clean JSON Responses**: Android receives only structured JSON responses:
  ```json
  { "success": true, "message": "...", "provider": "gemini" }
  ```
  or
  ```json
  { "success": false, "message": "AI service is temporarily unavailable." }
  ```
- **Redaction**: Secrets and stack traces are stripped before logging or sending error responses.

---

## Backend Quick Start

### 1. Installation

```bash
cd backend
npm install
```

### 2. Configuration

Create `backend/.env` (do NOT commit this file):

```bash
GEMINI_API_KEY=your_actual_gemini_api_key_here
GEMINI_MODEL=gemini-2.5-flash
PORT=8080
NODE_ENV=production
REQUEST_TIMEOUT_MS=30000
# Optional client token:
# NILA_BACKEND_AUTH_TOKEN=your_secure_client_token
```

### 3. Run Backend Tests

```bash
cd backend
npm test
```

### 4. Start Backend Server

```bash
cd backend
npm start
```

---

## Android Client Configuration

- **Android Emulator**: Uses `http://10.0.2.2:8080` (maps to host `localhost:8080`).
- **Physical Device / Local LAN**: In Nila Settings, set Backend Server URL to your host's LAN IP (e.g. `http://192.168.1.100:8080`).
- **Production**: Deploy the backend to Cloud Run / VPS with HTTPS, and configure `https://api.yourdomain.com`.
- **Cleartext Policy**: Cleartext HTTP is globally prohibited by `network_security_config.xml` except for local development IPs (`10.0.2.2`, `localhost`, `127.0.0.1`).
