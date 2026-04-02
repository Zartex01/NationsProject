package fr.nations.jobs;

import fr.nations.NationsPlugin;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class JobManager {

    private final NationsPlugin plugin;
    // Cache: playerId → (jobType → data)
    private final Map<UUID, Map<JobType, PlayerJobData>> cache = new HashMap<>();

    public JobManager(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── DB ──────────────────────────────────────────────────────────────────

    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_jobs (
                player_id   TEXT    NOT NULL,
                job_type    TEXT    NOT NULL,
                level       INTEGER DEFAULT 1,
                xp          INTEGER DEFAULT 0,
                total_xp    INTEGER DEFAULT 0,
                coins_earned INTEGER DEFAULT 0,
                PRIMARY KEY (player_id, job_type)
            )
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Jobs] Erreur création table", e);
        }
    }

    // ─── Load / Save ─────────────────────────────────────────────────────────

    public void loadPlayer(UUID playerId) {
        Map<JobType, PlayerJobData> map = new EnumMap<>(JobType.class);
        // Init avec des données vides
        for (JobType type : JobType.values()) map.put(type, PlayerJobData.empty(type));

        String sql = "SELECT * FROM player_jobs WHERE player_id = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        JobType type = JobType.valueOf(rs.getString("job_type"));
                        PlayerJobData data = new PlayerJobData(
                            type,
                            rs.getInt("level"),
                            rs.getInt("xp"),
                            rs.getLong("total_xp"),
                            rs.getLong("coins_earned")
                        );
                        map.put(type, data);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Jobs] Erreur chargement joueur " + playerId, e);
        }
        cache.put(playerId, map);
    }

    public void savePlayer(UUID playerId) {
        Map<JobType, PlayerJobData> map = cache.get(playerId);
        if (map == null) return;
        String sql = """
            INSERT OR REPLACE INTO player_jobs (player_id, job_type, level, xp, total_xp, coins_earned)
            VALUES (?,?,?,?,?,?)
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PlayerJobData data : map.values()) {
                ps.setString(1, playerId.toString());
                ps.setString(2, data.getJobType().name());
                ps.setInt(3, data.getLevel());
                ps.setInt(4, data.getXp());
                ps.setLong(5, data.getTotalXpEarned());
                ps.setLong(6, data.getCoinsEarned());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Jobs] Erreur sauvegarde joueur " + playerId, e);
        }
    }

    public void unloadPlayer(UUID playerId) {
        savePlayer(playerId);
        cache.remove(playerId);
    }

    // ─── Accès données ────────────────────────────────────────────────────────

    public PlayerJobData getData(UUID playerId, JobType type) {
        if (!cache.containsKey(playerId)) loadPlayer(playerId);
        return cache.get(playerId).get(type);
    }

    public Map<JobType, PlayerJobData> getAllData(UUID playerId) {
        if (!cache.containsKey(playerId)) loadPlayer(playerId);
        return Collections.unmodifiableMap(cache.get(playerId));
    }

    /**
     * Ajoute de l'XP à un joueur pour un métier.
     * Retourne les niveaux gagnés (0 si aucun).
     */
    public int addXp(UUID playerId, JobType type, int xp) {
        if (!cache.containsKey(playerId)) loadPlayer(playerId);
        PlayerJobData data = cache.get(playerId).get(type);
        return data.addXp(xp);
    }

    public boolean isLoaded(UUID playerId) {
        return cache.containsKey(playerId);
    }
}
