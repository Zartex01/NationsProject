import { pgTable, text, primaryKey } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";

export const nationAlliesTable = pgTable("nation_allies", {
  nationA: text("nation_a").notNull(),
  nationB: text("nation_b").notNull(),
}, (table) => [
  primaryKey({ columns: [table.nationA, table.nationB] }),
]);

export const insertNationAllySchema = createInsertSchema(nationAlliesTable);
export const selectNationAllySchema = createSelectSchema(nationAlliesTable);
export type InsertNationAlly = z.infer<typeof insertNationAllySchema>;
export type NationAlly = typeof nationAlliesTable.$inferSelect;
