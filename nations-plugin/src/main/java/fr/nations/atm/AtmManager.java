package fr.nations.atm;

import fr.nations.NationsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class AtmManager {

    private final NationsPlugin plugin;
    private final Map<UUID, Long> sessionStartTimes = new HashMap<>();

    public AtmManager(NationsPlugin plugin) {
        this.plugin = plugin;
        for (Player p : Bukkit.getOnlinePlayers()) {
            sessionStartTimes.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }

    public void onPlayerJoin(UUID playerId) {
        sessionStartTimes.put(playerId, System.currentTimeMillis());
        ensurePlayerExists(playerId);
    }

    public void onPlayerQuit(UUID playerId) {
        Long start = sessionStartTimes.remove(playerId);
        if (start == null) return;
        long sessionSeconds = (System.currentTimeMillis() - start) / 1000L;
        addPlaytimeSeconds(playerId, sessionSeconds);
    }

    public void flushAllSessions() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : sessionStartTimes.entrySet()) {
            long sessionSeconds = (now - entry.getValue()) / 1000L;
            addPlaytimeSeconds(entry.getKey(), sessionSeconds);
        }
        sessionStartTimes.clear();
    }

    private void ensurePlayerExists(UUID playerId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = """
            INSERT INTO player_playtime (player_id, total_seconds, claimed_seconds) VALUES (?, 0, 0)
            ON CONFLICT (player_id) DO NOTHING
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[ATM] Erreur création joueur playtime", e);
        }
    }

    private void addPlaytimeSeconds(UUID playerId, long seconds) {
        if (!plugin.getDatabaseManager().isConnected() || seconds <= 0) return;
        String sql = """
            INSERT INTO player_playtime (player_id, total_seconds, claimed_seconds) VALUES (?, ?, 0)
            ON CONFLICT (player_id) DO UPDATE SET total_seconds = total_seconds + ?
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setLong(2, seconds);
            ps.setLong(3, seconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[ATM] Erreur ajout playtime", e);
        }
    }

    private long getCurrentSessionSeconds(UUID playerId) {
        Long start = sessionStartTimes.get(playerId);
        if (start == null) return 0;
        return (System.currentTimeMillis() - start) / 1000L;
    }

    public long getClaimableMinutes(UUID playerId) {
        long sessionSeconds = getCurrentSessionSeconds(playerId);

        if (!plugin.getDatabaseManager().isConnected()) {
            return sessionSeconds / 60L;
        }

        String sql = "SELECT total_seconds, claimed_seconds FROM player_playtime WHERE player_id = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long total = rs.getLong("total_seconds") + sessionSeconds;
                    long claimed = rs.getLong("claimed_seconds");
                    long unclaimedSeconds = Math.max(0, total - claimed);
                    return unclaimedSeconds / 60L;
                } else {
                    return sessionSeconds / 60L;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[ATM] Erreur lecture playtime", e);
        }
        return sessionSeconds / 60L;
    }

    public boolean claimReward(UUID playerId) {
        long minutes = getClaimableMinutes(playerId);
        if (minutes <= 0) return false;

        double reward = minutes * 2.0;
        long sessionSeconds = getCurrentSessionSeconds(playerId);

        String sql = """
            INSERT INTO player_playtime (player_id, total_seconds, claimed_seconds)
            VALUES (?, ?, ?)
            ON CONFLICT (player_id) DO UPDATE SET
                claimed_seconds = total_seconds + ?
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setLong(2, sessionSeconds);
            ps.setLong(3, sessionSeconds);
            ps.setLong(4, sessionSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[ATM] Erreur claim ATM", e);
            return false;
        }

        plugin.getEconomyManager().deposit(playerId, reward);
        return true;
    }

    public long getRewardForMinutes(long minutes) {
        return minutes * 2L;
    }
}
