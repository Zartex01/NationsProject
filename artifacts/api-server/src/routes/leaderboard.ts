import { Router } from "express";
import { db } from "@workspace/db";
import { nationsTable, nationMembersTable, seasonStatsTable, seasonsTable } from "@workspace/db";
import { eq, desc, sql, count } from "drizzle-orm";

const router = Router();

router.get("/leaderboard/nations", async (req, res) => {
  const limit = Number(req.query["limit"] ?? 10);

  const memberCountSq = db
    .select({
      nationId: nationMembersTable.nationId,
      memberCount: count(nationMembersTable.playerId).as("memberCount"),
    })
    .from(nationMembersTable)
    .groupBy(nationMembersTable.nationId)
    .as("memberCountSq");

  const nations = await db
    .select({
      id: nationsTable.id,
      name: nationsTable.name,
      description: nationsTable.description,
      leaderId: nationsTable.leaderId,
      level: nationsTable.level,
      xp: nationsTable.xp,
      seasonPoints: nationsTable.seasonPoints,
      bankBalance: nationsTable.bankBalance,
      open: nationsTable.open,
      coalitionId: nationsTable.coalitionId,
      createdAt: nationsTable.createdAt,
      memberCount: sql<number>`coalesce(${memberCountSq.memberCount}, 0)`.as("memberCount"),
    })
    .from(nationsTable)
    .leftJoin(memberCountSq, eq(nationsTable.id, memberCountSq.nationId))
    .orderBy(desc(nationsTable.seasonPoints))
    .limit(limit);

  res.json(nations);
});

router.get("/leaderboard/players", async (req, res) => {
  const limit = Number(req.query["limit"] ?? 10);

  const currentSeason = await db.query.seasonsTable.findFirst({
    where: eq(seasonsTable.current, true),
  });

  if (!currentSeason) {
    return res.json([]);
  }

  const stats = await db
    .select()
    .from(seasonStatsTable)
    .where(eq(seasonStatsTable.seasonNumber, currentSeason.seasonNumber))
    .orderBy(desc(seasonStatsTable.kills))
    .limit(limit);

  res.json(stats);
});

export default router;
