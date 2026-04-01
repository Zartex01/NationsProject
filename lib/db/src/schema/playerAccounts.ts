import { pgTable, text, real } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";

export const playerAccountsTable = pgTable("player_accounts", {
  playerId: text("player_id").primaryKey(),
  playerName: text("player_name"),
  balance: real("balance").default(0).notNull(),
});

export const insertPlayerAccountSchema = createInsertSchema(playerAccountsTable);
export const selectPlayerAccountSchema = createSelectSchema(playerAccountsTable);
export type InsertPlayerAccount = z.infer<typeof insertPlayerAccountSchema>;
export type PlayerAccount = typeof playerAccountsTable.$inferSelect;
