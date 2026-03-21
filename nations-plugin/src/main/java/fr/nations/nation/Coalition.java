package fr.nations.nation;

import java.util.*;

public class Coalition {

    private final UUID id;
    private String name;
    private UUID leaderNationId;
    private final Set<UUID> memberNations;
    private final long createdAt;
    private String description;

    public Coalition(UUID id, String name, UUID leaderNationId) {
        this.id = id;
        this.name = name;
        this.leaderNationId = leaderNationId;
        this.memberNations = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
        this.description = "";
        this.memberNations.add(leaderNationId);
    }

    public void addNation(UUID nationId) {
        memberNations.add(nationId);
    }

    public void removeNation(UUID nationId) {
        memberNations.remove(nationId);
    }

    public boolean contains(UUID nationId) {
        return memberNations.contains(nationId);
    }

    public int getNationCount() {
        return memberNations.size();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getLeaderNationId() { return leaderNationId; }
    public void setLeaderNationId(UUID leaderNationId) { this.leaderNationId = leaderNationId; }
    public Set<UUID> getMemberNations() { return Collections.unmodifiableSet(memberNations); }
    public long getCreatedAt() { return createdAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
