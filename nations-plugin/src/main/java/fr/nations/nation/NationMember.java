package fr.nations.nation;

import java.util.UUID;

public class NationMember {

    private final UUID playerId;
    private String playerName;
    private NationRole role;
    private long joinedAt;

    public NationMember(UUID playerId, String playerName, NationRole role) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.role = role;
        this.joinedAt = System.currentTimeMillis();
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String name) { this.playerName = name; }
    public NationRole getRole() { return role; }
    public void setRole(NationRole role) { this.role = role; }
    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    public boolean canBuild() { return role.canBuild(); }
    public boolean canInvite() { return role.canInvite(); }
    public boolean canKick() { return role.canKick(); }
    public boolean canManageWar() { return role.canManageWar(); }
    public boolean canManageBank() { return role.canManageBank(); }
}
