package fr.nations.territory;

import fr.nations.NationsPlugin;
import fr.nations.grade.PlayerGrade;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TerritoryManager {

    private final NationsPlugin plugin;
    private final Map<String, ClaimedChunk> claimedChunks;

    public TerritoryManager(NationsPlugin plugin) {
        this.plugin = plugin;
        this.claimedChunks = new HashMap<>();
    }

    public void addClaim(ClaimedChunk chunk) {
        claimedChunks.put(chunk.getKey(), chunk);
        PlayerGrade grade = plugin.getGradeManager().getPlayerGrade(chunk.getClaimedBy());
        if (grade != null) {
            grade.incrementClaimCount();
        }
    }

    public ClaimResult claimChunk(Player player, Chunk chunk) {
        String key = buildKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());

        if (claimedChunks.containsKey(key)) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        if (!plugin.getNationManager().hasNation(player.getUniqueId())) {
            return ClaimResult.NO_NATION;
        }

        PlayerGrade grade = plugin.getGradeManager().getOrCreatePlayerGrade(player.getUniqueId(), player.getName());
        int maxClaims = plugin.getConfigManager().getMaxClaimsForGrade(grade.getGradeName());

        if (grade.getClaimCount() >= maxClaims) {
            return ClaimResult.MAX_CLAIMS_REACHED;
        }

        double cost = plugin.getConfigManager().getClaimPrice();
        if (!plugin.getEconomyManager().has(player.getUniqueId(), cost)) {
            return ClaimResult.INSUFFICIENT_FUNDS;
        }

        plugin.getEconomyManager().withdraw(player.getUniqueId(), cost);

        UUID nationId = plugin.getNationManager().getPlayerNation(player.getUniqueId()).getId();
        ClaimedChunk claimed = new ClaimedChunk(
            chunk.getWorld().getName(),
            chunk.getX(),
            chunk.getZ(),
            nationId,
            player.getUniqueId(),
            System.currentTimeMillis()
        );

        claimedChunks.put(key, claimed);
        grade.incrementClaimCount();

        plugin.getNationManager().getPlayerNation(player.getUniqueId()).addSeasonPoints(5);
        plugin.getSeasonManager().addPlayerStat(player.getUniqueId(), "claims", 1);
        plugin.getGradeManager().addXp(player.getUniqueId(), plugin.getConfigManager().getXpPerClaim());

        plugin.getDataManager().saveClaims();
        plugin.getDataManager().savePlayers();
        return ClaimResult.SUCCESS;
    }

    public boolean unclaimChunk(Player player, Chunk chunk) {
        String key = buildKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        ClaimedChunk claimed = claimedChunks.get(key);
        if (claimed == null) return false;

        UUID playerNationId = plugin.getNationManager().getPlayerNation(player.getUniqueId()) != null
            ? plugin.getNationManager().getPlayerNation(player.getUniqueId()).getId()
            : null;

        if (!claimed.getNationId().equals(playerNationId)) return false;

        claimedChunks.remove(key);
        PlayerGrade grade = plugin.getGradeManager().getPlayerGrade(player.getUniqueId());
        if (grade != null) grade.decrementClaimCount();

        plugin.getDataManager().saveClaims();
        return true;
    }

    public void unclaimAllForNation(UUID nationId) {
        List<String> toRemove = claimedChunks.entrySet().stream()
            .filter(e -> e.getValue().getNationId().equals(nationId))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        for (String key : toRemove) {
            ClaimedChunk chunk = claimedChunks.remove(key);
            PlayerGrade grade = plugin.getGradeManager().getPlayerGrade(chunk.getClaimedBy());
            if (grade != null) grade.decrementClaimCount();
        }

        plugin.getDataManager().saveClaims();
    }

    public ClaimedChunk getClaimedChunk(Chunk chunk) {
        return claimedChunks.get(buildKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
    }

    public ClaimedChunk getClaimedChunkByKey(String key) {
        return claimedChunks.get(key);
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return claimedChunks.containsKey(buildKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
    }

    public boolean isChunkOwnedByNation(Chunk chunk, UUID nationId) {
        ClaimedChunk claimed = getClaimedChunk(chunk);
        return claimed != null && claimed.getNationId().equals(nationId);
    }

    public List<ClaimedChunk> getClaimsForNation(UUID nationId) {
        return claimedChunks.values().stream()
            .filter(c -> c.getNationId().equals(nationId))
            .collect(Collectors.toList());
    }

    public int getClaimCountForNation(UUID nationId) {
        return getClaimsForNation(nationId).size();
    }

    public Collection<ClaimedChunk> getAllClaims() {
        return Collections.unmodifiableCollection(claimedChunks.values());
    }

    private String buildKey(String world, int x, int z) {
        return world + "_" + x + "_" + z;
    }

    public enum ClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        NO_NATION,
        MAX_CLAIMS_REACHED,
        INSUFFICIENT_FUNDS
    }
}
