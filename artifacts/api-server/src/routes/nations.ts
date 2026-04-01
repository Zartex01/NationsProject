import { Router } from "express";
import { db } from "@workspace/db";
import {
  nationsTable,
  nationMembersTable,
  nationAlliesTable,
  claimedChunksTable,
  warsTable,
} from "@workspace/db";
import { eq, or, desc, asc, sql, count } from "drizzle-orm";

const router = Router();

router.get("/nations", async (req, res) => {
  const limit = Number(req.query["limit"] ?? 50);
  const offset = Number(req.query["offset"] ?? 0);
  const sortBy = (req.query["sortBy"] as string) ?? "level";
  const order = (req.query["order"] as string) ?? "desc";

  const sortColumns: Record<string, any> = {
    name: nationsTable.name,
    level: nationsTable.level,
    seasonPoints: nationsTable.seasonPoints,
    bankBalance: nationsTable.bankBalance,
  };

  const memberCountSq = db
    .select({
      nationId: nationMembersTable.nationId,
      memberCount: count(nationMembersTable.playerId).as("memberCount"),
    })
    .from(nationMembersTable)
    .groupBy(nationMembersTable.nationId)
    .as("memberCountSq");

  const orderFn = order === "asc" ? asc : desc;

  let query = db
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
    .leftJoin(memberCountSq, eq(nationsTable.id, memberCountSq.nationId));

  const sorted =
    sortBy === "memberCount"
      ? query.orderBy(orderFn(sql`coalesce(${memberCountSq.memberCount}, 0)`))
      : query.orderBy(orderFn(sortColumns[sortBy] ?? nationsTable.level));

  const totalResult = await db.select({ count: count() }).from(nationsTable);
  const data = await sorted.limit(limit).offset(offset);

  res.json({ data, total: Number(totalResult[0]?.count ?? 0) });
});

router.get("/nations/:id", async (req, res) => {
  const { id } = req.params;

  const nation = await db.query.nationsTable.findFirst({
    where: eq(nationsTable.id, id),
  });

  if (!nation) {
    return res.status(404).json({ error: "Nation not found" });
  }

  const members = await db
    .select()
    .from(nationMembersTable)
    .where(eq(nationMembersTable.nationId, id));

  const allies = await db
    .select()
    .from(nationAlliesTable)
    .where(or(eq(nationAlliesTable.nationA, id), eq(nationAlliesTable.nationB, id)));

  const allyIds = allies.map((a) => (a.nationA === id ? a.nationB : a.nationA));

  res.json({ ...nation, members, allies: allyIds });
});

router.get("/nations/:id/members", async (req, res) => {
  const { id } = req.params;

  const nation = await db.query.nationsTable.findFirst({
    where: eq(nationsTable.id, id),
  });

  if (!nation) {
    return res.status(404).json({ error: "Nation not found" });
  }

  const members = await db
    .select()
    .from(nationMembersTable)
    .where(eq(nationMembersTable.nationId, id));

  res.json(members);
});

router.get("/nations/:id/claims", async (req, res) => {
  const { id } = req.params;

  const nation = await db.query.nationsTable.findFirst({
    where: eq(nationsTable.id, id),
  });

  if (!nation) {
    return res.status(404).json({ error: "Nation not found" });
  }

  const claims = await db
    .select()
    .from(claimedChunksTable)
    .where(eq(claimedChunksTable.nationId, id));

  res.json(claims);
});

router.get("/nations/:id/wars", async (req, res) => {
  const { id } = req.params;
  const status = req.query["status"] as string | undefined;

  const nation = await db.query.nationsTable.findFirst({
    where: eq(nationsTable.id, id),
  });

  if (!nation) {
    return res.status(404).json({ error: "Nation not found" });
  }

  let wars = await db
    .select()
    .from(warsTable)
    .where(or(eq(warsTable.attackerNationId, id), eq(warsTable.defenderNationId, id)));

  if (status) {
    wars = wars.filter((w) => w.status === status);
  }

  res.json(wars);
});

export default router;
