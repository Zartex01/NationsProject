package fr.nations.territory;

import java.util.UUID;

public class ClaimedChunk {

    private final UUID id;
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final UUID nationId;
    private final UUID claimedBy;
    private final long claimedAt;

    public ClaimedChunk(UUID id, String worldName, int chunkX, int chunkZ, UUID nationId, UUID claimedBy, long claimedAt) {
        this.id = id;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.nationId = nationId;
        this.claimedBy = claimedBy;
        this.claimedAt = claimedAt;
    }

    public ClaimedChunk(String worldName, int chunkX, int chunkZ, UUID nationId, UUID claimedBy, long claimedAt) {
        this(UUID.randomUUID(), worldName, chunkX, chunkZ, nationId, claimedBy, claimedAt);
    }

    public String getKey() {
        return worldName + "_" + chunkX + "_" + chunkZ;
    }

    public UUID getId() { return id; }
    public String getWorldName() { return worldName; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public UUID getNationId() { return nationId; }
    public UUID getClaimedBy() { return claimedBy; }
    public long getClaimedAt() { return claimedAt; }
}
