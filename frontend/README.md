# Frontend

This directory is reserved for frontend/dashboard UI components. Currently, the Honeypot API is a **backend-only** service.

## Future Scope

When a frontend is developed, it should include:

- **Dashboard** — Real-time scam engagement monitoring
- **API Integration Layer** — Typed client for `/api/honeypot/message`
- **Session Viewer** — Browse active/completed honeypot sessions
- **Intelligence Feed** — View extracted UPI IDs, phone numbers, phishing links
- **Environment Configuration** — `.env` files for API base URL & keys
- **Build Configuration** — Vite/Next.js bundler setup
- **Dockerfile** — Production container for serving the frontend

## Getting Started

```bash
# When frontend code is added:
cd frontend
npm install
npm run dev
```
