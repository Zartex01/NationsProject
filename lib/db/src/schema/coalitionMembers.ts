import { pgTable, text, primaryKey } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";
import { coalitionsTable } from "./coalitions";

export const coalitionMembersTable = pgTable("coalition_members", {
  coalitionId: text("coalition_id").notNull().references(() => coalitionsTable.id, { onDelete: "cascade" }),
  nationId: text("nation_id").notNull(),
}, (table) => [
  primaryKey({ columns: [table.coalitionId, table.nationId] }),
]);

export const insertCoalitionMemberSchema = createInsertSchema(coalitionMembersTable);
export const selectCoalitionMemberSchema = createSelectSchema(coalitionMembersTable);
export type InsertCoalitionMember = z.infer<typeof insertCoalitionMemberSchema>;
export type CoalitionMember = typeof coalitionMembersTable.$inferSelect;
