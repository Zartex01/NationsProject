package fr.nations.nation;

import fr.nations.NationsPlugin;
import org.bukkit.entity.Player;

import java.util.*;
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

        plugin.getDataManager().saveNations();
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
        plugin.getDataManager().saveNations();
    }

    public boolean addPlayerToNation(UUID nationId, UUID playerId, String playerName) {
        Nation nation = nations.get(nationId);
        if (nation == null) return false;
        if (hasNation(playerId)) return false;

        NationMember member = new NationMember(playerId, playerName, NationRole.RECRUIT);
        nation.addMember(member);
        playerNationMap.put(playerId, nationId);
        plugin.getDataManager().saveNations();
        return true;
    }

    public boolean removePlayerFromNation(UUID playerId) {
        UUID nationId = playerNationMap.get(playerId);
        if (nationId == null) return false;

        Nation nation = nations.get(nationId);
        if (nation == null) return false;

        nation.removeMember(playerId);
        playerNationMap.remove(playerId);
        plugin.getDataManager().saveNations();
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
        plugin.getDataManager().saveNations();
        return true;
    }

    public boolean addNationToCoalition(UUID coalitionId, Nation nation) {
        Coalition coalition = coalitions.get(coalitionId);
        if (coalition == null) return false;
        if (nation.getCoalitionId() != null) return false;

        coalition.addNation(nation.getId());
        nation.setCoalitionId(coalitionId);
        plugin.getDataManager().saveNations();
        return true;
    }

    public boolean removeNationFromCoalition(Nation nation) {
        if (nation.getCoalitionId() == null) return false;

        Coalition coalition = coalitions.get(nation.getCoalitionId());
        if (coalition == null) return false;

        coalition.removeNation(nation.getId());
        nation.setCoalitionId(null);

        if (coalition.getNationCount() == 0) {
            coalitions.remove(coalition.getId());
        } else if (coalition.getLeaderNationId().equals(nation.getId())) {
            coalition.setLeaderNationId(coalition.getMemberNations().iterator().next());
        }

        plugin.getDataManager().saveNations();
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
        plugin.getDataManager().saveNations();
        return true;
    }

    public boolean acceptAlliance(Nation acceptor, Nation requester) {
        if (!acceptor.hasAllyRequest(requester.getId())) return false;
        acceptor.removeAllyRequest(requester.getId());
        acceptor.addAlly(requester.getId());
        requester.addAlly(acceptor.getId());
        plugin.getDataManager().saveNations();
        return true;
    }

    public void breakAlliance(Nation a, Nation b) {
        a.removeAlly(b.getId());
        b.removeAlly(a.getId());
        plugin.getDataManager().saveNations();
    }

    public NationRole getPlayerRole(UUID playerId) {
        Nation nation = getPlayerNation(playerId);
        if (nation == null) return null;
        NationMember member = nation.getMember(playerId);
        return member != null ? member.getRole() : null;
    }
}
