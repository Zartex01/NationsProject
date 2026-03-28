package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.role.CustomRole;
import fr.nations.role.RolePermission;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * GUI d'édition des permissions d'un rôle.
 *
 * Layout (4 rangées) :
 *   Row 0 : bordure violette, slot 4 = infos du rôle
 *   Row 1 : perms 0-6  (slots 10-16)
 *   Row 2 : perms 7-9  (slots 19-21), reste = remplissage
 *   Row 3 : bordure + boutons actions (28, 30, 32, 35)
 */
public class NationRoleEditGui {

    // 10 permissions mappées sur 10 slots intérieurs (lignes 1 et 2)
    private static final int[] PERM_SLOTS = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 };

    private static final RolePermission[] PERMS = RolePermission.values();

    // Matériaux représentant chaque permission
    private static final Material[] PERM_MATERIALS = {
        Material.IRON_PICKAXE,   // BUILD
        Material.WRITTEN_BOOK,   // INVITE
        Material.IRON_SWORD,     // KICK
        Material.NETHERITE_SWORD,// MANAGE_WAR
        Material.GOLD_INGOT,     // MANAGE_BANK
        Material.GRASS_BLOCK,    // MANAGE_CLAIMS
        Material.BLUE_DYE,       // MANAGE_ALLIES
        Material.NAME_TAG,       // MANAGE_ROLES
        Material.OAK_SIGN,       // RENAME
        Material.TNT             // DISBAND
    };

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;
    private final CustomRole role;

    public NationRoleEditGui(NationsPlugin plugin, Player player, Nation nation, CustomRole role) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
        this.role   = role;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Build
    // ──────────────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&5Rôle: &d" + role.getDisplayName(), 4);
        GuiUtil.fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);

        // ── Infos du rôle ──
        inv.setItem(4, GuiUtil.createItem(Material.BOOK,
            "&d&l" + role.getDisplayName(),
            "&7Nom interne: &f" + role.getName(),
            "&7Rang: &e" + role.getRank(),
            "&7Membres avec ce rôle: &f" + countAssigned()));

        // ── Toggles de permissions ──
        boolean canManage = canManage();
        for (int i = 0; i < PERMS.length; i++) {
            RolePermission perm = PERMS[i];
            boolean active = role.hasPermission(perm);
            Material mat = active ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
            String status = active ? "&a✔ Activé" : "&c✘ Désactivé";
            String hint   = canManage ? (active ? "&7Clique pour &cdésactiver" : "&7Clique pour &aactiver") : "";
            inv.setItem(PERM_SLOTS[i], GuiUtil.createItem(
                PERM_MATERIALS[i],
                perm.getLabel(),
                "&7" + perm.getDescription(),
                "",
                status,
                hint
            ));
        }

        // ── Boutons actions ──
        if (canManage) {
            inv.setItem(28, GuiUtil.createItem(Material.PLAYER_HEAD,
                "&e👥 Assigner à un joueur",
                "&7Voir les membres de la nation",
                "&7et leur attribuer ce rôle"));

            inv.setItem(30, GuiUtil.createItem(Material.NAME_TAG,
                "&6✎ Renommer",
                "&7Changer le nom d'affichage",
                "&7Actuel: &f" + role.getDisplayName()));

            inv.setItem(32, GuiUtil.createItem(Material.COMPARATOR,
                "&b↕ Changer le rang",
                "&7Priorité du rôle (1 = bas)",
                "&7Actuel: &e" + role.getRank()));
        }

        inv.setItem(35, GuiUtil.createItem(Material.BARRIER, "&c◄ Retour"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Click
    // ──────────────────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Toggle permission
        for (int i = 0; i < PERM_SLOTS.length; i++) {
            if (slot == PERM_SLOTS[i]) {
                if (!canManage()) return;
                RolePermission perm = PERMS[i];
                boolean newVal = !role.hasPermission(perm);
                role.setPermission(perm, newVal);
                plugin.getCustomRoleManager().saveRole(role);
                MessageUtil.sendSuccess(player, perm.getLabel() + " " + (newVal ? "&aactivé" : "&cdésactivé") + " &apour &6" + role.getDisplayName());
                refresh();
                return;
            }
        }

        switch (slot) {
            case 28 -> {
                if (!canManage()) return;
                new NationRoleAssignGui(plugin, player, nation, role).open();
            }
            case 30 -> {
                if (!canManage()) return;
                player.closeInventory();
                MessageUtil.send(player, "&dRôles &8» &7Nouveau nom d'affichage pour &6" + role.getDisplayName() + "&7:");
                MessageUtil.send(player, "&7Tapez &cAnnuler &7pour annuler.");
                GuiManager.setPendingAction(player.getUniqueId(),
                    "role_rename:" + role.getId() + ":" + nation.getId());
            }
            case 32 -> {
                if (!canManage()) return;
                player.closeInventory();
                MessageUtil.send(player, "&dRôles &8» &7Nouveau rang (1-999) pour &6" + role.getDisplayName() + "&7:");
                MessageUtil.send(player, "&7Tapez &cAnnuler &7pour annuler.");
                GuiManager.setPendingAction(player.getUniqueId(),
                    "role_setrank:" + role.getId() + ":" + nation.getId());
            }
            case 35 -> new NationRolesGui(plugin, player, nation).open();
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
        return plugin.getCustomRoleManager().hasPermission(player.getUniqueId(),
            fr.nations.role.RolePermission.MANAGE_ROLES);
    }

    private long countAssigned() {
        return nation.getMembers().stream()
            .filter(m -> {
                CustomRole r = plugin.getCustomRoleManager().getPlayerRole(m.getPlayerId());
                return r != null && r.getId().equals(role.getId());
            })
            .count();
    }
}
