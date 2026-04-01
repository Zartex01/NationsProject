import { Router } from "express";
import { db } from "@workspace/db";
import { coalitionsTable, coalitionMembersTable } from "@workspace/db";
import { eq } from "drizzle-orm";

const router = Router();

router.get("/coalitions", async (req, res) => {
  const coalitions = await db.select().from(coalitionsTable);

  const result = await Promise.all(
    coalitions.map(async (coalition) => {
      const members = await db
        .select()
        .from(coalitionMembersTable)
        .where(eq(coalitionMembersTable.coalitionId, coalition.id));

      return {
        ...coalition,
        memberNationIds: members.map((m) => m.nationId),
      };
    }),
  );

  res.json(result);
});

router.get("/coalitions/:id", async (req, res) => {
  const { id } = req.params;

  const coalition = await db.query.coalitionsTable.findFirst({
    where: eq(coalitionsTable.id, id),
  });

  if (!coalition) {
    return res.status(404).json({ error: "Coalition not found" });
  }

  const members = await db
    .select()
    .from(coalitionMembersTable)
    .where(eq(coalitionMembersTable.coalitionId, id));

  res.json({
    ...coalition,
    memberNationIds: members.map((m) => m.nationId),
  });
});

export default router;
