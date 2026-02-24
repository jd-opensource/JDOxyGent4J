# Repository Guidelines

## Project Structure & Module Organization

- Vite + Vue 3 + TypeScript entry points live in `src/main.ts` and `src/App.vue`, with routing defined under `src/router`.
- Views are grouped in `src/views` (e.g., `dashboard`, `knowledge-base`, `error`); shared UI sits in `src/components/common` and layout shells in `src/components/layout`.
- State lives in `src/stores` (Pinia), API clients and generated interfaces in `src/api`, configuration/constants in `src/config` and `src/constant`, and utilities/composables in `src/utils` and `src/composables`.
- Global styles are in `src/assets/styles` and `src/style.css`; static assets and the HTML shell stay in `public/` and `index.html`. Keep Ant Design Vue styles and Tailwind (preflight disabled) in mind when adding CSS.

## Build, Test, and Development Commands

- `pnpm install` — install dependencies (pnpm is expected; lockfile is pnpm-lock.yaml).
- `pnpm dev` — start the Vite dev server.
- `pnpm build` — type-check with `vue-tsc` then create the production bundle.
- `pnpm preview` — serve the production build locally.
- `pnpm lint` / `pnpm lint:fix` — run ESLint with the Antfu config (auto-fix with `:fix`). Pre-commit hooks run `eslint --fix` via lint-staged.

## Coding Style & Naming Conventions

- ESLint (`@antfu/eslint-config`) enforces 2-space indentation and single quotes; follow `<script setup>` patterns for Vue SFCs.
- Name Vue components in PascalCase files and use kebab-case when referencing them in templates (per `vue/component-name-in-template-casing`).
- Prefer multi-word component names even though the rule is relaxed; keep imports relative and avoid deep path aliases unless configured.
- Keep Tailwind utility ordering consistent (prettier-plugin-tailwindcss is available) and avoid re-enabling Tailwind preflight to prevent clashes with Ant Design.

## Testing Guidelines

- No automated test harness is present yet; at minimum run `pnpm lint` and `pnpm build` before opening a PR.
- When introducing non-trivial logic, add unit tests (Vitest + Vue Test Utils are recommended choices to add) and structure specs alongside the feature directory or in a parallel `__tests__` folder.
- Prefer deterministic data over live API calls; mock API layers in `src/api` for repeatable tests.

## Commit & Pull Request Guidelines

- Commit messages follow Conventional Commits (enforced by commitlint): `type(scope?): subject`. Allowed types include `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `revert` (see `commitlint.config.js`).
- Pre-commit and commit-msg hooks are managed by simple-git-hooks; do not skip them. Use focused, incremental commits rather than large batches.
- PRs should summarize intent, list key changes, note test evidence (commands run), and link any related issues or designs. Include screenshots/GIFs for UI changes and highlight any new env vars (use the `VITE_` prefix; never commit secrets).
