import express from 'express';
import cors from 'cors';
import healthRouter from './routes/health.js';
import aiRouter from './routes/ai.js';
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js';
import { config } from './config.js';

const app = express();

// Secure CORS configuration
const corsOptions = {
  origin: (origin, callback) => {
    if (!origin || config.allowedOrigins.includes('*') || config.allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error('CORS policy violation'));
    }
  },
  methods: ['GET', 'POST', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'x-api-key'],
  maxAge: 86400
};

app.use(cors(corsOptions));

// JSON Body Parser with strict limit
app.use(express.json({ limit: '1mb' }));

// Sanitize request logging (never logs tokens, passwords, or full prompts)
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    console.log(`[HTTP] ${req.method} ${req.path} -> ${res.statusCode} (${duration}ms)`);
  });
  next();
});

// Mount API routes
app.use('/api', healthRouter);
app.use('/api/ai', aiRouter);

// 404 handler
app.use(notFoundHandler);

// Central error handler
app.use(errorHandler);

export default app;
