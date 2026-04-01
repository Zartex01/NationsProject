package fr.nations.grade;

import fr.nations.NationsPlugin;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class GradeManager {

    private final NationsPlugin plugin;
    private final Map<UUID, PlayerGrade> playerGrades;

    public GradeManager(NationsPlugin plugin) {
        this.plugin = plugin;
        this.playerGrades = new HashMap<>();
    }

    public void addPlayerGrade(PlayerGrade grade) {
        playerGrades.put(grade.getPlayerId(), grade);
    }

    public PlayerGrade getPlayerGrade(UUID playerId) {
        return playerGrades.get(playerId);
    }

    public PlayerGrade getOrCreatePlayerGrade(UUID playerId, String playerName) {
        return playerGrades.computeIfAbsent(playerId, id ->
            new PlayerGrade(id, GradeType.JOUEUR.name(), 1, 0, 0)
        );
    }

    public GradeType getEffectiveGrade(Player player) {
        GradeType permGrade = GradeType.fromPermission(player);
        PlayerGrade stored = playerGrades.get(player.getUniqueId());
        if (stored == null) return permGrade;
        try {
            GradeType dbGrade = GradeType.valueOf(stored.getGradeName().toUpperCase());
            return dbGrade.ordinal() > permGrade.ordinal() ? dbGrade : permGrade;
        } catch (IllegalArgumentException e) {
            return permGrade;
        }
    }

    public String getPlayerGradeName(Player player) {
        GradeType gradeType = getEffectiveGrade(player);
        PlayerGrade grade = getOrCreatePlayerGrade(player.getUniqueId(), player.getName());
        grade.setGradeName(gradeType.name());
        return gradeType.name();
    }

    public int getMaxClaims(Player player) {
        return getEffectiveGrade(player).getMaxClaims();
    }

    public void addXp(UUID playerId, long amount) {
        PlayerGrade grade = playerGrades.get(playerId);
        if (grade == null) return;

        int maxLevel = plugin.getConfigManager().getMaxLevel();
        if (grade.getLevel() >= maxLevel) return;

        grade.addXp(amount);

        while (grade.canLevelUp() && grade.getLevel() < maxLevel) {
            long xpCost = grade.getXpForNextLevel();
            grade.setXp(grade.getXp() - xpCost);
            grade.setLevel(grade.getLevel() + 1);

            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                    + "§aFélicitations! Vous êtes maintenant niveau §6" + grade.getLevel() + "§a!");
            }
        }

        saveGradeToDatabase(playerId);
    }

    public Collection<PlayerGrade> getAllPlayerGrades() {
        return Collections.unmodifiableCollection(playerGrades.values());
    }

    public List<PlayerGrade> getTopPlayersByLevel(int limit) {
        return playerGrades.values().stream()
            .sorted(Comparator.comparingInt(PlayerGrade::getLevel).reversed()
                .thenComparingLong(PlayerGrade::getXp).reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    public double getLevelProgressPercent(PlayerGrade grade) {
        long xpNeeded = grade.getXpForNextLevel();
        if (xpNeeded == 0) return 100.0;
        return Math.min(100.0, (grade.getXp() * 100.0) / xpNeeded);
    }

    public void loadFromDatabase() {
        String sql = "SELECT player_id, player_name, grade, level, xp FROM player_grades";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_id"));
                String grade = rs.getString("grade");
                int level = rs.getInt("level");
                double xp = rs.getDouble("xp");
                PlayerGrade pg = new PlayerGrade(id, grade, level, (long) xp, 0);
                pg.setPlayerName(rs.getString("player_name"));
                playerGrades.put(id, pg);
                count++;
            }
            plugin.getLogger().info("[Grades] " + count + " grades chargés depuis la DB.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Grades] Erreur chargement DB", e);
        }
    }

    public void saveGradeToDatabase(UUID playerId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        PlayerGrade grade = playerGrades.get(playerId);
        if (grade == null) return;
        String sql = """
            INSERT INTO player_grades (player_id, player_name, grade, level, xp) VALUES (?,?,?,?,?)
            ON CONFLICT (player_id) DO UPDATE SET
                player_name=excluded.player_name, grade=excluded.grade,
                level=excluded.level, xp=excluded.xp
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, grade.getPlayerName());
            ps.setString(3, grade.getGradeName());
            ps.setInt(4, grade.getLevel());
            ps.setDouble(5, grade.getXp());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Grades] Erreur sauvegarde grade", e);
        }
    }

    public void saveAllToDatabase() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        for (UUID playerId : playerGrades.keySet()) {
            saveGradeToDatabase(playerId);
        }
    }
}
