package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.territory.ClaimedChunk;
import fr.nations.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class BlockProtectionListener implements Listener {

    private final NationsPlugin plugin;

    public BlockProtectionListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("nations.bypass")) return;

        Chunk chunk = event.getBlock().getChunk();
        ClaimedChunk claimed = plugin.getTerritoryManager().getClaimedChunk(chunk);
        if (claimed == null) return;

        if (!canPlayerBuildInNation(player, claimed.getNationId())) {
            event.setCancelled(true);
            MessageUtil.sendError(player, "Ce territoire appartient à §6" + getNationName(claimed.getNationId()) + "§c.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("nations.bypass")) return;

        Chunk chunk = event.getBlock().getChunk();
        ClaimedChunk claimed = plugin.getTerritoryManager().getClaimedChunk(chunk);
        if (claimed == null) return;

        if (!canPlayerBuildInNation(player, claimed.getNationId())) {
            event.setCancelled(true);
            MessageUtil.sendError(player, "Ce territoire appartient à §6" + getNationName(claimed.getNationId()) + "§c.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (player.hasPermission("nations.bypass")) return;

        Chunk chunk = event.getClickedBlock().getChunk();
        ClaimedChunk claimed = plugin.getTerritoryManager().getClaimedChunk(chunk);
        if (claimed == null) return;

        if (!canPlayerInteractInNation(player, claimed.getNationId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Protection PvP dans les claims.
     *
     * Règles :
     * - Dans un territoire claimé, le PvP est INTERDIT par défaut.
     * - Il est autorisé UNIQUEMENT si les deux joueurs appartiennent à des nations
     *   en guerre active l'une contre l'autre.
     * - Hors claim → pas d'intervention (PvP normal de Minecraft).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (attacker.hasPermission("nations.bypass")) return;

        Chunk chunk = victim.getLocation().getChunk();
        ClaimedChunk claimed = plugin.getTerritoryManager().getClaimedChunk(chunk);
        if (claimed == null) return;

        if (plugin.getWarManager().isPvpAllowedInClaim(attacker.getUniqueId(), victim.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        attacker.sendMessage("§c⚔ Le PvP est interdit dans les territoires claimés.");
        attacker.sendMessage("§7Déclarez une guerre pour pouvoir vous battre ici.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Chunk chunk = block.getChunk();
            ClaimedChunk claimed = plugin.getTerritoryManager().getClaimedChunk(chunk);
            return claimed != null;
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Chunk chunk = block.getChunk();
            ClaimedChunk claimed = plugin.getTerritoryManager().getClaimedChunk(chunk);
            return claimed != null;
        });
    }

    private boolean canPlayerBuildInNation(Player player, UUID nationId) {
        // Si le joueur est proprio d'un logement qui contient le bloc, il peut tout faire
        if (plugin.getHousingManager() != null) {
            fr.nations.housing.Housing housing = plugin.getHousingManager().getHousingContaining(
                player.getWorld().getName(),
                (int) player.getLocation().getX(),
                (int) player.getLocation().getY(),
                (int) player.getLocation().getZ());
            if (housing != null && player.getUniqueId().equals(housing.getOwnerId())) {
                return true;
            }
        }

        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return true;
        if (!nation.isMember(player.getUniqueId())) return false;
        NationMember member = nation.getMember(player.getUniqueId());
        return member != null && member.canBuild();
    }

    private boolean canPlayerInteractInNation(Player player, UUID nationId) {
        // Si le joueur est proprio d'un logement dans ce chunk, il peut interagir
        if (plugin.getHousingManager() != null) {
            fr.nations.housing.Housing housing = plugin.getHousingManager().getHousingContaining(
                player.getWorld().getName(),
                (int) player.getLocation().getX(),
                (int) player.getLocation().getY(),
                (int) player.getLocation().getZ());
            if (housing != null && player.getUniqueId().equals(housing.getOwnerId())) {
                return true;
            }
        }

        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return true;
        return nation.isMember(player.getUniqueId());
    }

    private String getNationName(UUID nationId) {
        Nation nation = plugin.getNationManager().getNationById(nationId);
        return nation != null ? nation.getName() : "Inconnu";
    }
}
