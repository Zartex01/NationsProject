package fr.nations.kits;

import fr.nations.NationsPlugin;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class KitManager {

    private final NationsPlugin plugin;

    /** Cache local: playerId → (kitType → timestamp de dernier claim en secondes) */
    private final Map<UUID, Map<KitType, Long>> cache = new HashMap<>();

    public KitManager(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Table ────────────────────────────────────────────────────────────────

    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS kit_cooldowns (
                player_id TEXT NOT NULL,
                kit_type  TEXT NOT NULL,
                claimed_at INTEGER NOT NULL,
                PRIMARY KEY (player_id, kit_type)
            )
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Kits] Erreur création table", e);
        }
    }

    // ─── Cooldown ─────────────────────────────────────────────────────────────

    /**
     * @return timestamp (epoch secondes) du dernier claim du joueur pour ce kit, ou 0 si jamais claim.
     */
    public long getLastClaim(UUID playerId, KitType kit) {
        loadIfAbsent(playerId);
        return cache.get(playerId).getOrDefault(kit, 0L);
    }

    /**
     * @return true si le joueur peut claim ce kit (cooldown écoulé ou jamais claim).
     */
    public boolean canClaim(UUID playerId, KitType kit) {
        long last = getLastClaim(playerId, kit);
        if (last == 0L) return true;
        long now = System.currentTimeMillis() / 1000L;
        return (now - last) >= kit.getCooldownSeconds();
    }

    /**
     * @return secondes restantes avant de pouvoir re-claim, ou 0 si disponible.
     */
    public long remainingCooldown(UUID playerId, KitType kit) {
        long last = getLastClaim(playerId, kit);
        if (last == 0L) return 0L;
        long now  = System.currentTimeMillis() / 1000L;
        long diff = kit.getCooldownSeconds() - (now - last);
        return Math.max(0L, diff);
    }

    /**
     * Enregistre un claim et met à jour le cache + la base de données.
     */
    public void recordClaim(UUID playerId, KitType kit) {
        loadIfAbsent(playerId);
        long now = System.currentTimeMillis() / 1000L;
        cache.get(playerId).put(kit, now);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT OR REPLACE INTO kit_cooldowns (player_id, kit_type, claimed_at)
                VALUES (?, ?, ?)
            """;
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerId.toString());
                ps.setString(2, kit.name());
                ps.setLong(3, now);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[Kits] Erreur sauvegarde cooldown", e);
            }
        });
    }

    // ─── Cache ────────────────────────────────────────────────────────────────

    private void loadIfAbsent(UUID playerId) {
        if (cache.containsKey(playerId)) return;
        Map<KitType, Long> map = new HashMap<>();

        String sql = "SELECT kit_type, claimed_at FROM kit_cooldowns WHERE player_id = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        KitType kt = KitType.valueOf(rs.getString("kit_type"));
                        map.put(kt, rs.getLong("claimed_at"));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Kits] Erreur chargement cooldowns", e);
        }

        cache.put(playerId, map);
    }

    public void unloadPlayer(UUID playerId) {
        cache.remove(playerId);
    }

    // ─── Utilitaire affichage ─────────────────────────────────────────────────

    /** Formate une durée en secondes → "Xh Ym Zs" lisible. */
    public static String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        sb.append(s).append("s");
        return sb.toString().trim();
    }
}
