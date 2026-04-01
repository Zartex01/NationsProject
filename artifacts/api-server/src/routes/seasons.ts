import { Router } from "express";
import { db } from "@workspace/db";
import { seasonsTable, seasonStatsTable } from "@workspace/db";
import { eq, desc, asc, count } from "drizzle-orm";

const router = Router();

router.get("/seasons", async (req, res) => {
  const seasons = await db
    .select()
    .from(seasonsTable)
    .orderBy(asc(seasonsTable.seasonNumber));

  res.json(seasons);
});

router.get("/seasons/current", async (req, res) => {
  const current = await db.query.seasonsTable.findFirst({
    where: eq(seasonsTable.current, true),
  });

  if (!current) {
    return res.status(404).json({ error: "No active season" });
  }

  res.json(current);
});

router.get("/seasons/:number/stats", async (req, res) => {
  const seasonNumber = Number(req.params["number"]);
  const limit = Number(req.query["limit"] ?? 50);
  const offset = Number(req.query["offset"] ?? 0);

  const all = await db
    .select()
    .from(seasonStatsTable)
    .where(eq(seasonStatsTable.seasonNumber, seasonNumber))
    .orderBy(desc(seasonStatsTable.kills));

  const total = all.length;
  const data = all.slice(offset, offset + limit);

  res.json({ data, total });
});

export default router;
