import { pgTable, integer, boolean } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";

export const seasonsTable = pgTable("seasons", {
  seasonNumber: integer("season_number").primaryKey(),
  startedAt: integer("started_at").notNull(),
  endedAt: integer("ended_at"),
  current: boolean("current").default(false).notNull(),
});

export const insertSeasonSchema = createInsertSchema(seasonsTable);
export const selectSeasonSchema = createSelectSchema(seasonsTable);
export type InsertSeason = z.infer<typeof insertSeasonSchema>;
export type Season = typeof seasonsTable.$inferSelect;
