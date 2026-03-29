package fr.nations.epoque;

import java.util.UUID;

public class EpoqueNationProgress {

    private final UUID nationId;
    private int currentLevel;
    private long researchEndTime;

    public EpoqueNationProgress(UUID nationId, int currentLevel, long researchEndTime) {
        this.nationId        = nationId;
        this.currentLevel    = currentLevel;
        this.researchEndTime = researchEndTime;
    }

    public boolean isResearching() {
        return researchEndTime > 0;
    }

    public boolean isResearchComplete() {
        return researchEndTime > 0 && System.currentTimeMillis() >= researchEndTime;
    }

    public long getTimeRemainingMillis() {
        if (researchEndTime <= 0) return 0;
        long remaining = researchEndTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public String formatTimeRemaining() {
        long ms = getTimeRemainingMillis();
        long secs  = ms / 1_000;
        long mins  = secs / 60;
        long hours = mins / 60;

        if (hours > 0)   return hours + "h " + (mins % 60) + "min";
        if (mins > 0)    return mins + "min " + (secs % 60) + "s";
        return secs + "s";
    }

    public UUID getNationId()        { return nationId; }
    public int getCurrentLevel()     { return currentLevel; }
    public long getResearchEndTime() { return researchEndTime; }

    public void setCurrentLevel(int level)       { this.currentLevel = level; }
    public void setResearchEndTime(long endTime) { this.researchEndTime = endTime; }
}
