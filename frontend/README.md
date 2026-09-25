# Credit Lens frontend

This directory contains the React and TypeScript user interface. It supports
new requests, successful request history, and stored extract details.

## Prerequisites

- Node.js 22.12 or newer
- npm 11 or newer
- Credit Lens backend available at `http://localhost:8080`

The Vite development server proxies `/api` to the backend.

## Development

```bash
npm ci
npm run dev
```

Open `http://localhost:5173`.

## Checks

```bash
npm run lint
npm test
npm run build
```

For the complete Docker Compose startup and demo, use the repository
[README](../README.md).
