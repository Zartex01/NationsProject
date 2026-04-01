package fr.nations.grade;

import java.util.UUID;

public class PlayerGrade {

    private final UUID playerId;
    private String playerName;
    private String gradeName;
    private int level;
    private long xp;
    private int claimCount;

    public PlayerGrade(UUID playerId, String gradeName, int level, long xp, int claimCount) {
        this.playerId = playerId;
        this.gradeName = gradeName;
        this.level = level;
        this.xp = xp;
        this.claimCount = claimCount;
    }

    public long getXpForNextLevel() {
        return (long) level * level * 100L;
    }

    public boolean canLevelUp() {
        return xp >= getXpForNextLevel();
    }

    public void incrementClaimCount() { this.claimCount++; }
    public void decrementClaimCount() { this.claimCount = Math.max(0, claimCount - 1); }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getGradeName() { return gradeName; }
    public void setGradeName(String gradeName) { this.gradeName = gradeName; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = xp; }
    public void addXp(long amount) { this.xp += amount; }
    public int getClaimCount() { return claimCount; }
    public void setClaimCount(int count) { this.claimCount = count; }
}
