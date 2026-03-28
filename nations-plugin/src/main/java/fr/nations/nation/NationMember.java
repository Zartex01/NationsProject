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

    public boolean hasPerm(String permKey) { return role.hasPerm(permKey); }

    public boolean canBuild()          { return role.canBuild(); }
    public boolean canInvite()         { return role.canInvite(); }
    public boolean canKick()           { return role.canKick(); }
    public boolean canManageWar()      { return role.canManageWar(); }
    public boolean canManageBank()     { return hasPerm("can-manage-bank"); }
    public boolean canDissolve()       { return role.canDissolve(); }
    public boolean canPromote()        { return hasPerm("can-promote"); }
    public boolean canDemote()         { return hasPerm("can-demote"); }
    public boolean canClaim()          { return hasPerm("can-claim"); }
    public boolean canUnclaim()        { return hasPerm("can-unclaim"); }
    public boolean canDepositBank()    { return true; }
    public boolean canManageAllies()   { return hasPerm("can-manage-allies"); }
    public boolean canRename()         { return hasPerm("can-rename"); }
    public boolean canSetDescription() { return hasPerm("can-set-description"); }
    public boolean canSetHome()        { return hasPerm("can-set-home"); }
    public boolean canTeleportHome()   { return hasPerm("can-teleport-home"); }
    public boolean canOpenNation()     { return hasPerm("can-open-nation"); }
    public boolean canAccessStorage()  { return hasPerm("can-access-storage"); }
    public boolean canManageFlags()    { return hasPerm("can-manage-flags"); }
    public boolean canUseAtm()         { return hasPerm("can-use-atm"); }
}
