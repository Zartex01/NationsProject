package fr.nations.season;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
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

    public void shutdown() {
        if (seasonCheckTask != null) seasonCheckTask.cancel();
        plugin.getDataManager().saveSeasons();
    }
}
