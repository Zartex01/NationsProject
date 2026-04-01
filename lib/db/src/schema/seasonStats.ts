import { pgTable, text, integer, primaryKey } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";

export const seasonStatsTable = pgTable("season_stats", {
  playerId: text("player_id").notNull(),
  seasonNumber: integer("season_number").notNull(),
  playerName: text("player_name"),
  kills: integer("kills").default(0).notNull(),
  deaths: integer("deaths").default(0).notNull(),
  warsWon: integer("wars_won").default(0).notNull(),
  claims: integer("claims").default(0).notNull(),
}, (table) => [
  primaryKey({ columns: [table.playerId, table.seasonNumber] }),
]);

export const insertSeasonStatSchema = createInsertSchema(seasonStatsTable);
export const selectSeasonStatSchema = createSelectSchema(seasonStatsTable);
export type InsertSeasonStat = z.infer<typeof insertSeasonStatSchema>;
export type SeasonStat = typeof seasonStatsTable.$inferSelect;
