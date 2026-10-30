# Database Choice: Moving from Supabase to Neon (PostgreSQL)

## Decision
We initially chose **Supabase** for our cloud PostgreSQL database, but are **moving to Neon** after running into reliability issues with Supabase during development.

## Context
- Backend: Spring Boot
- Project is still in active development — schema (tables, columns, relationships) will keep changing as we build
- Multiple team members need to work against the same live database
- We plan to add AI features later: AI-based learning support and AI-generated notes from lecture transcripts
- We want a free tier to start, with the option to upgrade later

## Why We Moved

### Reliability
Supabase was intermittently unreachable/unresponsive during development, which blocked work. This was the deciding factor — a database the team can't reliably connect to isn't usable, regardless of its other features.

## Supabase vs Neon

| Factor | Supabase | Neon |
|---|---|---|
| Core | Postgres + bundled auth, storage, realtime, auto-generated API | Pure serverless Postgres — no bundled extras |
| Dev/Prod separation | 2 free projects | Instant copy-on-write branching |
| pgvector (AI/RAG support) | Yes, plus native embedding generation | Yes, plus embedding generation |
| Connection pooling | PgBouncer built in | Built-in HTTP/WebSocket proxy |
| Cold starts | None on DB | Suspends after 5 min idle, wakes in ~300ms |
| Reliability (our experience) | Intermittent issues during dev | Not yet tested at scale — to confirm |
| Free tier | 500MB–1GB storage | 0.5GB storage, scale-to-zero |

## Why Neon Is the Better Fit Now

### 1. Standard PostgreSQL, no lock-in
Same as Supabase — real Postgres underneath, so our Spring Boot app connects with the standard JDBC Postgres driver, no special adapters needed.

### 2. Instant branching for dev/prod and team workflows
Instead of maintaining two separate Supabase projects, Neon lets us branch off the main database instantly (copy-on-write). A team member can branch off `main` to test a risky schema change without touching the shared dev database, then discard the branch if it doesn't work out.

### 3. Schema can still evolve freely during development
`spring.jpa.hibernate.ddl-auto=update` works the same way — Hibernate auto-adds tables/columns/relationships as our `@Entity` classes change. No change to our workflow here.

### 4. pgvector support — ready for our AI features
Neon also ships with `pgvector` and embedding generation, so our planned RAG pipeline for AI-based transcript notes is unaffected by the switch:
1. Break lecture transcripts into chunks
2. Generate embeddings for each chunk
3. Store embeddings in Postgres using pgvector
4. Run similarity search to retrieve relevant chunks for notes/Q&A

### 5. Free tier is enough for a course project
- 0.5 GB storage, scale-to-zero compute
- Branching included, no credit card required
- Sufficient for a team project of our size

## What Changes in the Setup
- Connection string format changes (Neon host instead of Supabase host)
- `application-dev.properties` / `application-prod.properties` updated with the new URL
- Same Postgres JDBC driver in `pom.xml` — no code changes needed
- Dev/prod split now handled via a Neon **branch** instead of a second project

## Next Steps
1. Create Neon project and main branch
2. Create a `dev` branch off `main`
3. Get connection strings for both branches
4. Update Spring Boot `application-dev.properties` / `application-prod.properties`
5. Share connection strings with team (private channel, not committed to Git)
6. Migrate/recreate schema via Spring Boot entities on the new database
