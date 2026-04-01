import { pgTable, text, integer, primaryKey } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";
import { nationsTable } from "./nations";

export const nationMembersTable = pgTable("nation_members", {
  playerId: text("player_id").primaryKey(),
  nationId: text("nation_id").notNull().references(() => nationsTable.id, { onDelete: "cascade" }),
  roleName: text("role_name").default("MEMBER").notNull(),
  playerName: text("player_name"),
  joinedAt: integer("joined_at").notNull(),
});

export const insertNationMemberSchema = createInsertSchema(nationMembersTable);
export const selectNationMemberSchema = createSelectSchema(nationMembersTable);
export type InsertNationMember = z.infer<typeof insertNationMemberSchema>;
export type NationMember = typeof nationMembersTable.$inferSelect;
