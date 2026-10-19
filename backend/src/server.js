import app from './app.js';
import { config } from './config.js';

const server = app.listen(config.port, '0.0.0.0', () => {
  console.log(`=============================================`);
  console.log(`🚀 Nila AI Backend Server running`);
  console.log(`📡 Port: ${config.port}`);
  console.log(`🌍 Environment: ${config.nodeEnv}`);
  console.log(`🤖 Gemini Configured: ${config.isGeminiConfigured ? 'YES' : 'NO (Set GEMINI_API_KEY)'}`);
  console.log(`🎯 Model: ${config.geminiModel}`);
  console.log(`🔒 Authentication: ${config.backendAuthToken ? 'ENFORCED' : 'OPEN (Development)'}`);
  console.log(`=============================================`);
});

// Graceful shutdown handling
process.on('SIGTERM', () => {
  console.log('SIGTERM received. Closing HTTP server gracefully...');
  server.close(() => {
    console.log('HTTP server closed.');
    process.exit(0);
  });
});

process.on('SIGINT', () => {
  console.log('SIGINT received. Closing HTTP server gracefully...');
  server.close(() => {
    console.log('HTTP server closed.');
    process.exit(0);
  });
});
