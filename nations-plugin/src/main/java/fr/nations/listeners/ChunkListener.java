package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.territory.ClaimedChunk;
import fr.nations.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChunkListener implements Listener {

    private final NationsPlugin plugin;
    private final Map<UUID, String> lastChunkKey;

    public ChunkListener(NationsPlugin plugin) {
        this.plugin = plugin;
        this.lastChunkKey = new HashMap<>();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;

        Player player = event.getPlayer();
        Chunk toChunk = event.getTo().getChunk();
        String newKey = toChunk.getWorld().getName() + "_" + toChunk.getX() + "_" + toChunk.getZ();
        String prevKey = lastChunkKey.get(player.getUniqueId());

        if (newKey.equals(prevKey)) return;

        lastChunkKey.put(player.getUniqueId(), newKey);
        ClaimedChunk claimed = plugin.getTerritoryManager().getClaimedChunkByKey(newKey);

        if (claimed != null) {
            Nation nation = plugin.getNationManager().getNationById(claimed.getNationId());
            String nationName = nation != null ? nation.getName() : "Inconnu";
            boolean isOwnNation = nation != null && nation.isMember(player.getUniqueId());
            boolean isAlly = false;

            if (!isOwnNation && nation != null) {
                Nation playerNation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
                isAlly = playerNation != null && nation.isAlly(playerNation.getId());
            }

            if (isOwnNation) {
                player.sendActionBar(MessageUtil.colorize("§a§lTerritory de §6" + nationName));
            } else if (isAlly) {
                player.sendActionBar(MessageUtil.colorize("§e§lAllié: §6" + nationName));
            } else {
                player.sendActionBar(MessageUtil.colorize("§c§lTerritory ennemi: §6" + nationName));
            }
        } else {
            player.sendActionBar(MessageUtil.colorize("§7Zone neutre"));
        }
    }
}
