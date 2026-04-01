import { pgTable, text, real, integer, boolean, timestamp } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";

export const nationsTable = pgTable("nations", {
  id: text("id").primaryKey(),
  name: text("name").notNull().unique(),
  description: text("description").default(""),
  leaderId: text("leader_id").notNull(),
  bankBalance: real("bank_balance").default(0).notNull(),
  seasonPoints: integer("season_points").default(0).notNull(),
  level: integer("level").default(1).notNull(),
  xp: real("xp").default(0).notNull(),
  open: boolean("open").default(false).notNull(),
  coalitionId: text("coalition_id"),
  createdAt: integer("created_at").notNull(),
});

export const insertNationSchema = createInsertSchema(nationsTable);
export const selectNationSchema = createSelectSchema(nationsTable);
export type InsertNation = z.infer<typeof insertNationSchema>;
export type Nation = typeof nationsTable.$inferSelect;
