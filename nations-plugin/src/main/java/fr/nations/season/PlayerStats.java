package fr.nations.season;

import java.util.UUID;

public class PlayerStats {

    private final UUID playerId;
    private String playerName;
    private int kills;
    private int deaths;
    private int warsWon;
    private int claimsCount;

    public PlayerStats(UUID playerId, int kills, int deaths, int warsWon, int claimsCount) {
        this.playerId = playerId;
        this.kills = kills;
        this.deaths = deaths;
        this.warsWon = warsWon;
        this.claimsCount = claimsCount;
    }

    public double getKillDeathRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public void addKill() { this.kills++; }
    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void addDeath() { this.deaths++; }
    public int getWarsWon() { return warsWon; }
    public void setWarsWon(int warsWon) { this.warsWon = warsWon; }
    public void addWarWin() { this.warsWon++; }
    public int getClaimsCount() { return claimsCount; }
    public void setClaimsCount(int count) { this.claimsCount = count; }
    public void addClaim() { this.claimsCount++; }
}
