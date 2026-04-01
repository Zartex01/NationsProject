package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationRole;
import fr.nations.role.CustomRole;
import fr.nations.util.GuiUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI qui liste tous les rôles : rôles de base + rôles personnalisés.
 * Accessible via /nation role (sans argument).
 */
public class NationRoleListGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;

    private static final NationRole[] BASE_ROLES = NationRole.values();

    public NationRoleListGui(NationsPlugin plugin, Player player, Nation nation) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
    }

    public Inventory build() {
        List<CustomRole> customRoles = plugin.getCustomRoleManager().getRolesForNation(nation.getId());
        int totalItems = BASE_ROLES.length + customRoles.size() + 1; // +1 bouton créer
        int size = Math.max(27, (int) Math.ceil((totalItems + 9) / 9.0) * 9);
        size = Math.min(size, 54);

        Inventory inv = GuiUtil.createGui("&8Rôles de " + nation.getName(), size / 9);
        GuiUtil.fillBorder(inv);

        int slot = 10;

        // --- Rôles de base ---
        for (NationRole role : BASE_ROLES) {
            if (slot >= size - 9) break;
            Material mat = getMaterialForBaseRole(role);
            inv.setItem(slot, GuiUtil.createItem(mat,
                    role.getColoredDisplay(),
                    "&7Type: &fRôle de base",
                    "&7Rang: &e" + role.getRank(),
                    "",
                    "&eCliquer pour gérer les permissions"
            ));
            slot++;
        }

        // --- Séparateur ---
        if (slot < size - 9) {
            inv.setItem(slot, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, "&8─────────────────"));
            slot++;
        }

        // --- Rôles personnalisés ---
        for (CustomRole role : customRoles) {
            if (slot >= size - 9) break;
            inv.setItem(slot, GuiUtil.createItem(Material.NAME_TAG,
                    "&6" + role.getDisplayName() + " &8(rang " + role.getRank() + ")",
                    "&7Type: &dRôle personnalisé",
                    "&7Nom interne: &f" + role.getName(),
                    "",
                    "&eCliquer pour gérer les permissions"
            ));
            slot++;
        }

        // --- Bouton créer un rôle ---
        boolean canManage = isLeaderOrOfficer();
        if (canManage) {
            inv.setItem(size - 5, GuiUtil.createItem(Material.LIME_DYE,
                    "&a&l+ Créer un rôle personnalisé",
                    "&7Utilisez &e/nationrole create <nom> <rang>",
                    "&7Limite: &e10 rôles par nation"
            ));
        }

        inv.setItem(size - 1, GuiUtil.createItem(Material.BARRIER, "&cFermer"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.LIME_DYE) {
            player.closeInventory();
            player.sendMessage("§eUtilisez: §f/nationrole create <nom> <rang> [affichage]");
            return;
        }

        // Identifier le rôle cliqué
        if (clicked.getItemMeta() == null) return;
        String displayName = clicked.getItemMeta().getDisplayName();

        // Vérifier si c'est un rôle de base
        for (NationRole role : BASE_ROLES) {
            if (displayName.contains(role.getDisplayName())) {
                RolePermissionsGui gui = new RolePermissionsGui(plugin, player, role);
                player.openInventory(gui.build());
                GuiManager.registerGui(player.getUniqueId(), gui);
                return;
            }
        }

        // Vérifier si c'est un rôle personnalisé
        List<CustomRole> customRoles = plugin.getCustomRoleManager().getRolesForNation(nation.getId());
        for (CustomRole role : customRoles) {
            if (displayName.contains(role.getDisplayName())) {
                Inventory editInv = RolesGui.buildRoleEditGui(plugin, role);
                player.openInventory(editInv);
                GuiManager.registerGui(player.getUniqueId(), new CustomRoleEditHolder(plugin, player, nation, role));
                return;
            }
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }

    private boolean isLeaderOrOfficer() {
        fr.nations.nation.NationMember member = nation.getMember(player.getUniqueId());
        if (member == null) return false;
        return member.getRole() == NationRole.LEADER
                || member.getRole() == NationRole.CO_LEADER
                || plugin.getCustomRoleManager().hasPermission(player.getUniqueId(), fr.nations.role.RolePermission.MANAGE_ROLES);
    }

    private Material getMaterialForBaseRole(NationRole role) {
        return switch (role) {
            case LEADER    -> Material.GOLDEN_SWORD;
            case CO_LEADER -> Material.IRON_SWORD;
            case OFFICER   -> Material.IRON_AXE;
            case MEMBER    -> Material.WOODEN_SWORD;
            case RECRUIT   -> Material.WOODEN_SHOVEL;
        };
    }
}
