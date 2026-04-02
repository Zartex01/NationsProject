package fr.nations.jobs;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class JobManager {

    public static final int MAX_ACTIVE_JOBS = 2;

    private final NationsPlugin plugin;
    /** Cache: playerId → (jobType → data) */
    private final Map<UUID, Map<JobType, PlayerJobData>> cache = new HashMap<>();

    public JobManager(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── DB ──────────────────────────────────────────────────────────────────

    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_jobs (
                player_id    TEXT    NOT NULL,
                job_type     TEXT    NOT NULL,
                level        INTEGER DEFAULT 1,
                xp           INTEGER DEFAULT 0,
                total_xp     INTEGER DEFAULT 0,
                coins_earned INTEGER DEFAULT 0,
                active       INTEGER DEFAULT 0,
                actions      INTEGER DEFAULT 0,
                PRIMARY KEY (player_id, job_type)
            )
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            // Migration: ajouter les colonnes si la table existait déjà sans elles
            for (String col : List.of(
                    "ALTER TABLE player_jobs ADD COLUMN active  INTEGER DEFAULT 0",
                    "ALTER TABLE player_jobs ADD COLUMN actions INTEGER DEFAULT 0")) {
                try { stmt.executeUpdate(col); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Jobs] Erreur création table", e);
        }
    }

    // ─── Load / Save ─────────────────────────────────────────────────────────

    public void loadPlayer(UUID playerId) {
        Map<JobType, PlayerJobData> map = new EnumMap<>(JobType.class);
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
                            rs.getLong("coins_earned"),
                            rs.getInt("active") == 1,
                            rs.getLong("actions")
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
            INSERT OR REPLACE INTO player_jobs
                (player_id, job_type, level, xp, total_xp, coins_earned, active, actions)
            VALUES (?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PlayerJobData data : map.values()) {
                ps.setString(1, playerId.toString());
                ps.setString(2, data.getJobType().name());
                ps.setInt   (3, data.getLevel());
                ps.setInt   (4, data.getXp());
                ps.setLong  (5, data.getTotalXpEarned());
                ps.setLong  (6, data.getCoinsEarned());
                ps.setInt   (7, data.isActive() ? 1 : 0);
                ps.setLong  (8, data.getActionsCount());
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

    public boolean isLoaded(UUID playerId) {
        return cache.containsKey(playerId);
    }

    // ─── Rejoindre / Quitter ─────────────────────────────────────────────────

    /**
     * Tentative de rejoindre un métier.
     * @return true si réussi, false si déjà plein (MAX_ACTIVE_JOBS atteint)
     */
    public boolean joinJob(Player player, JobType type) {
        UUID id = player.getUniqueId();
        if (!cache.containsKey(id)) loadPlayer(id);
        Map<JobType, PlayerJobData> map = cache.get(id);
        PlayerJobData data = map.get(type);

        if (data.isActive()) return true; // Déjà actif

        long activeCount = map.values().stream().filter(PlayerJobData::isActive).count();
        if (activeCount >= MAX_ACTIVE_JOBS) return false;

        data.setActive(true);
        String color = type.getColor();
        player.sendMessage("");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("  §a§l✔ MÉTIER REJOINT — " + type.getColoredName());
        player.sendMessage("  " + color + "Vous êtes maintenant " + type.getDisplayName() + " §f!");
        player.sendMessage("  §7Gagnez de l'XP en pratiquant ce métier.");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayer(id));
        return true;
    }

    /** Quitte un métier actif. */
    public void leaveJob(Player player, JobType type) {
        UUID id = player.getUniqueId();
        if (!cache.containsKey(id)) loadPlayer(id);
        PlayerJobData data = cache.get(id).get(type);

        if (!data.isActive()) return;
        data.setActive(false);

        String color = type.getColor();
        player.sendMessage("");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("  §c§l✖ MÉTIER QUITTÉ — " + type.getColoredName());
        player.sendMessage("  §7Vous n'êtes plus " + color + type.getDisplayName() + "§7.");
        player.sendMessage("  §7Votre progression est sauvegardée.");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayer(id));
    }

    // ─── XP ──────────────────────────────────────────────────────────────────

    /**
     * Ajoute de l'XP (avec multiplicateur de tier appliqué automatiquement).
     * N'accorde de l'XP que si le métier est actif.
     * @return niveaux gagnés (0 si aucun ou métier inactif)
     */
    public int addXp(UUID playerId, JobType type, int rawXp) {
        if (!cache.containsKey(playerId)) loadPlayer(playerId);
        PlayerJobData data = cache.get(playerId).get(type);
        if (!data.isActive()) return 0;
        int finalXp = (int) Math.round(rawXp * data.getXpMultiplier());
        return data.addXp(finalXp);
    }

    /** XP final après multiplicateur, ou 0 si inactif. */
    public int computeXp(UUID playerId, JobType type, int rawXp) {
        if (!cache.containsKey(playerId)) loadPlayer(playerId);
        PlayerJobData data = cache.get(playerId).get(type);
        if (!data.isActive()) return 0;
        return (int) Math.round(rawXp * data.getXpMultiplier());
    }

    public boolean isJobActive(UUID playerId, JobType type) {
        if (!cache.containsKey(playerId)) loadPlayer(playerId);
        return cache.get(playerId).get(type).isActive();
    }

    public int getActiveCount(UUID playerId) {
        if (!cache.containsKey(playerId)) loadPlayer(playerId);
        return (int) cache.get(playerId).values().stream().filter(PlayerJobData::isActive).count();
    }
}
