# CodeNow (码上记) — personal tech blog

Two apps in one repo: `codenow-backend` (Spring Boot 3.4.7, Java 21, Maven wrapper) and `codenow-frontend` (Vue 3 + Vite 8, npm). Docs and commit messages are in Chinese. There is no root-level npm project — the root `package.json` is a stub (do not `npm install` at root).

## Commands

Backend (`codenow-backend/`):
- Run dev: `.\mvnw.cmd spring-boot:run` (Windows) / `./mvnw spring-boot:run` — defaults to `local` profile
- Tests: `./mvnw --batch-mode clean test` — pure Mockito unit tests, no MySQL/Redis needed (Flyway disabled via `src/test/resources/application-test.yml`)
- Knife4j API docs: `http://localhost:8080/doc.html`

Frontend (`codenow-frontend/`, Node ^22.22.2 or >=24.15.0):
- `npm ci` then `npm run dev` — Vite proxies `/api` to `http://localhost:8080`
- Full CI gate: `npm run lint:check` (eslint **and** oxlint) → `npm run test:coverage` → `npm run build`

Required CI checks (GitHub branch protection): `backend-tests`, `frontend-quality`, `compose-smoke` (full-stack `docker compose` up + `scripts/smoke-test.sh`).

## Local dev prerequisites

- MySQL 8 on `localhost:3306`, db `codenow`, user `root`, password `123456`; init via `codenow-backend/sql/init.sql`
- Redis on `localhost:6379`, password `123456`, db index 1 (see `application.yaml`)
- Default login `admin` / `123456`; login requires a math captcha from `/api/auth/captcha`
- `application-local.yml` is gitignored and holds real SMTP secrets — never commit it
- Admin UI at `:5173`, public blog at `:5173/blog`

## Database & migrations (read before touching schema)

- Flyway migrations in `codenow-backend/src/main/resources/db/migration/` (V1–V9) auto-apply at backend startup with `validate-on-migrate: true` — never edit an already-applied migration; add `V<n+1>` files
- `baseline-on-migrate: true` + `baseline-version: 5`: existing databases are baselined at 5 (only V5+ run); fresh DBs run V1–V9
- `codenow-backend/sql/init.sql` is destructive (`DROP TABLE`); Docker's MySQL image runs it only when the `mysql_data` volume is first created — never run it against an existing database
- `sql/migration-*.sql` / `rollback-*.sql` are manual scripts for existing deployments; upgrade/rollback procedures live in `DEPLOY.md`
- `/api/health` returns DB status and `dbSchemaVersion` (CI asserts on it)

## Architecture notes

- Roles: `ADMIN` / `AUTHOR` / `USER`. Since V6, categories and tags are per-author isolated — an AUTHOR manages only their own articles/categories/tags; tags are platform-shared and tag CRUD is admin-only
- Blog-frontend APIs are public (no auth); admin APIs use Sa-Token (`Authorization` header)
- Element Plus components are auto-imported via `unplugin-auto-import`/`unplugin-vue-components` — don't add manual imports (plugins are disabled in test mode)

## Testing quirks

- Coverage thresholds in `vite.config.js` apply **only** to `ImageUpload.vue`, `CommentForm.vue`, and `src/utils/format.js`; other specs just run
- Vitest uses jsdom with `src/test/setup.js`
- `scripts/smoke-test.sh` / `smoke-test.ps1`: E2E smoke against a running stack; requires `python3` (parses the login captcha SVG); leaves test data in the DB; configurable via `BACKEND_URL`, `FRONTEND_URL`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`

## Deploy

- `docker-compose.yml` fails to start unless `.env` provides `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET` (also `CORS_ALLOWED_ORIGINS` must exactly match the browser origin; `STORAGE_TYPE` is `local` or `oss`)
- Never use `docker compose down -v` in production — it deletes MySQL, Redis, and uploads volumes
- Entrypoints: `deploy.sh` / `deploy.bat`; full ops manual in `DEPLOY.md`
