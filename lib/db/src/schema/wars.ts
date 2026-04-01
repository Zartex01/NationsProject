import { pgTable, text, integer, boolean } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";

export const warsTable = pgTable("wars", {
  id: text("id").primaryKey(),
  attackerNationId: text("attacker_nation_id").notNull(),
  defenderNationId: text("defender_nation_id").notNull(),
  type: text("type").notNull(),
  status: text("status").notNull(),
  declaredAt: integer("declared_at").notNull(),
  endsAt: integer("ends_at").notNull(),
  reason: text("reason"),
  attackerKills: integer("attacker_kills").default(0).notNull(),
  defenderKills: integer("defender_kills").default(0).notNull(),
  staffNote: text("staff_note"),
  validatedBy: text("validated_by"),
  surrenderRequested: boolean("surrender_requested").default(false).notNull(),
  surrenderRequestedAt: integer("surrender_requested_at"),
});

export const insertWarSchema = createInsertSchema(warsTable);
export const selectWarSchema = createSelectSchema(warsTable);
export type InsertWar = z.infer<typeof insertWarSchema>;
export type War = typeof warsTable.$inferSelect;
