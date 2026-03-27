# NationsEpoque

A multi-faceted development project centered around a Minecraft "Nations" plugin with historical epochs, combined with a Node.js/React web ecosystem for API services and UI component visualization.

## Architecture

### Minecraft Plugin (`nations-plugin/`)
- Java 21, Paper API (1.21.4), Maven build system
- SQLite (primary) with YAML fallback for persistence
- Features: land claiming, historical epochs, economic systems, warfare mechanics

#### Grade Hierarchy
JOUEUR → SOUTIEN → HÉROS → CHEVALIER → PREMIUM → ROI

#### Kit System (`fr/nations/kit/KitManager.java`)
- `/kit [heros|chevalier|premium]` — 24h cooldown per kit
- Kit Héros: armure fer Prot II, épée diamant Sharp II, arc Power I
- Kit Chevalier: armure fer Prot III, épée diamant Sharp III + Fire Aspect I, arc Power II
- Kit Premium: armure diamant Prot III, épée diamant Sharp IV, arc Power III + Infinity

#### Grade Advantage Commands
| Grade | Commandes |
|-------|-----------|
| Héros | `/craft`, `/pweather`, `/ptime` |
| Chevalier | `/furnace [all]`, `/stonecutter`, `/anvil`, `/back`, `/ec`, `/xpb` |
| Premium | `/repair [all]`, `/nick` |

#### Future Permissions (already in plugin.yml, à implémenter)
- `nations.hotel.slots.*` — emplacements Hôtel des Ventes
- `nations.mine.heros` — accès mine secondaire
- `nations.sell.all` — /sell all (shop)

### TypeScript Monorepo (pnpm workspaces)

#### Artifacts
- **`artifacts/api-server/`** — Express 5 backend, port 8080, path `/api`
- **`artifacts/mockup-sandbox/`** — React + Vite UI component preview, port 8081, path `/__mockup`

#### Shared Libraries (`lib/`)
- **`lib/api-spec/`** — OpenAPI 3.1 spec (`openapi.yaml`), Orval codegen config
- **`lib/db/`** — Drizzle ORM schema definitions (PostgreSQL)
- **`lib/api-zod/`** — Generated Zod schemas from the API spec
- **`lib/api-client-react/`** — Generated React Query hooks

## Tech Stack
- **Frontend**: React 19, Vite 7, Tailwind CSS 4, Radix UI, Lucide React, Framer Motion
- **Backend**: Node.js ESM, Express 5, Pino logging
- **Database**: Drizzle ORM (PostgreSQL), Drizzle Kit
- **API/Tooling**: OpenAPI 3.1, Orval (codegen), Zod, TypeScript

## Workflows
- **Start application**: Runs the mockup-sandbox dev server
  - Command: `PORT=8081 BASE_PATH=/__mockup pnpm --filter @workspace/mockup-sandbox run dev`
  - Port: 8081

## Key Files
- `nations-plugin/pom.xml` — Java dependencies
- `nations-plugin/src/main/java/fr/nations/NationsPlugin.java` — Plugin entry point
- `lib/api-spec/openapi.yaml` — API source of truth
- `lib/db/src/schema/index.ts` — Database models
- `artifacts/api-server/src/app.ts` — Express server config
- `artifacts/mockup-sandbox/src/App.tsx` — Component preview renderer
