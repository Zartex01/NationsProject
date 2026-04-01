package fr.nations.housing;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HousingListener implements Listener {

    private final NationsPlugin plugin;

    // Sélections en cours (pos1 et pos2 par joueur)
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    // Joueurs en attente de placement de panneau
    private final Map<UUID, UUID> pendingSignPlacement = new HashMap<>(); // playerId → housingId

    private static final int MAX_SIZE = 30; // taille max par dimension
    private static final Set<Material> AXE_TYPES = Set.of(
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
        Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    );
    private static final Set<Material> SIGN_TYPES = new java.util.HashSet<>();

    static {
        for (Material m : Material.values()) {
            if (m.name().endsWith("_SIGN") || m.name().endsWith("_WALL_SIGN") || m.name().endsWith("_HANGING_SIGN")) {
                SIGN_TYPES.add(m);
            }
        }
    }

    public HousingListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Material itemInHand = player.getInventory().getItemInMainHand().getType();

        // ── Clic droit avec hache ──────────────────────────────────────────────
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && AXE_TYPES.contains(itemInHand)) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            // Vérifier que le joueur peut créer un logement (leader/co-leader ou MANAGE_CLAIMS)
            if (!canManageHousing(player)) {
                return; // Pas de message, la hache est utilisée normalement
            }

            event.setCancelled(true);

            if (!pos1.containsKey(playerId)) {
                // Première sélection
                pos1.put(playerId, block.getLocation());
                pos2.remove(playerId);
                player.sendMessage("§a§l[Logement] §aPosition §61 §asélectionnée: §f"
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
                player.sendMessage("§7Faites un second clic droit pour la position 2.");
            } else {
                // Deuxième sélection
                Location l1 = pos1.get(playerId);
                Location l2 = block.getLocation();

                if (!l1.getWorld().getName().equals(l2.getWorld().getName())) {
                    MessageUtil.sendError(player, "Les deux blocs doivent être dans le même monde.");
                    return;
                }

                int minX = Math.min(l1.getBlockX(), l2.getBlockX());
                int minY = Math.min(l1.getBlockY(), l2.getBlockY());
                int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ());
                int maxX = Math.max(l1.getBlockX(), l2.getBlockX());
                int maxY = Math.max(l1.getBlockY(), l2.getBlockY());
                int maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());

                int w = maxX - minX + 1, h = maxY - minY + 1, d = maxZ - minZ + 1;

                if (w > MAX_SIZE || h > MAX_SIZE || d > MAX_SIZE) {
                    MessageUtil.sendError(player, "La zone est trop grande (max " + MAX_SIZE + " blocs par dimension). Taille actuelle: " + w + "×" + h + "×" + d);
                    pos1.remove(playerId);
                    return;
                }

                pos2.put(playerId, l2);
                player.sendMessage("§a§l[Logement] §aPosition §62 §asélectionnée: §f"
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
                player.sendMessage("§7Zone: §e" + w + "×" + h + "×" + d + " §7blocs.");
                player.sendMessage("§eTapez le §6nom §edu logement dans le tchat §8(ou §cannuler§8):");

                // Activer la saisie du nom
                fr.nations.gui.GuiManager.setPendingAction(playerId,
                    "housing_name:" + minX + "," + minY + "," + minZ + "," + maxX + "," + maxY + "," + maxZ + "," + l1.getWorld().getName());
                pos1.remove(playerId);
                pos2.remove(playerId);
            }
            return;
        }

        // ── Clic droit sur un panneau → acheter le logement ───────────────────
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;
            if (!SIGN_TYPES.contains(block.getType())) return;

            Housing housing = plugin.getHousingManager().getHousingBySign(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
            if (housing == null) return;

            event.setCancelled(true);
            handleSignClick(player, housing, block);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        UUID housingId = pendingSignPlacement.get(playerId);
        if (housingId == null) return;

        pendingSignPlacement.remove(playerId);
        Housing housing = plugin.getHousingManager().getHousingById(housingId);
        if (housing == null) return;

        Block block = event.getBlock();

        // Vérifier que le panneau est DANS ou ADJACENT à la zone
        if (!isNearHousing(housing, block)) {
            MessageUtil.sendError(player, "Placez le panneau dans ou à côté de la zone du logement.");
            pendingSignPlacement.put(playerId, housingId); // Remettre en attente
            return;
        }

        // Enregistrer le panneau
        housing.setSign(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        plugin.getHousingManager().saveToDatabase(housing);

        // Formater le panneau
        event.setLine(0, "§8[§6Logement§8]");
        event.setLine(1, "§e" + housing.getName());
        event.setLine(2, "§aPrix: §f" + (int) housing.getPrice() + " §acoins");
        event.setLine(3, "§7Cliquez pour louer");

        MessageUtil.sendSuccess(player, "Logement §6" + housing.getName() + " §acréé avec succès! Les joueurs peuvent cliquer sur ce panneau pour louer.");
    }

    // ─── Achat d'un logement ─────────────────────────────────────────────────────

    private void handleSignClick(Player player, Housing housing, Block signBlock) {
        if (housing.isOwned()) {
            player.sendMessage("§8[§6Logement§8] §eCe logement appartient à: §f" + housing.getOwnerName());
            if (player.getUniqueId().equals(housing.getOwnerId())) {
                player.sendMessage("§7C'est votre logement.");
            }
            return;
        }

        // Vérifier que le joueur est dans la nation
        Nation nation = plugin.getNationManager().getNationById(housing.getNationId());
        if (nation == null) {
            MessageUtil.sendError(player, "Nation introuvable.");
            return;
        }
        if (!nation.isMember(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous devez être membre de §6" + nation.getName() + " §cpour louer ce logement.");
            return;
        }

        // Vérifier que le joueur n'a pas déjà un logement dans cette nation
        boolean alreadyHas = plugin.getHousingManager().getHousingsForNation(nation.getId()).stream()
            .anyMatch(h -> player.getUniqueId().equals(h.getOwnerId()));
        if (alreadyHas) {
            MessageUtil.sendError(player, "Vous avez déjà un logement dans cette nation.");
            return;
        }

        // Vérifier les fonds
        if (!plugin.getEconomyManager().has(player.getUniqueId(), housing.getPrice())) {
            MessageUtil.sendError(player, "Fonds insuffisants. Prix: §6" + (int) housing.getPrice() + " coins§c.");
            return;
        }

        // Achat
        plugin.getEconomyManager().withdraw(player.getUniqueId(), housing.getPrice());
        nation.depositToBank(housing.getPrice());
        plugin.getNationManager().saveNationToDatabase(nation);

        housing.setOwnerId(player.getUniqueId());
        housing.setOwnerName(player.getName());
        plugin.getHousingManager().saveToDatabase(housing);

        // Mettre à jour le panneau
        if (signBlock.getState() instanceof Sign sign) {
            sign.setLine(0, "§8[§6Logement§8]");
            sign.setLine(1, "§e" + housing.getName());
            sign.setLine(2, "§2§lOccupé");
            sign.setLine(3, "§7" + player.getName());
            sign.update();
        }

        player.sendMessage("§a§l✔ §aLogement §6" + housing.getName() + " §aloué pour §6" + (int) housing.getPrice() + " coins§a!");
        player.sendMessage("§7Vous avez maintenant accès complet à cette chambre.");

        // Notifier les membres en ligne
        for (UUID memberId : nation.getMembers().keySet()) {
            org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(memberId);
            if (online != null && !online.getUniqueId().equals(player.getUniqueId())) {
                online.sendMessage("§8[§6Nation§8] §a" + player.getName() + " §aa loué le logement §6" + housing.getName() + "§a.");
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    public void setPendingSign(UUID playerId, UUID housingId) {
        pendingSignPlacement.put(playerId, housingId);
    }

    private boolean canManageHousing(Player player) {
        if (player.hasPermission("nations.admin")) return true;
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) return false;
        NationMember member = nation.getMember(player.getUniqueId());
        if (member == null) return false;
        return member.getRole() == NationRole.LEADER
            || member.getRole() == NationRole.CO_LEADER
            || plugin.getCustomRoleManager().hasPermission(player.getUniqueId(), fr.nations.role.RolePermission.MANAGE_CLAIMS);
    }

    private boolean isNearHousing(Housing h, Block block) {
        String world = block.getWorld().getName();
        int x = block.getX(), y = block.getY(), z = block.getZ();
        // Dans la zone ou au maximum 2 blocs à l'extérieur
        return world.equals(h.getWorld())
            && x >= h.getMinX() - 2 && x <= h.getMaxX() + 2
            && y >= h.getMinY() - 2 && y <= h.getMaxY() + 2
            && z >= h.getMinZ() - 2 && z <= h.getMaxZ() + 2;
    }
}
