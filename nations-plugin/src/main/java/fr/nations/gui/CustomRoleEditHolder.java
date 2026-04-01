package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.role.CustomRole;
import fr.nations.role.RolePermission;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Gestionnaire de clics pour le GUI d'édition d'un rôle personnalisé (RolesGui.buildRoleEditGui).
 */
public class CustomRoleEditHolder {

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;
    private CustomRole role;

    public CustomRoleEditHolder(NationsPlugin plugin, Player player, Nation nation, CustomRole role) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
        this.role = role;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Bouton retour (slot 45)
        if (slot == 45 || clicked.getType() == Material.ARROW) {
            new NationRoleListGui(plugin, player, nation).open();
            return;
        }

        // Bouton supprimer (slot 49)
        if (slot == 49 && clicked.getType() == Material.BARRIER) {
            plugin.getCustomRoleManager().deleteRole(role.getId());
            MessageUtil.sendSuccess(player, "Rôle §6" + role.getDisplayName() + " §asupprimé.");
            new NationRoleListGui(plugin, player, nation).open();
            return;
        }

        // Clic sur le nom (slot 4) → renommer
        if (slot == 4 && clicked.getType() == Material.NAME_TAG) {
            player.closeInventory();
            MessageUtil.send(player, "&eTapez le nouveau nom d'affichage pour ce rôle (ou &cannuler&e) :");
            GuiManager.setPendingAction(player.getUniqueId(), "custom_role_rename:" + role.getId());
            return;
        }

        // Clic sur une permission (slots 10-21)
        RolePermission[] perms = RolePermission.values();
        int[] permSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < permSlots.length && i < perms.length; i++) {
            if (slot == permSlots[i]) {
                RolePermission perm = perms[i];
                boolean current = role.hasPermission(perm);
                role.setPermission(perm, !current);
                plugin.getCustomRoleManager().saveRole(role);
                // Rafraîchir le GUI
                player.openInventory(RolesGui.buildRoleEditGui(plugin, role));
                GuiManager.registerGui(player.getUniqueId(), new CustomRoleEditHolder(plugin, player, nation, role));
                MessageUtil.sendSuccess(player, "Permission " + perm.getLabel() + " " + (!current ? "§aactivée" : "§cdésactivée") + "§a.");
                return;
            }
        }
    }
}
