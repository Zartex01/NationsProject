package fr.nations.season;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SeasonManager {

    private final NationsPlugin plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private int currentSeason;
    private long seasonStartTime;
    private BukkitTask seasonCheckTask;

    public SeasonManager(NationsPlugin plugin) {
        this.plugin = plugin;
        this.playerStats = new HashMap<>();
        this.currentSeason = 1;
        this.seasonStartTime = System.currentTimeMillis();
        startSeasonCheckTask();
    }

    private void startSeasonCheckTask() {
        long checkIntervalTicks = 20L * 60 * 10;
        seasonCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkSeasonEnd, checkIntervalTicks, checkIntervalTicks);
    }

    private void checkSeasonEnd() {
        int durationDays = plugin.getConfigManager().getSeasonDurationDays();
        long durationMillis = (long) durationDays * 24 * 60 * 60 * 1000;
        if (System.currentTimeMillis() - seasonStartTime >= durationMillis) {
            endSeason();
        }
    }

    public void endSeason() {
        distributeSeasonRewards();
        resetSeasonData();
        currentSeason++;
        seasonStartTime = System.currentTimeMillis();
        plugin.getDataManager().saveSeasons();

        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix()
            + "§6La saison §e" + (currentSeason - 1) + " §6est terminée! La saison §e" + currentSeason + " §6commence!");
    }

    private void distributeSeasonRewards() {
        List<Nation> sorted = plugin.getNationManager().getNationsSortedByPoints();

        if (sorted.size() >= 1) {
            Nation first = sorted.get(0);
            int reward = plugin.getConfigManager().getSeasonFirstPlaceReward();
            first.depositToBank(reward);
            broadcastNationRanking(1, first, reward);
        }
        if (sorted.size() >= 2) {
            Nation second = sorted.get(1);
            int reward = plugin.getConfigManager().getSeasonSecondPlaceReward();
            second.depositToBank(reward);
            broadcastNationRanking(2, second, reward);
        }
        if (sorted.size() >= 3) {
            Nation third = sorted.get(2);
            int reward = plugin.getConfigManager().getSeasonThirdPlaceReward();
            third.depositToBank(reward);
            broadcastNationRanking(3, third, reward);
        }
    }

    private void broadcastNationRanking(int rank, Nation nation, int reward) {
        String medal = switch (rank) {
            case 1 -> "§6§l🥇";
            case 2 -> "§7§l🥈";
            case 3 -> "§c§l🥉";
            default -> "§7#" + rank;
        };
        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix()
            + medal + " §f" + nation.getName() + " §7— §a+" + reward + " coins §7(" + nation.getSeasonPoints() + " pts)");
    }

    private void resetSeasonData() {
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            nation.resetSeasonPoints();
        }
        playerStats.clear();
        plugin.getDataManager().saveNations();
    }

    public void addPlayerStats(PlayerStats stats) {
        playerStats.put(stats.getPlayerId(), stats);
    }

    public PlayerStats getOrCreatePlayerStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, id -> new PlayerStats(id, 0, 0, 0, 0));
    }

    public PlayerStats getPlayerStats(UUID playerId) {
        return playerStats.get(playerId);
    }

    public void addPlayerStat(UUID playerId, String stat, int amount) {
        PlayerStats stats = getOrCreatePlayerStats(playerId);
        switch (stat.toLowerCase()) {
            case "kills" -> { for (int i = 0; i < amount; i++) stats.addKill(); }
            case "deaths" -> { for (int i = 0; i < amount; i++) stats.addDeath(); }
            case "wars-won" -> { for (int i = 0; i < amount; i++) stats.addWarWin(); }
            case "claims" -> { for (int i = 0; i < amount; i++) stats.addClaim(); }
        }
        plugin.getDataManager().saveSeasons();
    }

    public void recordWarWin(UUID winnerNationId) {
        Nation nation = plugin.getNationManager().getNationById(winnerNationId);
        if (nation == null) return;
        for (UUID memberId : nation.getMembers().keySet()) {
            getOrCreatePlayerStats(memberId).addWarWin();
            plugin.getGradeManager().addXp(memberId, plugin.getConfigManager().getXpPerWarWon());
        }
        plugin.getDataManager().saveSeasons();
    }

    public Collection<PlayerStats> getAllPlayerStats() {
        return Collections.unmodifiableCollection(playerStats.values());
    }

    public List<PlayerStats> getTopPlayersByKills(int limit) {
        return playerStats.values().stream()
            .sorted(Comparator.comparingInt(PlayerStats::getKills).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    public int getCurrentSeason() { return currentSeason; }
    public void setCurrentSeason(int season) { this.currentSeason = season; }
    public long getSeasonStartTime() { return seasonStartTime; }
    public void setSeasonStartTime(long time) { this.seasonStartTime = time; }

    public long getSeasonTimeRemainingMillis() {
        long durationMillis = (long) plugin.getConfigManager().getSeasonDurationDays() * 24 * 60 * 60 * 1000;
        return Math.max(0, durationMillis - (System.currentTimeMillis() - seasonStartTime));
    }

    public String getFormattedSeasonTimeRemaining() {
        long remaining = getSeasonTimeRemainingMillis();
        long days = remaining / 86400000;
        long hours = (remaining % 86400000) / 3600000;
        return days + "j " + hours + "h";
    }

    public void loadFromDatabase() {
        String sqlSeason = "SELECT season_number, started_at FROM seasons WHERE current=TRUE LIMIT 1";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSeason);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                this.currentSeason = rs.getInt("season_number");
                this.seasonStartTime = rs.getLong("started_at");
            } else {
                saveCurrentSeasonToDatabase();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Season] Erreur chargement saison", e);
        }

        String sqlStats = "SELECT player_id, kills, deaths, wars_won, claims FROM season_stats WHERE season_number=?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlStats)) {
            ps.setInt(1, currentSeason);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("player_id"));
                    PlayerStats stats = new PlayerStats(
                        id,
                        rs.getInt("kills"),
                        rs.getInt("deaths"),
                        rs.getInt("wars_won"),
                        rs.getInt("claims")
                    );
                    playerStats.put(id, stats);
                    count++;
                }
                plugin.getLogger().info("[Season] " + count + " stats joueurs chargées depuis la DB.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Season] Erreur chargement stats", e);
        }
    }

    public void saveCurrentSeasonToDatabase() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = """
            INSERT INTO seasons (season_number, started_at, current) VALUES (?,?,TRUE)
            ON CONFLICT (season_number) DO UPDATE SET started_at=EXCLUDED.started_at, current=TRUE
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentSeason);
            ps.setLong(2, seasonStartTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Season] Erreur sauvegarde saison", e);
        }
    }

    public void savePlayerStatToDatabase(UUID playerId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        PlayerStats stats = playerStats.get(playerId);
        if (stats == null) return;
        String sql = """
            INSERT INTO season_stats (player_id, season_number, kills, deaths, wars_won, claims)
            VALUES (?,?,?,?,?,?)
            ON CONFLICT (player_id, season_number) DO UPDATE SET
                kills=EXCLUDED.kills, deaths=EXCLUDED.deaths,
                wars_won=EXCLUDED.wars_won, claims=EXCLUDED.claims
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setInt(2, currentSeason);
            ps.setInt(3, stats.getKills());
            ps.setInt(4, stats.getDeaths());
            ps.setInt(5, stats.getWarsWon());
            ps.setInt(6, stats.getClaimsCount());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Season] Erreur sauvegarde stat joueur", e);
        }
    }

    public void shutdown() {
        if (seasonCheckTask != null) seasonCheckTask.cancel();
    }
}
