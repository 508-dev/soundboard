# Add Migration

Use when a target repo needs a database schema migration.

## Workflow

1. Inspect the repo's existing migration system.
2. For Python services, prefer Alembic under the owning app when Alembic is the
   established migration tool.
3. For TypeScript services, prefer Drizzle migrations when that is the established data layer.
4. Update schema code and migration files together.
5. Add or update tests that prove the new schema behavior.
6. Document manual migration or rollback steps when needed.

Do not invent a second migration system in a repo that already has one.
