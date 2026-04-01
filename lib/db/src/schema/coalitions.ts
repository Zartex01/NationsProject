import { pgTable, text, integer } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";

export const coalitionsTable = pgTable("coalitions", {
  id: text("id").primaryKey(),
  name: text("name").notNull().unique(),
  leaderNationId: text("leader_nation_id").notNull(),
  createdAt: integer("created_at").notNull(),
});

export const insertCoalitionSchema = createInsertSchema(coalitionsTable);
export const selectCoalitionSchema = createSelectSchema(coalitionsTable);
export type InsertCoalition = z.infer<typeof insertCoalitionSchema>;
export type Coalition = typeof coalitionsTable.$inferSelect;
