package fr.nations.territory;

import fr.nations.NationsPlugin;
import fr.nations.grade.PlayerGrade;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TerritoryManager {

    private final NationsPlugin plugin;
    private final Map<String, ClaimedChunk> claimedChunks;

    public TerritoryManager(NationsPlugin plugin) {
        this.plugin = plugin;
        this.claimedChunks = new HashMap<>();
    }

    public void addClaim(ClaimedChunk chunk) {
        claimedChunks.put(chunk.getKey(), chunk);
        PlayerGrade grade = plugin.getGradeManager().getPlayerGrade(chunk.getClaimedBy());
        if (grade != null) {
            grade.incrementClaimCount();
        }
    }

    public ClaimResult claimChunk(Player player, Chunk chunk) {
        String key = buildKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());

        if (claimedChunks.containsKey(key)) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        if (!plugin.getNationManager().hasNation(player.getUniqueId())) {
            return ClaimResult.NO_NATION;
        }

        PlayerGrade grade = plugin.getGradeManager().getOrCreatePlayerGrade(player.getUniqueId(), player.getName());
        int maxClaims = plugin.getConfigManager().getMaxClaimsForGrade(grade.getGradeName());

        if (grade.getClaimCount() >= maxClaims) {
            return ClaimResult.MAX_CLAIMS_REACHED;
        }

        double cost = plugin.getConfigManager().getClaimPrice();
        if (!plugin.getEconomyManager().has(player.getUniqueId(), cost)) {
            return ClaimResult.INSUFFICIENT_FUNDS;
        }

        plugin.getEconomyManager().withdraw(player.getUniqueId(), cost);

        UUID nationId = plugin.getNationManager().getPlayerNation(player.getUniqueId()).getId();
        ClaimedChunk claimed = new ClaimedChunk(
            chunk.getWorld().getName(),
            chunk.getX(),
            chunk.getZ(),
            nationId,
            player.getUniqueId(),
            System.currentTimeMillis()
        );

        claimedChunks.put(key, claimed);
        grade.incrementClaimCount();

        plugin.getNationManager().getPlayerNation(player.getUniqueId()).addSeasonPoints(5);
        plugin.getSeasonManager().addPlayerStat(player.getUniqueId(), "claims", 1);
        plugin.getGradeManager().addXp(player.getUniqueId(), plugin.getConfigManager().getXpPerClaim());

        saveChunkToDatabase(claimed);
        plugin.getGradeManager().saveGradeToDatabase(player.getUniqueId());
        return ClaimResult.SUCCESS;
    }

    public boolean unclaimChunk(Player player, Chunk chunk) {
        String key = buildKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        ClaimedChunk claimed = claimedChunks.get(key);
        if (claimed == null) return false;

        UUID playerNationId = plugin.getNationManager().getPlayerNation(player.getUniqueId()) != null
            ? plugin.getNationManager().getPlayerNation(player.getUniqueId()).getId()
            : null;

        if (!claimed.getNationId().equals(playerNationId)) return false;

        claimedChunks.remove(key);
        PlayerGrade grade = plugin.getGradeManager().getPlayerGrade(player.getUniqueId());
        if (grade != null) grade.decrementClaimCount();

        deleteChunkFromDatabase(claimed.getWorldName(), claimed.getChunkX(), claimed.getChunkZ());
        return true;
    }

    public void unclaimAllForNation(UUID nationId) {
        List<String> toRemove = claimedChunks.entrySet().stream()
            .filter(e -> e.getValue().getNationId().equals(nationId))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        for (String key : toRemove) {
            ClaimedChunk chunk = claimedChunks.remove(key);
            PlayerGrade grade = plugin.getGradeManager().getPlayerGrade(chunk.getClaimedBy());
            if (grade != null) grade.decrementClaimCount();
        }

        deleteAllChunksForNation(nationId);
    }

    public ClaimedChunk getClaimedChunk(Chunk chunk) {
        return claimedChunks.get(buildKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
    }

    public ClaimedChunk getClaimedChunkByKey(String key) {
        return claimedChunks.get(key);
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return claimedChunks.containsKey(buildKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
    }

    public boolean isChunkOwnedByNation(Chunk chunk, UUID nationId) {
        ClaimedChunk claimed = getClaimedChunk(chunk);
        return claimed != null && claimed.getNationId().equals(nationId);
    }

    public List<ClaimedChunk> getClaimsForNation(UUID nationId) {
        return claimedChunks.values().stream()
            .filter(c -> c.getNationId().equals(nationId))
            .collect(Collectors.toList());
    }

    public int getClaimCountForNation(UUID nationId) {
        return getClaimsForNation(nationId).size();
    }

    public Collection<ClaimedChunk> getAllClaims() {
        return Collections.unmodifiableCollection(claimedChunks.values());
    }

    private String buildKey(String world, int x, int z) {
        return world + "_" + x + "_" + z;
    }

    public void loadFromDatabase() {
        String sql = "SELECT id, nation_id, world_name, chunk_x, chunk_z, claimed_at FROM claimed_chunks";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID nationId = UUID.fromString(rs.getString("nation_id"));
                String world = rs.getString("world_name");
                int x = rs.getInt("chunk_x");
                int z = rs.getInt("chunk_z");
                long claimedAt = rs.getLong("claimed_at");
                ClaimedChunk chunk = new ClaimedChunk(id, world, x, z, nationId, null, claimedAt);
                claimedChunks.put(buildKey(world, x, z), chunk);
                count++;
            }
            plugin.getLogger().info("[Territory] " + count + " chunks chargés depuis la DB.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Territory] Erreur chargement DB", e);
        }
    }

    public void saveChunkToDatabase(ClaimedChunk chunk) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = """
            INSERT OR IGNORE INTO claimed_chunks (id, nation_id, world_name, chunk_x, chunk_z, claimed_at)
            VALUES (?,?,?,?,?,?)
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (chunk.getId() != null ? chunk.getId() : UUID.randomUUID()).toString());
            ps.setString(2, chunk.getNationId().toString());
            ps.setString(3, chunk.getWorldName());
            ps.setInt(4, chunk.getChunkX());
            ps.setInt(5, chunk.getChunkZ());
            ps.setLong(6, chunk.getClaimedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Territory] Erreur sauvegarde chunk", e);
        }
    }

    public void deleteChunkFromDatabase(String world, int x, int z) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = "DELETE FROM claimed_chunks WHERE world_name=? AND chunk_x=? AND chunk_z=?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Territory] Erreur suppression chunk", e);
        }
    }

    public void deleteAllChunksForNation(UUID nationId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = "DELETE FROM claimed_chunks WHERE nation_id=?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nationId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Territory] Erreur suppression chunks nation", e);
        }
    }

    public enum ClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        NO_NATION,
        MAX_CLAIMS_REACHED,
        INSUFFICIENT_FUNDS
    }
}
