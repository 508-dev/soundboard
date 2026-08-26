import { pgTable, text, timestamp, uuid } from "drizzle-orm/pg-core";

// This is a placeholder contract, not product data. Replace it with the target
// repo's real tables when selecting Drizzle for TypeScript-side database access.
export const exampleRecords = pgTable("example_records", {
  id: uuid("id").primaryKey().defaultRandom(),
  name: text("name").notNull(),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});
