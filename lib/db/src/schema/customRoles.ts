import { pgTable, text, integer, boolean, unique } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";
import { nationsTable } from "./nations";

export const customRolesTable = pgTable("custom_roles", {
  id: text("id").primaryKey(),
  nationId: text("nation_id").notNull().references(() => nationsTable.id, { onDelete: "cascade" }),
  name: text("name").notNull(),
  displayName: text("display_name").notNull(),
  rank: integer("rank").default(50).notNull(),
  permBuild: boolean("perm_build").default(false).notNull(),
  permInvite: boolean("perm_invite").default(false).notNull(),
  permKick: boolean("perm_kick").default(false).notNull(),
  permManageWar: boolean("perm_manage_war").default(false).notNull(),
  permManageBank: boolean("perm_manage_bank").default(false).notNull(),
  permManageClaims: boolean("perm_manage_claims").default(false).notNull(),
  permManageAllies: boolean("perm_manage_allies").default(false).notNull(),
  permManageRoles: boolean("perm_manage_roles").default(false).notNull(),
  permRename: boolean("perm_rename").default(false).notNull(),
  permDisband: boolean("perm_disband").default(false).notNull(),
}, (table) => [
  unique().on(table.nationId, table.name),
]);

export const insertCustomRoleSchema = createInsertSchema(customRolesTable);
export const selectCustomRoleSchema = createSelectSchema(customRolesTable);
export type InsertCustomRole = z.infer<typeof insertCustomRoleSchema>;
export type CustomRole = typeof customRolesTable.$inferSelect;
