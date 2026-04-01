import { pgTable, text, integer, real } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";

export const playerGradesTable = pgTable("player_grades", {
  playerId: text("player_id").primaryKey(),
  playerName: text("player_name"),
  grade: text("grade").default("JOUEUR").notNull(),
  level: integer("level").default(1).notNull(),
  xp: real("xp").default(0).notNull(),
});

export const insertPlayerGradeSchema = createInsertSchema(playerGradesTable);
export const selectPlayerGradeSchema = createSelectSchema(playerGradesTable);
export type InsertPlayerGrade = z.infer<typeof insertPlayerGradeSchema>;
export type PlayerGrade = typeof playerGradesTable.$inferSelect;
