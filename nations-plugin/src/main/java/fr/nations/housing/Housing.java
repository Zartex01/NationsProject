package fr.nations.housing;

import java.util.UUID;

public class Housing {

    private final UUID id;
    private final UUID nationId;
    private String name;
    private double price;
    private UUID ownerId;
    private String ownerName;

    // Cuboid de la chambre
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    private final String world;

    // Panneau de vente
    private int signX, signY, signZ;
    private String signWorld;
    private boolean signPlaced;

    public Housing(UUID id, UUID nationId, String name, double price,
                   int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String world) {
        this.id = id;
        this.nationId = nationId;
        this.name = name;
        this.price = price;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.world = world;
        this.signPlaced = false;
    }

    public boolean contains(String world, int x, int y, int z) {
        return this.world.equals(world)
            && x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    public boolean isSignAt(String world, int x, int y, int z) {
        return signPlaced && this.signWorld.equals(world)
            && this.signX == x && this.signY == y && this.signZ == z;
    }

    public boolean isOwned() { return ownerId != null; }

    public void setSign(String world, int x, int y, int z) {
        this.signWorld = world;
        this.signX = x;
        this.signY = y;
        this.signZ = z;
        this.signPlaced = true;
    }

    public UUID getId() { return id; }
    public UUID getNationId() { return nationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
    public String getWorld() { return world; }
    public int getSignX() { return signX; }
    public int getSignY() { return signY; }
    public int getSignZ() { return signZ; }
    public String getSignWorld() { return signWorld; }
    public boolean isSignPlaced() { return signPlaced; }

    public int getWidth()  { return maxX - minX + 1; }
    public int getHeight() { return maxY - minY + 1; }
    public int getDepth()  { return maxZ - minZ + 1; }
    public int getVolume() { return getWidth() * getHeight() * getDepth(); }
}
