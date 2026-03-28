package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.role.CustomRole;
import fr.nations.role.CustomRoleManager;
import fr.nations.role.RolePermission;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.MouseButton;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI principal de gestion des rôles de nation.
 * Clic gauche sur un rôle → NationRoleEditGui
 * Clic droit sur un rôle   → supprimer (avec confirmation)
 * Slot 40                  → créer un nouveau rôle (prompt chat)
 * Slot 44                  → fermer / retour
 */
public class NationRolesGui {

    private static final int MAX_ROLES = 10;

    // Slots intérieurs dans un GUI 5 rangées avec bordure
    private static final int[] ROLE_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30
    };

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;

    public NationRolesGui(NationsPlugin plugin, Player player, Nation nation) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Build
    // ──────────────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&5✦ &dRôles &8— &f" + nation.getName(), 5);
        GuiUtil.fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);

        List<CustomRole> roles = plugin.getCustomRoleManager().getRolesForNation(nation.getId());
        boolean canManage = canManage();

        // ── Role items ──
        for (int i = 0; i < Math.min(roles.size(), ROLE_SLOTS.length); i++) {
            CustomRole role = roles.get(i);
            long active  = countPermissions(role, true);
            long total   = RolePermission.values().length;
            List<String> lore = new ArrayList<>();
            lore.add("&7Rang: &e" + role.getRank());
            lore.add("&7Permissions actives: &a" + active + "&7/" + total);
            lore.add("");
            if (canManage) {
                lore.add("&eClique gauche &7→ Modifier les permissions");
                lore.add("&cClique droit &7→ Supprimer ce rôle");
            } else {
                lore.add("&7Clique pour voir les permissions");
            }
            inv.setItem(ROLE_SLOTS[i], GuiUtil.createItem(Material.PAPER,
                "&d" + role.getDisplayName(), lore.toArray(new String[0])));
        }

        // ── Bouton créer ──
        if (canManage) {
            if (roles.size() >= MAX_ROLES) {
                inv.setItem(40, GuiUtil.createItem(Material.BARRIER,
                    "&c✖ Limite atteinte",
                    "&7Maximum &e" + MAX_ROLES + " &7rôles par nation"));
            } else {
                inv.setItem(40, GuiUtil.createItem(Material.EMERALD,
                    "&a✦ Créer un rôle",
                    "&7" + roles.size() + "/" + MAX_ROLES + " rôles utilisés",
                    "",
                    "&eClique &7→ Saisir nom & rang dans le chat"));
            }
        }

        // ── Fermer ──
        inv.setItem(44, GuiUtil.createItem(Material.BARRIER, "&c◄ Retour"));

        GuiUtil.fillAll(inv);
        return inv;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Click
    // ──────────────────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Clic sur les slots de rôles
        List<CustomRole> roles = plugin.getCustomRoleManager().getRolesForNation(nation.getId());
        for (int i = 0; i < Math.min(roles.size(), ROLE_SLOTS.length); i++) {
            if (slot == ROLE_SLOTS[i]) {
                CustomRole role = roles.get(i);
                if (event.isRightClick() && canManage()) {
                    // Supprimer
                    plugin.getCustomRoleManager().deleteRole(role.getId());
                    MessageUtil.sendSuccess(player, "Rôle &6" + role.getDisplayName() + " &asupprimé.");
                    refresh();
                } else {
                    // Modifier
                    new NationRoleEditGui(plugin, player, nation, role).open();
                }
                return;
            }
        }

        switch (slot) {
            case 40 -> {
                if (!canManage()) return;
                if (plugin.getCustomRoleManager().getRoleCount(nation.getId()) >= MAX_ROLES) return;
                player.closeInventory();
                MessageUtil.send(player, "&dRôles &8» &7Entrez: &e<nom_interne> <rang> [nom_affiché...]");
                MessageUtil.send(player, "&7Ex: &eofficier 50 Officier de Guerre");
                MessageUtil.send(player, "&7Tapez &cAnnuler &7pour annuler.");
                GuiManager.setPendingAction(player.getUniqueId(), "role_create:" + nation.getId());
            }
            case 44 -> new NationManageGui(plugin, player, nation).open();
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────

    private void refresh() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }

    private boolean canManage() {
        NationMember member = nation.getMember(player.getUniqueId());
        if (member == null) return false;
        if (member.getRole() == NationRole.LEADER || member.getRole() == NationRole.CO_LEADER) return true;
        return plugin.getCustomRoleManager().hasPermission(player.getUniqueId(), RolePermission.MANAGE_ROLES);
    }

    private long countPermissions(CustomRole role, boolean active) {
        long count = 0;
        for (RolePermission perm : RolePermission.values()) {
            if (role.hasPermission(perm) == active) count++;
        }
        return count;
    }
}
