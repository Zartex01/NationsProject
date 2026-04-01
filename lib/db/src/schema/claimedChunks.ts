import { pgTable, text, integer, unique } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";
import { z } from "zod";
import { nationsTable } from "./nations";

export const claimedChunksTable = pgTable("claimed_chunks", {
  id: text("id").primaryKey(),
  nationId: text("nation_id").notNull().references(() => nationsTable.id, { onDelete: "cascade" }),
  worldName: text("world_name").notNull(),
  chunkX: integer("chunk_x").notNull(),
  chunkZ: integer("chunk_z").notNull(),
  claimedAt: integer("claimed_at").notNull(),
}, (table) => [
  unique().on(table.worldName, table.chunkX, table.chunkZ),
]);

export const insertClaimedChunkSchema = createInsertSchema(claimedChunksTable);
export const selectClaimedChunkSchema = createSelectSchema(claimedChunksTable);
export type InsertClaimedChunk = z.infer<typeof insertClaimedChunkSchema>;
export type ClaimedChunk = typeof claimedChunksTable.$inferSelect;
