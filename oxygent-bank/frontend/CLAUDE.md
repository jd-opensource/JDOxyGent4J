# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OxyBank - A Vue 3 + TypeScript knowledge management web application built with Vite (using Rolldown), Ant Design Vue, Alova for API management, and Pinia for state management. The project uses a custom authentication flow with OIDC integration.

**Core Features**:
- Knowledge base management (create, configure, document ingestion)
- Chunk viewing and editing with metadata management
- Recall/retrieval testing for RAG systems
- Annotation platform for data labeling

## Package Manager

**Always use `pnpm` for package management.** This project is configured with pnpm-specific settings (.npmrc with shamefully-hoist=true, pnpm overrides in package.json).

## Common Commands

### Development

```bash
pnpm dev                 # Start dev server on http://localhost:3232
pnpm build              # Type-check with vue-tsc and build for production
pnpm preview            # Preview production build
```

### Code Quality

```bash
pnpm lint               # Run ESLint
pnpm lint:fix           # Run ESLint with auto-fix
```

### Git Hooks

The project uses `simple-git-hooks` with:

- **pre-commit**: Runs `lint-staged` to lint modified files
- **commit-msg**: Validates commit messages with `commitlint`

**Commit Message Convention** (enforced by commitlint):

- `feat:` - New features
- `fix:` - Bug fixes
- `docs:` - Documentation changes
- `style:` - Code formatting
- `refactor:` - Code refactoring
- `perf:` - Performance improvements
- `test:` - Test additions/changes
- `chore:` - Build/tooling changes
- `revert:` - Revert previous commits

## Architecture

### Core Stack

- **Build Tool**: Vite (specifically `rolldown-vite@7.2.2`) - faster Rust-based bundler
- **Framework**: Vue 3 with `<script setup>` composition API
- **UI Library**: Ant Design Vue 4.x (auto-imported via unplugin-vue-components)
- **State Management**: Pinia with persistence plugin
- **Routing**: Vue Router with guard-based authentication
- **HTTP Client**: Alova (wraps Axios) with custom interceptors
- **Styling**: Tailwind CSS 3.x + Ant Design Vue (hybrid approach)

### Auto-Import Configuration

- Vue Composition API and Vue Router are auto-imported (via unplugin-auto-import)
- Ant Design Vue components are auto-imported (via unplugin-vue-components)
- Generated types: `.eslintrc-auto-import.json` and `components.d.ts`

### API Layer Architecture

**Alova-based API Management** - The project uses Alova for declarative HTTP requests:

1. **OpenAPI Code Generation**:
   - OpenAPI spec: `openapi/swagger.json`
   - Config: `alova.config.ts`
   - Generated files (ignored by ESLint):
     - `src/api/apiDefinitions.ts` - API endpoint definitions
     - `src/api/createApis.ts` - API factory functions
     - `src/api/globals.d.ts` - Global type definitions

2. **Custom Axios Instance** (`src/api/axios.ts`):
   - Adds required query params to all requests: `functionId`, `appid`, `loginType`
   - Handles 401/403/302 with OIDC authentication flow
   - On auth failures, fetches `/oidc/authorize` and injects `oidc.code` param
   - Redirects to logout URL (`REDIRECT_URL_LOGOUT`) when auth fails

3. **Alova Instance** (`src/api/index.ts`):
   - Wraps custom Axios with Alova adapter
   - Adds Bearer token from localStorage to all requests
   - Global error handling with Ant Design notifications
   - Exposes APIs globally via `globalThis.Apis`
   - Handles FastAPI validation errors (detail array format)
   - Supports `rawResponse` config for bypassing default response handling

### Authentication Flow

1. Request fails with 401/403/302
2. Interceptor fetches `location` header URL
3. GET request to location returns `{type: 'OIDC_CODE', value: 'xxx'}`
4. Original request retried with `oidc.code` query param
5. On complete failure, redirects to `REDIRECT_URL_LOGOUT`

### Router Architecture

- **Routes**: Defined in `src/router/routes.ts` using lazy-loaded components
- **Guards**: `src/router/guards.ts` handles auth checks and page titles
- **Meta Fields**: `title`, `requiresAuth`
- Default redirect: `/` → `/banks`
- 404 catch-all: `/:pathMatch(.*)* → /404`

