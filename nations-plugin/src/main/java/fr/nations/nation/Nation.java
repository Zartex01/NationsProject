package fr.nations.nation;

import java.util.*;

public class Nation {

    private final UUID id;
    private String name;
    private UUID leaderId;
    private String description;
    private final long createdAt;
    private double bankBalance;
    private int seasonPoints;
    private boolean open;
    private UUID coalitionId;

    private final Map<UUID, NationMember> members;
    private final Set<UUID> allies;
    private final Set<UUID> pendingInvites;
    private final Set<UUID> allyRequests;

    public Nation(UUID id, String name, UUID leaderId, long createdAt) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.createdAt = createdAt;
        this.description = "";
        this.bankBalance = 0;
        this.seasonPoints = 0;
        this.open = false;
        this.members = new HashMap<>();
        this.allies = new HashSet<>();
        this.pendingInvites = new HashSet<>();
        this.allyRequests = new HashSet<>();
    }

    public void addMember(NationMember member) {
        members.put(member.getPlayerId(), member);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public NationMember getMember(UUID playerId) {
        return members.get(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }

    public void addAlly(UUID nationId) {
        allies.add(nationId);
    }

    public void removeAlly(UUID nationId) {
        allies.remove(nationId);
    }

    public boolean isAlly(UUID nationId) {
        return allies.contains(nationId);
    }

    public void addPendingInvite(UUID playerId) {
        pendingInvites.add(playerId);
    }

    public void removePendingInvite(UUID playerId) {
        pendingInvites.remove(playerId);
    }

    public boolean hasPendingInvite(UUID playerId) {
        return pendingInvites.contains(playerId);
    }

    public void addAllyRequest(UUID nationId) {
        allyRequests.add(nationId);
    }

    public void removeAllyRequest(UUID nationId) {
        allyRequests.remove(nationId);
    }

    public boolean hasAllyRequest(UUID nationId) {
        return allyRequests.contains(nationId);
    }

    public void addSeasonPoints(int points) {
        this.seasonPoints += points;
    }

    public void resetSeasonPoints() {
        this.seasonPoints = 0;
    }

    public int getMemberCount() {
        return members.size();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getCreatedAt() { return createdAt; }
    public double getBankBalance() { return bankBalance; }
    public void setBankBalance(double balance) { this.bankBalance = Math.max(0, balance); }
    public void depositToBank(double amount) { this.bankBalance += amount; }
    public boolean withdrawFromBank(double amount) {
        if (bankBalance < amount) return false;
        bankBalance -= amount;
        return true;
    }
    public int getSeasonPoints() { return seasonPoints; }
    public void setSeasonPoints(int points) { this.seasonPoints = points; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public UUID getCoalitionId() { return coalitionId; }
    public void setCoalitionId(UUID coalitionId) { this.coalitionId = coalitionId; }
    public Map<UUID, NationMember> getMembers() { return Collections.unmodifiableMap(members); }
    public Set<UUID> getAllies() { return Collections.unmodifiableSet(allies); }
    public Set<UUID> getPendingInvites() { return Collections.unmodifiableSet(pendingInvites); }
    public Set<UUID> getAllyRequests() { return Collections.unmodifiableSet(allyRequests); }
}
