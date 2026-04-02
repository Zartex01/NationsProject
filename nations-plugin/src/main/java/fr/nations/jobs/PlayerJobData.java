package fr.nations.jobs;

public class PlayerJobData {

    private final JobType jobType;
    private int level;
    private int xp;
    private long totalXpEarned;
    private long coinsEarned;

    public static final int MAX_LEVEL = 50;

    public PlayerJobData(JobType jobType, int level, int xp, long totalXpEarned, long coinsEarned) {
        this.jobType = jobType;
        this.level = level;
        this.xp = xp;
        this.totalXpEarned = totalXpEarned;
        this.coinsEarned = coinsEarned;
    }

    public static PlayerJobData empty(JobType type) {
        return new PlayerJobData(type, 1, 0, 0, 0);
    }

    // ─── Calculs XP/Niveau ────────────────────────────────────────────────────

    /** XP nécessaire pour passer du niveau actuel au suivant */
    public static int xpToNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return Integer.MAX_VALUE;
        return currentLevel * 100;
    }

    /** Ajoute de l'XP et gère les montées de niveau. Retourne le nombre de niveaux gagnés. */
    public int addXp(int amount) {
        if (level >= MAX_LEVEL) return 0;
        totalXpEarned += amount;
        xp += amount;
        int levelsGained = 0;
        while (level < MAX_LEVEL && xp >= xpToNextLevel(level)) {
            xp -= xpToNextLevel(level);
            level++;
            levelsGained++;
        }
        if (level >= MAX_LEVEL) xp = 0;
        return levelsGained;
    }

    /** Récompense en coins pour atteindre un niveau */
    public static int coinsRewardForLevel(int level) {
        int base = level * 100;
        if (level == 50) return base + 50_000;
        if (level == 25) return base + 10_000;
        if (level == 10) return base + 2_000;
        if (level == 5)  return base + 500;
        return base;
    }

    /** Pourcentage de progression vers le prochain niveau (0.0 - 1.0) */
    public double progressPercent() {
        if (level >= MAX_LEVEL) return 1.0;
        int needed = xpToNextLevel(level);
        return needed <= 0 ? 1.0 : Math.min(1.0, (double) xp / needed);
    }

    /** Barre de progression ASCII (10 blocs) */
    public String progressBar() {
        if (level >= MAX_LEVEL) return "§a§l██████████ §fMAX";
        double pct = progressPercent();
        int filled = (int) Math.round(pct * 10);
        String bar = "§a" + "█".repeat(filled) + "§8" + "█".repeat(10 - filled);
        return bar + " §f" + (int)(pct * 100) + "%";
    }

    // ─── Getters/Setters ──────────────────────────────────────────────────────

    public JobType getJobType()      { return jobType; }
    public int getLevel()            { return level; }
    public int getXp()               { return xp; }
    public long getTotalXpEarned()   { return totalXpEarned; }
    public long getCoinsEarned()     { return coinsEarned; }
    public void addCoinsEarned(long coins) { this.coinsEarned += coins; }
    public void setLevel(int level)  { this.level = level; }
    public void setXp(int xp)        { this.xp = xp; }

    public boolean isMaxLevel() { return level >= MAX_LEVEL; }
    public int xpNeeded()       { return isMaxLevel() ? 0 : xpToNextLevel(level) - xp; }
}
