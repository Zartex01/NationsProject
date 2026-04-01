import { pgTable, text, integer } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";

export const playerPlaytimeTable = pgTable("player_playtime", {
  playerId: text("player_id").primaryKey(),
  totalSeconds: integer("total_seconds").default(0).notNull(),
  claimedSeconds: integer("claimed_seconds").default(0).notNull(),
});

export const insertPlayerPlaytimeSchema = createInsertSchema(playerPlaytimeTable);
export const selectPlayerPlaytimeSchema = createSelectSchema(playerPlaytimeTable);
export type InsertPlayerPlaytime = z.infer<typeof insertPlayerPlaytimeSchema>;
export type PlayerPlaytime = typeof playerPlaytimeTable.$inferSelect;
