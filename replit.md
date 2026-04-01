# NationsEpoque

A multi-faceted development project centered around a Minecraft "Nations" plugin with historical epochs, combined with a Node.js/React web ecosystem for API services and UI component visualization.

## Architecture

### Minecraft Plugin (`nations-plugin/`)
- Java 21, Paper API (1.21.4), Maven build system
- SQLite (in-plugin) for local data persistence
- Features: land claiming, historical epochs, economic systems, warfare mechanics
- HDV (Hôtel des Ventes) marketplace: `/hdv sell` opens GUI to list items with price, optional pub (+2%), 5% sell tax; `/hdv` to browse/buy
- Nation pub command: `/npub <message>` broadcasts nation advertisement for 1000 coins

### TypeScript Monorepo (pnpm workspaces)

#### Artifacts
- **`artifacts/api-server/`** — Express 5 backend, port 8080, path `/api`
- **`artifacts/mockup-sandbox/`** — React + Vite UI component preview, port 8081, path `/__mockup`

#### Shared Libraries (`lib/`)
- **`lib/api-spec/`** — OpenAPI 3.1 spec (`openapi.yaml`), Orval codegen config
- **`lib/db/`** — Drizzle ORM schema definitions (PostgreSQL)
- **`lib/api-zod/`** — Generated Zod schemas from the API spec
- **`lib/api-client-react/`** — Generated React Query hooks

## Database Schema (PostgreSQL via Drizzle ORM)
All tables mirror the Minecraft plugin's SQLite structure:
- `nations` — Nation data (name, level, xp, bank, season points)
- `nation_members` — Player membership per nation
- `nation_allies` — Alliance pairs between nations
- `coalitions` — Nation coalitions
- `coalition_members` — Nations per coalition
- `claimed_chunks` — Territory claims (world, chunkX, chunkZ)
- `player_accounts` — Player economy balances
- `player_grades` — Player grades/levels
- `player_playtime` — Total and claimed playtime
- `wars` — War records (attacker, defender, kills, status)
- `seasons` — Season lifecycle
- `season_stats` — Per-player per-season stats
- `custom_roles` — Nation custom roles with permissions

## API Endpoints (`/api`)
- `GET /healthz` — Health check
- `GET /nations` — List nations (with sort, pagination)
- `GET /nations/:id` — Nation detail (with members + allies)
- `GET /nations/:id/members` — Nation members
- `GET /nations/:id/claims` — Claimed chunks
- `GET /nations/:id/wars` — Wars involving a nation
- `GET /players/:id` — Player profile (account + grade + playtime)
- `GET /players/:id/stats` — Player season stats
- `GET /wars` — List all wars (with status filter, pagination)
- `GET /wars/:id` — War detail
- `GET /coalitions` — List coalitions
- `GET /coalitions/:id` — Coalition detail
- `GET /seasons` — List seasons
- `GET /seasons/current` — Current active season
- `GET /seasons/:number/stats` — Season stats leaderboard
- `GET /leaderboard/nations` — Top nations by season points
- `GET /leaderboard/players` — Top players by kills in current season

## Tech Stack
- **Frontend**: React 19, Vite 7, Tailwind CSS 4, Radix UI, Lucide React, Framer Motion
- **Backend**: Node.js ESM, Express 5, Pino logging
- **Database**: Drizzle ORM (PostgreSQL), Drizzle Kit
- **API/Tooling**: OpenAPI 3.1, Orval (codegen), Zod, TypeScript

## Workflows
- **Start application**: Express API server on port 8080
  - Command: `PORT=8080 pnpm --filter @workspace/api-server run dev`
- **Component Preview Server**: Vite mockup sandbox on port 8081
  - Command: `PORT=8081 BASE_PATH=/__mockup pnpm --filter @workspace/mockup-sandbox run dev`

## Codegen
After modifying `lib/api-spec/openapi.yaml`, regenerate types with:
```
cd lib/api-spec && pnpm run codegen
```

After modifying `lib/db/src/schema`, push to DB with:
```
cd lib/db && pnpm run push
```

## Key Files
- `nations-plugin/pom.xml` — Java dependencies
- `nations-plugin/src/main/java/fr/nations/NationsPlugin.java` — Plugin entry point
- `nations-plugin/src/main/java/fr/nations/database/DatabaseManager.java` — SQLite schema
- `lib/api-spec/openapi.yaml` — API source of truth
- `lib/db/src/schema/index.ts` — All DB model exports
- `artifacts/api-server/src/routes/` — All API route handlers
- `artifacts/api-server/src/app.ts` — Express server config
- `artifacts/mockup-sandbox/src/App.tsx` — Component preview renderer
