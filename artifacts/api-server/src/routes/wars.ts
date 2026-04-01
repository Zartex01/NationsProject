import { Router } from "express";
import { db } from "@workspace/db";
import { warsTable } from "@workspace/db";
import { eq, desc, count } from "drizzle-orm";

const router = Router();

router.get("/wars", async (req, res) => {
  const limit = Number(req.query["limit"] ?? 50);
  const offset = Number(req.query["offset"] ?? 0);
  const status = req.query["status"] as string | undefined;

  let baseQuery = db.select().from(warsTable).orderBy(desc(warsTable.declaredAt));

  let wars = await baseQuery.limit(limit).offset(offset);

  if (status) {
    const all = await db.select().from(warsTable);
    const filtered = all.filter((w) => w.status === status);
    const paged = filtered.slice(offset, offset + limit);
    return res.json({ data: paged, total: filtered.length });
  }

  const totalResult = await db.select({ count: count() }).from(warsTable);

  res.json({ data: wars, total: Number(totalResult[0]?.count ?? 0) });
});

router.get("/wars/:id", async (req, res) => {
  const { id } = req.params;

  const war = await db.query.warsTable.findFirst({
    where: eq(warsTable.id, id),
  });

  if (!war) {
    return res.status(404).json({ error: "War not found" });
  }

  res.json(war);
});

export default router;
