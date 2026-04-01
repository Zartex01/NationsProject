import { Router, type IRouter } from "express";
import healthRouter from "./health";
import nationsRouter from "./nations";
import playersRouter from "./players";
import warsRouter from "./wars";
import coalitionsRouter from "./coalitions";
import seasonsRouter from "./seasons";
import leaderboardRouter from "./leaderboard";

const router: IRouter = Router();

router.use(healthRouter);
router.use(nationsRouter);
router.use(playersRouter);
router.use(warsRouter);
router.use(coalitionsRouter);
router.use(seasonsRouter);
router.use(leaderboardRouter);

export default router;
