package fr.nations.role;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class CustomRole {

    private final UUID id;
    private final UUID nationId;
    private String name;
    private String displayName;
    private int rank;
    private final Map<RolePermission, Boolean> permissions;

    public CustomRole(UUID id, UUID nationId, String name, String displayName, int rank) {
        this.id = id;
        this.nationId = nationId;
        this.name = name;
        this.displayName = displayName;
        this.rank = rank;
        this.permissions = new EnumMap<>(RolePermission.class);
        for (RolePermission perm : RolePermission.values()) {
            permissions.put(perm, false);
        }
    }

    public boolean hasPermission(RolePermission perm) {
        return permissions.getOrDefault(perm, false);
    }

    public void setPermission(RolePermission perm, boolean value) {
        permissions.put(perm, value);
    }

    public Map<RolePermission, Boolean> getPermissions() {
        return permissions;
    }

    public UUID getId() { return id; }
    public UUID getNationId() { return nationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getFormattedPermissions() {
        StringBuilder sb = new StringBuilder();
        for (RolePermission perm : RolePermission.values()) {
            boolean has = permissions.getOrDefault(perm, false);
            sb.append(has ? "§a✔ " : "§c✘ ").append(perm.getLabel()).append("  ");
        }
        return sb.toString().trim();
    }
}
