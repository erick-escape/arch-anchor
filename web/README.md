# Arch Anchor — Web

React 18 + TypeScript + Vite frontend: a homepage to import projects, the project list, and the
analysis page, where the modules are drawn as a layered graph with React Flow, the
recommendations can be applied, and the recovered constraints are exported as a PDF. Light and
dark themes follow the system unless the viewer picks one.

See the [root README](../README.md) for setup, the analysis workflow, and known limitations.

## Commands

| Command | Description |
|---|---|
| `npm install` | Install dependencies |
| `npm run dev` | Dev server on `http://localhost:5173` |
| `npm test` | Run the vitest suite |
| `npm run lint` | Run ESLint |
| `npm run format` | Format with Prettier |
| `npm run build` | Type-check and build for production |

The backend must be running on port 8080: `vite.config.ts` proxies `/api` to it, and most
components call the API through relative paths that depend on that proxy.

## Where things are

| Path | What |
|---|---|
| `src/theme/` | `palette.ts`, the one source of every colour, turned into `--aa-*` CSS variables and the antd theme; a test checks every pair's WCAG contrast |
| `src/components/vocabulary/` | The drawing vocabulary: logo, class tile, module container, package chip, meter, priority badge, dependency arrow |
| `src/components/CommandPalette/` | The ⌘K palette; pages add their own commands with `useRegisterCommands` |
| `src/screens/Home/` | Import bar (GitHub URL, folder upload, page-wide drop), claims, how it works, research |
| `src/screens/Projects/` | Project cards |
| `src/screens/Analyze/` | The graph (`graph/`, laid out with dagre), the modules panel (`modules/`) and the recommendations panel (`recommendations/`) |
| `src/services/` | One small module per API resource; failures become sentences for the user |

Tests run in jsdom with Testing Library; `src/test/renderWithProviders.tsx` mounts a screen
the way `main.tsx` mounts the app, and `src/test/fakes/` holds the named fakes for HTTP, theme
storage and the colour scheme.
