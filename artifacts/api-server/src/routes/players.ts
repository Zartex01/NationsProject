import { Router } from "express";
import { db } from "@workspace/db";
import {
  playerAccountsTable,
  playerGradesTable,
  playerPlaytimeTable,
  seasonStatsTable,
} from "@workspace/db";
import { eq } from "drizzle-orm";

const router = Router();

router.get("/players/:id", async (req, res) => {
  const { id } = req.params;

  const [account, grade, playtime] = await Promise.all([
    db.query.playerAccountsTable.findFirst({ where: eq(playerAccountsTable.playerId, id) }),
    db.query.playerGradesTable.findFirst({ where: eq(playerGradesTable.playerId, id) }),
    db.query.playerPlaytimeTable.findFirst({ where: eq(playerPlaytimeTable.playerId, id) }),
  ]);

  if (!account && !grade && !playtime) {
    return res.status(404).json({ error: "Player not found" });
  }

  const playerName = account?.playerName ?? grade?.playerName ?? null;

  res.json({
    playerId: id,
    playerName,
    balance: account?.balance ?? null,
    grade: grade?.grade ?? null,
    gradeLevel: grade?.level ?? null,
    gradeXp: grade?.xp ?? null,
    totalPlaytimeSeconds: playtime?.totalSeconds ?? null,
    claimedPlaytimeSeconds: playtime?.claimedSeconds ?? null,
  });
});

router.get("/players/:id/stats", async (req, res) => {
  const { id } = req.params;

  const stats = await db
    .select()
    .from(seasonStatsTable)
    .where(eq(seasonStatsTable.playerId, id));

  res.json(stats);
});

export default router;
