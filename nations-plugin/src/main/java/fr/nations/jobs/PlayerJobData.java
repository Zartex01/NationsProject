package fr.nations.jobs;

public class PlayerJobData {

    private final JobType jobType;
    private int  level;
    private int  xp;
    private long totalXpEarned;
    private long coinsEarned;
    private boolean active;
    private long actionsCount; // blocs cassés / mobs tués / récoltes / bûches coupées

    public static final int MAX_LEVEL      = 50;
    public static final int MAX_ACTIVE_JOBS = 2;

    public PlayerJobData(JobType jobType, int level, int xp,
                         long totalXpEarned, long coinsEarned,
                         boolean active, long actionsCount) {
        this.jobType       = jobType;
        this.level         = level;
        this.xp            = xp;
        this.totalXpEarned = totalXpEarned;
        this.coinsEarned   = coinsEarned;
        this.active        = active;
        this.actionsCount  = actionsCount;
    }

    public static PlayerJobData empty(JobType type) {
        return new PlayerJobData(type, 1, 0, 0, 0, false, 0);
    }

    // ─── Système de Tiers / Prestige ─────────────────────────────────────────

    public static String getTierName(int level) {
        if (level >= 50) return "§5§lGrand Maître";
        if (level >= 40) return "§6§lMaître";
        if (level >= 30) return "§b§lExpert";
        if (level >= 20) return "§aCompagnon";
        if (level >= 10) return "§eJournalier";
        return "§7Apprenti";
    }

    public static String getTierStars(int level) {
        if (level >= 50) return "§5✦✦✦✦✦";
        if (level >= 40) return "§6✦✦✦✦§8✦";
        if (level >= 30) return "§b✦✦✦§8✦✦";
        if (level >= 20) return "§a✦✦§8✦✦✦";
        if (level >= 10) return "§e✦§8✦✦✦✦";
        return "§8✦✦✦✦✦";
    }

    public static String getTierColor(int level) {
        if (level >= 50) return "§5";
        if (level >= 40) return "§6";
        if (level >= 30) return "§b";
        if (level >= 20) return "§a";
        if (level >= 10) return "§e";
        return "§7";
    }

    /** Multiplicateur XP en fonction du tier (récompense la progression) */
    public static double getXpMultiplier(int level) {
        if (level >= 50) return 2.00;
        if (level >= 40) return 1.75;
        if (level >= 30) return 1.50;
        if (level >= 20) return 1.25;
        if (level >= 10) return 1.10;
        return 1.00;
    }

    // ─── Calculs XP / Niveau ─────────────────────────────────────────────────

    /** XP requis pour passer du niveau actuel au suivant */
    public static int xpToNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return Integer.MAX_VALUE;
        return 80 + currentLevel * 120;
    }

    /**
     * Ajoute de l'XP (avec multiplicateur déjà appliqué) et gère les montées de niveau.
     * @return nombre de niveaux gagnés
     */
    public int addXp(int amount) {
        if (level >= MAX_LEVEL) return 0;
        totalXpEarned += amount;
        xp += amount;
        actionsCount++;
        int levelsGained = 0;
        while (level < MAX_LEVEL && xp >= xpToNextLevel(level)) {
            xp -= xpToNextLevel(level);
            level++;
            levelsGained++;
        }
        if (level >= MAX_LEVEL) xp = 0;
        return levelsGained;
    }

    /** Coins offerts lors de l'atteinte d'un niveau (avec bonus aux paliers) */
    public static int coinsRewardForLevel(int level) {
        int base = level * 150;
        if (level == 50) return base + 75_000;
        if (level == 40) return base + 25_000;
        if (level == 30) return base + 10_000;
        if (level == 25) return base +  5_000;
        if (level == 20) return base +  3_000;
        if (level == 15) return base +  1_500;
        if (level == 10) return base +  1_000;
        if (level ==  5) return base +    300;
        return base;
    }

    /** Vrai si ce niveau est un palier de prestige */
    public static boolean isMilestone(int level) {
        return level == 5 || level == 10 || level == 15 || level == 20
            || level == 25 || level == 30 || level == 40 || level == 50;
    }

    /** Pourcentage de progression (0.0 – 1.0) */
    public double progressPercent() {
        if (level >= MAX_LEVEL) return 1.0;
        int needed = xpToNextLevel(level);
        return needed <= 0 ? 1.0 : Math.min(1.0, (double) xp / needed);
    }

    /**
     * Barre de progression visuelle.
     * @param width     nombre de segments
     * @param jobColor  code couleur §X du métier (ex. "§8")
     */
    public String progressBar(int width, String jobColor) {
        if (isMaxLevel()) return jobColor + "█".repeat(width) + " §f§lMAX";
        int filledCount = (int) Math.round(progressPercent() * width);
        return jobColor + "█".repeat(filledCount) + "§8" + "█".repeat(width - filledCount);
    }

    public String progressBar() { return progressBar(12, "§a"); }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public JobType  getJobType()       { return jobType; }
    public int      getLevel()         { return level; }
    public int      getXp()            { return xp; }
    public long     getTotalXpEarned() { return totalXpEarned; }
    public long     getCoinsEarned()   { return coinsEarned; }
    public boolean  isActive()         { return active; }
    public long     getActionsCount()  { return actionsCount; }

    public void setLevel(int level)         { this.level  = level; }
    public void setXp(int xp)              { this.xp     = xp; }
    public void setActive(boolean active)   { this.active = active; }
    public void addCoinsEarned(long coins)  { this.coinsEarned += coins; }

    public boolean isMaxLevel()  { return level >= MAX_LEVEL; }
    public int     xpNeeded()    { return isMaxLevel() ? 0 : xpToNextLevel(level) - xp; }

    public String getTierName()    { return getTierName(level); }
    public String getTierStars()   { return getTierStars(level); }
    public String getTierColor()   { return getTierColor(level); }
    public double getXpMultiplier(){ return getXpMultiplier(level); }
}
