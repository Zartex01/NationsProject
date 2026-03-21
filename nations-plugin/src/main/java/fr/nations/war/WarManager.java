package fr.nations.war;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class WarManager {

    private final NationsPlugin plugin;
    private final Map<UUID, War> wars;
    private final Map<UUID, Long> warCooldowns;
    private BukkitTask warCheckTask;

    public WarManager(NationsPlugin plugin) {
        this.plugin = plugin;
        this.wars = new HashMap<>();
        this.warCooldowns = new HashMap<>();
        startWarCheckTask();
    }

    private void startWarCheckTask() {
        warCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpiredWars, 200L, 1200L);
    }

    private void checkExpiredWars() {
        for (War war : new ArrayList<>(wars.values())) {
            if (war.isExpired()) {
                resolveWarByKills(war);
            }
        }
    }

    private void resolveWarByKills(War war) {
        if (war.getAttackerKills() > war.getDefenderKills()) {
            endWar(war.getId(), WarStatus.ATTACKER_WON, null);
        } else if (war.getDefenderKills() > war.getAttackerKills()) {
            endWar(war.getId(), WarStatus.DEFENDER_WON, null);
        } else {
            endWar(war.getId(), WarStatus.DRAW, null);
        }
    }

    public void addWar(War war) {
        wars.put(war.getId(), war);
    }

    public WarDeclarationResult declareWar(Nation attacker, Nation defender, WarType type, String reason) {
        if (attacker.getId().equals(defender.getId())) {
            return WarDeclarationResult.SAME_NATION;
        }

        if (attacker.isAlly(defender.getId())) {
            return WarDeclarationResult.IS_ALLY;
        }

        long cooldownEnd = warCooldowns.getOrDefault(attacker.getId(), 0L);
        if (System.currentTimeMillis() < cooldownEnd) {
            return WarDeclarationResult.ON_COOLDOWN;
        }

        int maxWars = plugin.getConfig().getInt("wars.max-active-wars", 3);
        long attackerActiveWars = wars.values().stream()
            .filter(w -> w.isNationInvolved(attacker.getId()) && w.getStatus().isActive())
            .count();
        if (attackerActiveWars >= maxWars) {
            return WarDeclarationResult.MAX_WARS_REACHED;
        }

        boolean alreadyAtWar = wars.values().stream()
            .filter(w -> w.getStatus().isActive() || w.getStatus().isPending())
            .anyMatch(w -> (w.getAttackerNationId().equals(attacker.getId()) && w.getDefenderNationId().equals(defender.getId()))
                || (w.getAttackerNationId().equals(defender.getId()) && w.getDefenderNationId().equals(attacker.getId())));
        if (alreadyAtWar) {
            return WarDeclarationResult.ALREADY_AT_WAR;
        }

        double cost = type.getCost();
        if (!plugin.getEconomyManager().has(attacker.getLeaderId(), cost)) {
            if (attacker.getBankBalance() < cost) {
                return WarDeclarationResult.INSUFFICIENT_FUNDS;
            }
            attacker.withdrawFromBank(cost);
        } else {
            plugin.getEconomyManager().withdraw(attacker.getLeaderId(), cost);
        }

        long now = System.currentTimeMillis();
        long endsAt = now + type.getDurationMillis();
        War war = new War(UUID.randomUUID(), attacker.getId(), defender.getId(), type, now, endsAt, reason);
        wars.put(war.getId(), war);

        long cooldownHours = plugin.getConfigManager().getWarDeclarationCooldownHours();
        warCooldowns.put(attacker.getId(), now + cooldownHours * 3600000);

        plugin.getDataManager().saveWars();
        return WarDeclarationResult.PENDING_VALIDATION;
    }

    public boolean validateWar(UUID warId, UUID staffId) {
        War war = wars.get(warId);
        if (war == null || !war.getStatus().isPending()) return false;
        war.setStatus(WarStatus.ACTIVE);
        war.setValidatedBy(staffId);
        war.setEndsAt(System.currentTimeMillis() + war.getType().getDurationMillis());
        plugin.getDataManager().saveWars();
        return true;
    }

    public boolean rejectWar(UUID warId, UUID staffId, String note) {
        War war = wars.get(warId);
        if (war == null || !war.getStatus().isPending()) return false;
        war.setStatus(WarStatus.REJECTED);
        war.setValidatedBy(staffId);
        war.setStaffNote(note);

        Nation attacker = plugin.getNationManager().getNationById(war.getAttackerNationId());
        if (attacker != null) {
            attacker.depositToBank(war.getType().getCost() * 0.5);
        }

        plugin.getDataManager().saveWars();
        return true;
    }

    public void endWar(UUID warId, WarStatus result, UUID winnerId) {
        War war = wars.get(warId);
        if (war == null) return;
        war.setStatus(result);

        if (result == WarStatus.ATTACKER_WON) {
            Nation winner = plugin.getNationManager().getNationById(war.getAttackerNationId());
            if (winner != null) {
                winner.addSeasonPoints(100);
                plugin.getSeasonManager().recordWarWin(war.getAttackerNationId());
            }
        } else if (result == WarStatus.DEFENDER_WON) {
            Nation winner = plugin.getNationManager().getNationById(war.getDefenderNationId());
            if (winner != null) {
                winner.addSeasonPoints(75);
                plugin.getSeasonManager().recordWarWin(war.getDefenderNationId());
            }
        }

        plugin.getDataManager().saveWars();
    }

    public void recordKill(UUID killerPlayerId, UUID victimPlayerId) {
        Nation killerNation = plugin.getNationManager().getPlayerNation(killerPlayerId);
        Nation victimNation = plugin.getNationManager().getPlayerNation(victimPlayerId);
        if (killerNation == null || victimNation == null) return;

        War activeWar = getActiveWarBetween(killerNation.getId(), victimNation.getId());
        if (activeWar == null) return;

        if (activeWar.getAttackerNationId().equals(killerNation.getId())) {
            activeWar.incrementAttackerKills();
        } else {
            activeWar.incrementDefenderKills();
        }

        killerNation.addSeasonPoints(10);
        plugin.getSeasonManager().addPlayerStat(killerPlayerId, "kills", 1);
        plugin.getGradeManager().addXp(killerPlayerId, plugin.getConfigManager().getXpPerKill());
        plugin.getDataManager().saveWars();
    }

    public War getActiveWarBetween(UUID nationA, UUID nationB) {
        return wars.values().stream()
            .filter(w -> w.getStatus().isActive())
            .filter(w -> (w.getAttackerNationId().equals(nationA) && w.getDefenderNationId().equals(nationB))
                || (w.getAttackerNationId().equals(nationB) && w.getDefenderNationId().equals(nationA)))
            .findFirst()
            .orElse(null);
    }

    public List<War> getActiveWarsForNation(UUID nationId) {
        return wars.values().stream()
            .filter(w -> w.getStatus().isActive() && w.isNationInvolved(nationId))
            .collect(Collectors.toList());
    }

    public List<War> getPendingWars() {
        return wars.values().stream()
            .filter(w -> w.getStatus().isPending())
            .collect(Collectors.toList());
    }

    public War getWar(UUID warId) {
        return wars.get(warId);
    }

    public Collection<War> getAllWars() {
        return Collections.unmodifiableCollection(wars.values());
    }

    public boolean areNationsAtWar(UUID nationA, UUID nationB) {
        return getActiveWarBetween(nationA, nationB) != null;
    }

    public void shutdown() {
        if (warCheckTask != null) warCheckTask.cancel();
        plugin.getDataManager().saveWars();
    }

    public enum WarDeclarationResult {
        PENDING_VALIDATION,
        SAME_NATION,
        IS_ALLY,
        ON_COOLDOWN,
        MAX_WARS_REACHED,
        ALREADY_AT_WAR,
        INSUFFICIENT_FUNDS
    }
}
