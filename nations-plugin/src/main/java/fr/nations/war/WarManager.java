package fr.nations.war;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
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
    }

    public void startTasks() {
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

    public void loadAll() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = "SELECT * FROM wars";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID attacker = UUID.fromString(rs.getString("attacker_nation_id"));
                UUID defender = UUID.fromString(rs.getString("defender_nation_id"));
                WarType type = WarType.valueOf(rs.getString("type"));
                WarStatus status = WarStatus.valueOf(rs.getString("status"));
                long declaredAt = rs.getLong("declared_at");
                long endsAt = rs.getLong("ends_at");
                String reason = rs.getString("reason");
                War war = new War(id, attacker, defender, type, declaredAt, endsAt, reason);
                war.setStatus(status);
                war.setAttackerKills(rs.getInt("attacker_kills"));
                war.setDefenderKills(rs.getInt("defender_kills"));
                war.setStaffNote(rs.getString("staff_note"));
                String vb = rs.getString("validated_by");
                if (vb != null) war.setValidatedBy(UUID.fromString(vb));
                war.setSurrenderRequested(rs.getBoolean("surrender_requested"));
                war.setSurrenderRequestedAt(rs.getLong("surrender_requested_at"));
                wars.put(id, war);
            }
            plugin.getLogger().info("[Wars] " + wars.size() + " guerres chargées.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Wars] Erreur chargement", e);
        }
    }

    public void saveWar(War war) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = """
            INSERT INTO wars (id, attacker_nation_id, defender_nation_id, type, status,
                declared_at, ends_at, reason, attacker_kills, defender_kills, staff_note,
                validated_by, surrender_requested, surrender_requested_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET
                status=EXCLUDED.status, attacker_kills=EXCLUDED.attacker_kills,
                defender_kills=EXCLUDED.defender_kills, staff_note=EXCLUDED.staff_note,
                validated_by=EXCLUDED.validated_by, ends_at=EXCLUDED.ends_at,
                surrender_requested=EXCLUDED.surrender_requested,
                surrender_requested_at=EXCLUDED.surrender_requested_at
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, war.getId());
            ps.setObject(2, war.getAttackerNationId());
            ps.setObject(3, war.getDefenderNationId());
            ps.setString(4, war.getType().name());
            ps.setString(5, war.getStatus().name());
            ps.setLong(6, war.getDeclaredAt());
            ps.setLong(7, war.getEndsAt());
            ps.setString(8, war.getReason());
            ps.setInt(9, war.getAttackerKills());
            ps.setInt(10, war.getDefenderKills());
            ps.setString(11, war.getStaffNote());
            ps.setObject(12, war.getValidatedBy());
            ps.setBoolean(13, war.hasSurrenderRequest());
            ps.setLong(14, war.getSurrenderRequestedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Wars] Erreur sauvegarde guerre " + war.getId(), e);
        }
    }

    /**
     * Compte le nombre de membres de la nation actuellement connectés.
     */
    public int countOnlineMembers(Nation nation) {
        return (int) nation.getMembers().keySet().stream()
            .filter(id -> Bukkit.getPlayer(id) != null)
            .count();
    }

    public WarDeclarationResult declareWar(Nation attacker, Nation defender, WarType type, String reason) {
        if (attacker.getId().equals(defender.getId())) {
            return WarDeclarationResult.SAME_NATION;
        }
        if (attacker.isAlly(defender.getId())) {
            return WarDeclarationResult.IS_ALLY;
        }

        int minOnline = plugin.getConfig().getInt("wars.min-online-to-declare", 2);
        if (countOnlineMembers(attacker) < minOnline) {
            return WarDeclarationResult.NOT_ENOUGH_ONLINE_ATTACKER;
        }
        if (countOnlineMembers(defender) < minOnline) {
            return WarDeclarationResult.NOT_ENOUGH_ONLINE_DEFENDER;
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
            .anyMatch(w ->
                (w.getAttackerNationId().equals(attacker.getId()) && w.getDefenderNationId().equals(defender.getId()))
                || (w.getAttackerNationId().equals(defender.getId()) && w.getDefenderNationId().equals(attacker.getId()))
            );
        if (alreadyAtWar) {
            return WarDeclarationResult.ALREADY_AT_WAR;
        }

        double cost = type.getCost();
        if (attacker.getBankBalance() >= cost) {
            attacker.withdrawFromBank(cost);
        } else if (plugin.getEconomyManager().has(attacker.getLeaderId(), cost)) {
            plugin.getEconomyManager().withdraw(attacker.getLeaderId(), cost);
        } else {
            return WarDeclarationResult.INSUFFICIENT_FUNDS;
        }

        long now = System.currentTimeMillis();
        long endsAt = now + type.getDurationMillis();
        War war = new War(UUID.randomUUID(), attacker.getId(), defender.getId(), type, now, endsAt, reason);
        wars.put(war.getId(), war);

        long cooldownHours = plugin.getConfigManager().getWarDeclarationCooldownHours();
        warCooldowns.put(attacker.getId(), now + cooldownHours * 3600000L);

        saveWar(war);
        return WarDeclarationResult.PENDING_VALIDATION;
    }

    public boolean validateWar(UUID warId, UUID staffId) {
        War war = wars.get(warId);
        if (war == null || !war.getStatus().isPending()) return false;
        war.setStatus(WarStatus.ACTIVE);
        war.setValidatedBy(staffId);
        if (!war.getType().isAssault()) {
            war.setEndsAt(System.currentTimeMillis() + war.getType().getDurationMillis());
        }
        saveWar(war);
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
        saveWar(war);
        return true;
    }

    /**
     * Demande de reddition par la nation défenseure.
     * Seulement disponible pour une guerre ASSAULT active.
     */
    public SurrenderResult requestSurrender(UUID defenderNationId, UUID warId) {
        War war = wars.get(warId);
        if (war == null || !war.getStatus().isActive()) return SurrenderResult.WAR_NOT_FOUND;
        if (!war.getDefenderNationId().equals(defenderNationId)) return SurrenderResult.NOT_DEFENDER;
        if (!war.getType().isAssault()) return SurrenderResult.NOT_ASSAULT;
        if (war.hasSurrenderRequest()) return SurrenderResult.ALREADY_REQUESTED;
        war.requestSurrender();
        saveWar(war);
        return SurrenderResult.REQUESTED;
    }

    /**
     * Confirme la reddition (le leader défenseur confirme après /war surrender confirm).
     * Termine la guerre ASSAULT avec victoire de l'attaquant.
     */
    public boolean confirmSurrender(UUID defenderNationId, UUID warId) {
        War war = wars.get(warId);
        if (war == null || !war.getStatus().isActive()) return false;
        if (!war.getDefenderNationId().equals(defenderNationId)) return false;
        if (!war.getType().isAssault() || !war.hasSurrenderRequest()) return false;

        endWar(warId, WarStatus.DEFENDER_SURRENDERED, war.getAttackerNationId());
        return true;
    }

    /**
     * Appelé quand la nation défenseure est dissoute en pleine guerre d'assaut.
     */
    public void handleNationDisband(UUID nationId) {
        wars.values().stream()
            .filter(w -> w.getStatus().isActive() && w.isNationInvolved(nationId))
            .forEach(w -> {
                if (w.getType().isAssault() && w.getDefenderNationId().equals(nationId)) {
                    endWar(w.getId(), WarStatus.DEFENDER_SURRENDERED, w.getAttackerNationId());
                } else {
                    WarStatus result = w.getAttackerNationId().equals(nationId)
                        ? WarStatus.DEFENDER_WON : WarStatus.ATTACKER_WON;
                    endWar(w.getId(), result, null);
                }
            });
    }

    public void endWar(UUID warId, WarStatus result, UUID winnerId) {
        War war = wars.get(warId);
        if (war == null) return;
        war.setStatus(result);

        if (result == WarStatus.ATTACKER_WON || result == WarStatus.DEFENDER_SURRENDERED) {
            Nation winner = plugin.getNationManager().getNationById(war.getAttackerNationId());
            if (winner != null) {
                int points = war.getType().isAssault() ? 500 : 100;
                winner.addSeasonPoints(points);
                plugin.getSeasonManager().recordWarWin(war.getAttackerNationId());
            }
        } else if (result == WarStatus.DEFENDER_WON) {
            Nation winner = plugin.getNationManager().getNationById(war.getDefenderNationId());
            if (winner != null) {
                winner.addSeasonPoints(75);
                plugin.getSeasonManager().recordWarWin(war.getDefenderNationId());
            }
        }

        saveWar(war);

        String attName = getWarNationName(war.getAttackerNationId());
        String defName = getWarNationName(war.getDefenderNationId());
        Bukkit.broadcastMessage("§8[§4Guerre§8] §7La guerre §e" + war.getType().getColoredName()
            + " §7entre §c" + attName + " §7et §9" + defName + " §7est terminée — " + result.getDisplayName());
    }

    private String getWarNationName(UUID nationId) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        return n != null ? n.getName() : "Inconnue";
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
        saveWar(activeWar);
    }

    public War getActiveWarBetween(UUID nationA, UUID nationB) {
        return wars.values().stream()
            .filter(w -> w.getStatus().isActive())
            .filter(w ->
                (w.getAttackerNationId().equals(nationA) && w.getDefenderNationId().equals(nationB))
                || (w.getAttackerNationId().equals(nationB) && w.getDefenderNationId().equals(nationA))
            )
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
            .sorted(Comparator.comparingLong(War::getDeclaredAt))
            .collect(Collectors.toList());
    }

    public List<War> getAllActiveWars() {
        return wars.values().stream()
            .filter(w -> w.getStatus().isActive())
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

    public void addWarFallback(War war) {
        wars.put(war.getId(), war);
    }

    public void shutdown() {
        if (warCheckTask != null) warCheckTask.cancel();
    }

    /**
     * Vérifie si le PvP est autorisé entre deux joueurs dans un chunk donné.
     * Règle : autorisé seulement si les deux joueurs sont dans des nations en guerre.
     */
    public boolean isPvpAllowedInClaim(UUID playerA, UUID playerB) {
        Nation nationA = plugin.getNationManager().getPlayerNation(playerA);
        Nation nationB = plugin.getNationManager().getPlayerNation(playerB);
        if (nationA == null || nationB == null) return false;
        if (nationA.getId().equals(nationB.getId())) return false;
        return areNationsAtWar(nationA.getId(), nationB.getId());
    }

    public enum WarDeclarationResult {
        PENDING_VALIDATION,
        SAME_NATION,
        IS_ALLY,
        ON_COOLDOWN,
        MAX_WARS_REACHED,
        ALREADY_AT_WAR,
        INSUFFICIENT_FUNDS,
        NOT_ENOUGH_ONLINE_ATTACKER,
        NOT_ENOUGH_ONLINE_DEFENDER
    }

    public enum SurrenderResult {
        REQUESTED,
        WAR_NOT_FOUND,
        NOT_DEFENDER,
        NOT_ASSAULT,
        ALREADY_REQUESTED
    }
}
