package fr.nations.role;

import fr.nations.NationsPlugin;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CustomRoleManager {

    private final NationsPlugin plugin;
    private final Map<UUID, CustomRole> roles = new HashMap<>();
    private final Map<UUID, UUID> playerRoleAssignments = new HashMap<>();

    public CustomRoleManager(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        loadRoles();
        loadPlayerAssignments();
    }

    private void loadRoles() {
        String sql = "SELECT * FROM custom_roles";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID nationId = UUID.fromString(rs.getString("nation_id"));
                String name = rs.getString("name");
                String displayName = rs.getString("display_name");
                int rank = rs.getInt("rank");
                CustomRole role = new CustomRole(id, nationId, name, displayName, rank);
                for (RolePermission perm : RolePermission.values()) {
                    role.setPermission(perm, rs.getBoolean(perm.getColumnName()));
                }
                roles.put(id, role);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Roles] Erreur chargement rôles", e);
        }
    }

    private void loadPlayerAssignments() {
        String sql = "SELECT player_id, role_id FROM player_custom_roles";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                playerRoleAssignments.put(
                    UUID.fromString(rs.getString("player_id")),
                    UUID.fromString(rs.getString("role_id"))
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Roles] Erreur chargement assignations", e);
        }
    }

    public CustomRole createRole(UUID nationId, String name, String displayName, int rank) {
        UUID id = UUID.randomUUID();
        CustomRole role = new CustomRole(id, nationId, name, displayName, rank);
        roles.put(id, role);
        saveRole(role);
        return role;
    }

    public void saveRole(CustomRole role) {
        String sql = """
            INSERT INTO custom_roles (id, nation_id, name, display_name, rank,
                perm_build, perm_invite, perm_kick, perm_manage_war, perm_manage_bank,
                perm_manage_claims, perm_manage_allies, perm_manage_roles, perm_rename, perm_disband)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET
                name=EXCLUDED.name, display_name=EXCLUDED.display_name, rank=EXCLUDED.rank,
                perm_build=EXCLUDED.perm_build, perm_invite=EXCLUDED.perm_invite,
                perm_kick=EXCLUDED.perm_kick, perm_manage_war=EXCLUDED.perm_manage_war,
                perm_manage_bank=EXCLUDED.perm_manage_bank, perm_manage_claims=EXCLUDED.perm_manage_claims,
                perm_manage_allies=EXCLUDED.perm_manage_allies, perm_manage_roles=EXCLUDED.perm_manage_roles,
                perm_rename=EXCLUDED.perm_rename, perm_disband=EXCLUDED.perm_disband
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, role.getId());
            ps.setObject(2, role.getNationId());
            ps.setString(3, role.getName());
            ps.setString(4, role.getDisplayName());
            ps.setInt(5, role.getRank());
            ps.setBoolean(6, role.hasPermission(RolePermission.BUILD));
            ps.setBoolean(7, role.hasPermission(RolePermission.INVITE));
            ps.setBoolean(8, role.hasPermission(RolePermission.KICK));
            ps.setBoolean(9, role.hasPermission(RolePermission.MANAGE_WAR));
            ps.setBoolean(10, role.hasPermission(RolePermission.MANAGE_BANK));
            ps.setBoolean(11, role.hasPermission(RolePermission.MANAGE_CLAIMS));
            ps.setBoolean(12, role.hasPermission(RolePermission.MANAGE_ALLIES));
            ps.setBoolean(13, role.hasPermission(RolePermission.MANAGE_ROLES));
            ps.setBoolean(14, role.hasPermission(RolePermission.RENAME));
            ps.setBoolean(15, role.hasPermission(RolePermission.DISBAND));
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Roles] Erreur sauvegarde rôle", e);
        }
    }

    public void deleteRole(UUID roleId) {
        roles.remove(roleId);
        playerRoleAssignments.entrySet().removeIf(e -> e.getValue().equals(roleId));
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM custom_roles WHERE id=?")) {
            ps.setObject(1, roleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Roles] Erreur suppression rôle", e);
        }
    }

    public void assignPlayerRole(UUID playerId, UUID nationId, UUID roleId) {
        playerRoleAssignments.put(playerId, roleId);
        String sql = """
            INSERT INTO player_custom_roles (player_id, nation_id, role_id) VALUES (?,?,?)
            ON CONFLICT (player_id, nation_id) DO UPDATE SET role_id=EXCLUDED.role_id
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setObject(2, nationId);
            ps.setObject(3, roleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Roles] Erreur assignation rôle", e);
        }
    }

    public void removePlayerRole(UUID playerId, UUID nationId) {
        playerRoleAssignments.remove(playerId);
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM player_custom_roles WHERE player_id=? AND nation_id=?")) {
            ps.setObject(1, playerId);
            ps.setObject(2, nationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Roles] Erreur suppression assignation", e);
        }
    }

    public CustomRole getPlayerRole(UUID playerId) {
        UUID roleId = playerRoleAssignments.get(playerId);
        if (roleId == null) return null;
        return roles.get(roleId);
    }

    public boolean hasPermission(UUID playerId, RolePermission perm) {
        CustomRole role = getPlayerRole(playerId);
        if (role == null) return false;
        return role.hasPermission(perm);
    }

    public List<CustomRole> getRolesForNation(UUID nationId) {
        return roles.values().stream()
            .filter(r -> r.getNationId().equals(nationId))
            .sorted(Comparator.comparingInt(CustomRole::getRank))
            .collect(Collectors.toList());
    }

    public CustomRole getRoleByName(UUID nationId, String name) {
        return roles.values().stream()
            .filter(r -> r.getNationId().equals(nationId) && r.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public CustomRole getRole(UUID roleId) {
        return roles.get(roleId);
    }

    public int getRoleCount(UUID nationId) {
        return (int) roles.values().stream().filter(r -> r.getNationId().equals(nationId)).count();
    }
}
