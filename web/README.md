# Arch Anchor — Web

React 18 + TypeScript + Vite frontend. Renders the module dependency graph with ReactFlow,
drives all module operations, and exports the recovered architectural constraints as a PDF.

See the [root README](../README.md) for setup, the analysis workflow, and known limitations.

## Commands

| Command | Description |
|---|---|
| `npm install` | Install dependencies |
| `npm run dev` | Dev server on `http://localhost:5173` |
| `npm test` | Run the vitest suite |
| `npm run lint` | Run ESLint |
| `npm run format` | Format with Prettier |
| `npm run build` | Type-check and build — currently fails on pre-existing type errors |

The backend must be running on port 8080: `vite.config.ts` proxies `/api` to it, and most
components call the API through relative paths that depend on that proxy.