### State Management

- **Pinia Store**: `src/stores/index.ts` with persistence plugin
- **Store Modules**: Located in `src/stores/modules/`
  - `app.ts` - Application-wide state
  - `user.ts` - User authentication state

### Styling Strategy

**Tailwind + Ant Design Hybrid**:

- Tailwind configured to avoid conflicts with Ant Design:
  - `important: '#app'` - Scopes Tailwind to app container
  - `preflight: false` - Disables Tailwind's CSS reset
  - Breakpoints aligned with Ant Design (`xs: 480px`, `sm: 576px`, etc.)
  - Primary color palette matches Ant Design blue
- Ant Design CSS loaded via `ant-design-vue/dist/reset.css`
- Custom styles: `src/assets/styles/variables.css` and `index.css`

### Path Aliases

```typescript
'~': project root
'@': src directory
```

## Component Organization

### Layout System

- `src/components/layout/AppLayout.vue` - Main layout wrapper
- `src/components/layout/AppHeader.vue` - Header component
- `src/components/layout/AppSider.vue` - Sidebar navigation
- `src/components/layout/AppFooter.vue` - Footer component

### Views Structure

- `src/views/knowledge/` - Knowledge base management
  - `index.vue` - Knowledge base list
  - `create/index.vue` - Multi-step knowledge base creation wizard
  - `detail/index.vue` - Knowledge base detail with document table
  - `chunks/index.vue` - Document chunk viewing and editing
  - `recall/index.vue` - Retrieval/recall testing for RAG
  - `setting/index.vue` - Knowledge base settings
- `src/views/annotation/` - Annotation platform
- `src/views/error/` - Error pages (404, 500)

## ESLint Configuration

Uses `@antfu/eslint-config` with:

- Vue 3 support enabled
- TypeScript support enabled
- Formatters enabled (includes Prettier-like formatting)
- Custom rules:
  - `vue/multi-word-component-names`: OFF
  - `vue/component-name-in-template-casing`: kebab-case enforced
- Auto-generated files excluded from linting (API definitions, types)

## Environment Variables

The project uses environment-specific `.env` files:

- `.env` - Base configuration
- `.env.development` - Development overrides
- `.env.production` - Production overrides

Expected variables (from `src/config/index.ts`):

- `VITE_APP_TITLE` - Application title
- `VITE_APP_VERSION` - Application version
- `VITE_API_BASE_URL` - API base URL

## Important Files to Preserve

**Never modify these auto-generated files manually**:

- `src/api/apiDefinitions.ts`
- `src/api/createApis.ts`
- `src/api/globals.d.ts`
- `.eslintrc-auto-import.json`
- `components.d.ts`

To regenerate API definitions, update `openapi/swagger.json` and rebuild the project.

## TypeScript Configuration

- Strict mode enabled
- Vue-specific TypeScript support via `@vue/tsconfig`
- Global type definitions in `src/types/global.d.ts` and `src/types/env.d.ts`

## Development Workflow

1. **Adding New Pages**:
   - Create view in `src/views/[feature]/index.vue`
   - Add route to `src/router/routes.ts`
   - Update meta fields (`title`, `requiresAuth`)
   - For nested knowledge base routes, use `/knowledge/:id/[page]` pattern

2. **Adding API Calls**:
   - Update OpenAPI spec in `openapi/swagger.json`
   - Rebuild to regenerate API definitions
   - Access via `globalThis.Apis` or import from `src/api`
   - Use `rawResponse: true` in `$$userConfigMap` for endpoints that need raw response handling

3. **Component Development**:
   - Use Composition API with `<script setup>`
   - Ant Design components auto-import (no explicit imports needed)
   - Vue APIs (ref, computed, etc.) auto-import
   - For icons, import from `@ant-design/icons-vue`

4. **Styling**:
   - Prefer Ant Design components for UI consistency
   - Use Tailwind utilities for custom styling
   - Scope custom styles to avoid Ant Design conflicts
   - Sidebar uses dark theme (`theme="dark"`)
