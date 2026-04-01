package fr.nations.nation;

import fr.nations.NationsPlugin;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class NationManager {

    private final NationsPlugin plugin;
    private final Map<UUID, Nation> nations;
    private final Map<UUID, Coalition> coalitions;
    private final Map<UUID, UUID> playerNationMap;

    public NationManager(NationsPlugin plugin) {
        this.plugin = plugin;
        this.nations = new HashMap<>();
        this.coalitions = new HashMap<>();
        this.playerNationMap = new HashMap<>();
    }

    public void addNation(Nation nation) {
        nations.put(nation.getId(), nation);
        for (UUID memberId : nation.getMembers().keySet()) {
            playerNationMap.put(memberId, nation.getId());
        }
    }

    public Nation createNation(Player leader, String name) {
        if (hasNation(leader.getUniqueId())) return null;
        if (getNationByName(name) != null) return null;

        Nation nation = new Nation(UUID.randomUUID(), name, leader.getUniqueId(), System.currentTimeMillis());
        NationMember leaderMember = new NationMember(leader.getUniqueId(), leader.getName(), NationRole.LEADER);
        nation.addMember(leaderMember);
        nations.put(nation.getId(), nation);
        playerNationMap.put(leader.getUniqueId(), nation.getId());

        saveNationToDatabase(nation);
        saveMemberToDatabase(nation.getId(), leaderMember);
        return nation;
    }

    public void disbandNation(UUID nationId) {
        Nation nation = nations.get(nationId);
        if (nation == null) return;

        for (UUID memberId : nation.getMembers().keySet()) {
            playerNationMap.remove(memberId);
        }

        plugin.getTerritoryManager().unclaimAllForNation(nationId);

        for (Nation ally : getAllNations()) {
            ally.removeAlly(nationId);
        }

        if (nation.getCoalitionId() != null) {
            Coalition coalition = coalitions.get(nation.getCoalitionId());
            if (coalition != null) {
                coalition.removeNation(nationId);
                if (coalition.getNationCount() == 0) {
                    coalitions.remove(coalition.getId());
                } else if (coalition.getLeaderNationId().equals(nationId)) {
                    UUID newLeader = coalition.getMemberNations().iterator().next();
                    coalition.setLeaderNationId(newLeader);
                }
            }
        }

        nations.remove(nationId);
        deleteNationFromDatabase(nationId);
    }

    public boolean addPlayerToNation(UUID nationId, UUID playerId, String playerName) {
        Nation nation = nations.get(nationId);
        if (nation == null) return false;
        if (hasNation(playerId)) return false;

        NationMember member = new NationMember(playerId, playerName, NationRole.RECRUIT);
        nation.addMember(member);
        playerNationMap.put(playerId, nationId);
        saveNationToDatabase(nation);
        saveMemberToDatabase(nationId, member);
        return true;
    }

    public boolean removePlayerFromNation(UUID playerId) {
        UUID nationId = playerNationMap.get(playerId);
        if (nationId == null) return false;

        Nation nation = nations.get(nationId);
        if (nation == null) return false;

        nation.removeMember(playerId);
        playerNationMap.remove(playerId);
        deleteMemberFromDatabase(playerId);
        return true;
    }

    public boolean hasNation(UUID playerId) {
        return playerNationMap.containsKey(playerId);
    }

    public Nation getPlayerNation(UUID playerId) {
        UUID nationId = playerNationMap.get(playerId);
        return nationId != null ? nations.get(nationId) : null;
    }

    public Nation getNationById(UUID id) {
        return nations.get(id);
    }

    public Nation getNationByName(String name) {
        return nations.values().stream()
            .filter(n -> n.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public Collection<Nation> getAllNations() {
        return Collections.unmodifiableCollection(nations.values());
    }

    public List<Nation> getNationsSortedByPoints() {
        return nations.values().stream()
            .sorted(Comparator.comparingInt(Nation::getSeasonPoints).reversed())
            .collect(Collectors.toList());
    }

    public boolean createCoalition(Nation leaderNation, String coalitionName) {
        if (leaderNation.getCoalitionId() != null) return false;

        Coalition coalition = new Coalition(UUID.randomUUID(), coalitionName, leaderNation.getId());
        coalitions.put(coalition.getId(), coalition);
        leaderNation.setCoalitionId(coalition.getId());
        saveCoalitionToDatabase(coalition);
        saveCoalitionMemberToDatabase(coalition.getId(), leaderNation.getId());
        saveNationToDatabase(leaderNation);
        return true;
    }

    public boolean addNationToCoalition(UUID coalitionId, Nation nation) {
        Coalition coalition = coalitions.get(coalitionId);
        if (coalition == null) return false;
        if (nation.getCoalitionId() != null) return false;

        coalition.addNation(nation.getId());
        nation.setCoalitionId(coalitionId);
        saveCoalitionMemberToDatabase(coalitionId, nation.getId());
        saveNationToDatabase(nation);
        return true;
    }

    public boolean removeNationFromCoalition(Nation nation) {
        if (nation.getCoalitionId() == null) return false;

        Coalition coalition = coalitions.get(nation.getCoalitionId());
        if (coalition == null) return false;

        UUID coalitionId = coalition.getId();
        coalition.removeNation(nation.getId());
        nation.setCoalitionId(null);

        deleteCoalitionMemberFromDatabase(coalitionId, nation.getId());
        saveNationToDatabase(nation);

        if (coalition.getNationCount() == 0) {
            coalitions.remove(coalitionId);
            deleteCoalitionFromDatabase(coalitionId);
        } else if (coalition.getLeaderNationId().equals(nation.getId())) {
            coalition.setLeaderNationId(coalition.getMemberNations().iterator().next());
            saveCoalitionToDatabase(coalition);
        }

        return true;
    }

    public Coalition getCoalition(UUID coalitionId) {
        return coalitions.get(coalitionId);
    }

    public Coalition getCoalitionByName(String name) {
        return coalitions.values().stream()
            .filter(c -> c.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public Collection<Coalition> getAllCoalitions() {
        return Collections.unmodifiableCollection(coalitions.values());
    }

    public void addCoalition(Coalition coalition) {
        coalitions.put(coalition.getId(), coalition);
    }

    public boolean requestAlliance(Nation requester, Nation target) {
        if (requester.isAlly(target.getId())) return false;
        target.addAllyRequest(requester.getId());
        return true;
    }

    public boolean acceptAlliance(Nation acceptor, Nation requester) {
        if (!acceptor.hasAllyRequest(requester.getId())) return false;
        acceptor.removeAllyRequest(requester.getId());
        acceptor.addAlly(requester.getId());
        requester.addAlly(acceptor.getId());
        saveAllianceToDatabase(acceptor.getId(), requester.getId());
        return true;
    }

    public void breakAlliance(Nation a, Nation b) {
        a.removeAlly(b.getId());
        b.removeAlly(a.getId());
        deleteAllianceFromDatabase(a.getId(), b.getId());
    }

    public NationRole getPlayerRole(UUID playerId) {
        Nation nation = getPlayerNation(playerId);
        if (nation == null) return null;
        NationMember member = nation.getMember(playerId);
        return member != null ? member.getRole() : null;
    }

    public void loadFromDatabase() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            loadNations(conn);
            loadMembers(conn);
            loadAllies(conn);
            loadCoalitions(conn);
            plugin.getLogger().info("[Nations] " + nations.size() + " nations et " + coalitions.size() + " coalitions chargées.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur chargement DB", e);
        }
    }

    private void loadNations(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, description, leader_id, bank_balance, season_points, level, xp, created_at FROM nations");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                String name = rs.getString("name");
                UUID leaderId = UUID.fromString(rs.getString("leader_id"));
                long createdAt = rs.getLong("created_at");
                Nation nation = new Nation(id, name, leaderId, createdAt);
                nation.setDescription(rs.getString("description"));
                nation.setSeasonPoints(rs.getInt("season_points"));
                nation.setLevel(rs.getInt("level"));
                nation.setXp(rs.getDouble("xp"));
                nation.depositToBank(rs.getDouble("bank_balance"));
                nations.put(id, nation);
            }
        }
    }

    private void loadMembers(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT player_id, nation_id, role_name, player_name, joined_at FROM nation_members");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("player_id"));
                UUID nationId = UUID.fromString(rs.getString("nation_id"));
                String roleName = rs.getString("role_name");
                String playerName = rs.getString("player_name");
                Nation nation = nations.get(nationId);
                if (nation == null) continue;
                NationRole role;
                try { role = NationRole.valueOf(roleName); }
                catch (IllegalArgumentException e) { role = NationRole.MEMBER; }
                NationMember member = new NationMember(playerId, playerName, role);
                member.setJoinedAt(rs.getLong("joined_at"));
                nation.addMember(member);
                playerNationMap.put(playerId, nationId);
            }
        }
    }

    private void loadAllies(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT nation_a, nation_b FROM nation_allies");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID a = UUID.fromString(rs.getString("nation_a"));
                UUID b = UUID.fromString(rs.getString("nation_b"));
                Nation nationA = nations.get(a);
                if (nationA != null) nationA.addAlly(b);
            }
        }
    }

    private void loadCoalitions(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, leader_nation_id, created_at FROM coalitions");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                String name = rs.getString("name");
                UUID leaderId = UUID.fromString(rs.getString("leader_nation_id"));
                Coalition coalition = new Coalition(id, name, leaderId);
                coalitions.put(id, coalition);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT coalition_id, nation_id FROM coalition_members");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID cId = UUID.fromString(rs.getString("coalition_id"));
                UUID nId = UUID.fromString(rs.getString("nation_id"));
                Coalition coalition = coalitions.get(cId);
                Nation nation = nations.get(nId);
                if (coalition != null) coalition.addNation(nId);
                if (nation != null) nation.setCoalitionId(cId);
            }
        }
    }

    public void saveNationToDatabase(Nation nation) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = """
            INSERT INTO nations (id, name, description, leader_id, bank_balance, season_points, level, xp, created_at)
            VALUES (?,?,?,?,?,?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET
                name=excluded.name, description=excluded.description, leader_id=excluded.leader_id,
                bank_balance=excluded.bank_balance, season_points=excluded.season_points,
                level=excluded.level, xp=excluded.xp
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nation.getId().toString());
            ps.setString(2, nation.getName());
            ps.setString(3, nation.getDescription());
            ps.setString(4, nation.getLeaderId().toString());
            ps.setDouble(5, nation.getBankBalance());
            ps.setInt(6, nation.getSeasonPoints());
            ps.setInt(7, nation.getLevel());
            ps.setDouble(8, nation.getXp());
            ps.setLong(9, nation.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur sauvegarde nation", e);
        }
    }

    public void deleteNationFromDatabase(UUID nationId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM nations WHERE id=?")) {
            ps.setString(1, nationId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur suppression nation", e);
        }
    }

    public void saveMemberToDatabase(UUID nationId, NationMember member) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = """
            INSERT INTO nation_members (player_id, nation_id, role_name, player_name, joined_at) VALUES (?,?,?,?,?)
            ON CONFLICT (player_id) DO UPDATE SET
                nation_id=excluded.nation_id, role_name=excluded.role_name, player_name=excluded.player_name
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getPlayerId().toString());
            ps.setString(2, nationId.toString());
            ps.setString(3, member.getRole().name());
            ps.setString(4, member.getPlayerName());
            ps.setLong(5, member.getJoinedAt() > 0 ? member.getJoinedAt() : System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur sauvegarde membre", e);
        }
    }

    public void deleteMemberFromDatabase(UUID playerId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM nation_members WHERE player_id=?")) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur suppression membre", e);
        }
    }

    public void saveAllianceToDatabase(UUID a, UUID b) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = "INSERT OR IGNORE INTO nation_allies (nation_a, nation_b) VALUES (?,?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.toString());
            ps.setString(2, b.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur sauvegarde alliance", e);
        }
    }

    public void deleteAllianceFromDatabase(UUID a, UUID b) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = "DELETE FROM nation_allies WHERE (nation_a=? AND nation_b=?) OR (nation_a=? AND nation_b=?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.toString());
            ps.setString(2, b.toString());
            ps.setString(3, b.toString());
            ps.setString(4, a.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur suppression alliance", e);
        }
    }

    public void saveCoalitionToDatabase(Coalition coalition) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = """
            INSERT INTO coalitions (id, name, leader_nation_id, created_at) VALUES (?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET name=excluded.name, leader_nation_id=excluded.leader_nation_id
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coalition.getId().toString());
            ps.setString(2, coalition.getName());
            ps.setString(3, coalition.getLeaderNationId().toString());
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur sauvegarde coalition", e);
        }
    }

    public void deleteCoalitionFromDatabase(UUID coalitionId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM coalitions WHERE id=?")) {
            ps.setString(1, coalitionId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur suppression coalition", e);
        }
    }

    public void saveCoalitionMemberToDatabase(UUID coalitionId, UUID nationId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = "INSERT OR IGNORE INTO coalition_members (coalition_id, nation_id) VALUES (?,?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coalitionId.toString());
            ps.setString(2, nationId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur sauvegarde membre coalition", e);
        }
    }

    public void deleteCoalitionMemberFromDatabase(UUID coalitionId, UUID nationId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = "DELETE FROM coalition_members WHERE coalition_id=? AND nation_id=?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coalitionId.toString());
            ps.setString(2, nationId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Nations] Erreur suppression membre coalition", e);
        }
    }

    public void saveAllToDatabase() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        for (Nation nation : nations.values()) {
            saveNationToDatabase(nation);
            for (Map.Entry<UUID, NationMember> entry : nation.getMembers().entrySet()) {
                saveMemberToDatabase(nation.getId(), entry.getValue());
            }
            for (UUID allyId : nation.getAllies()) {
                saveAllianceToDatabase(nation.getId(), allyId);
            }
        }
        for (Coalition coalition : coalitions.values()) {
            saveCoalitionToDatabase(coalition);
            for (UUID nationId : coalition.getMemberNations()) {
                saveCoalitionMemberToDatabase(coalition.getId(), nationId);
            }
        }
    }
}
