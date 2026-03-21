package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.role.CustomRole;
import fr.nations.role.RolePermission;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RolesGui {

    private static final String TITLE_LIST = "§8§l⚔ Rôles de la nation";
    private static final String TITLE_EDIT = "§8§l⚙ Modifier le rôle";

    public static Inventory buildRoleListGui(NationsPlugin plugin, Nation nation) {
        List<CustomRole> roles = plugin.getCustomRoleManager().getRolesForNation(nation.getId());
        int size = Math.max(27, ((roles.size() / 9) + 2) * 9);
        Inventory inv = Bukkit.createInventory(null, size, TITLE_LIST);

        for (int i = 0; i < roles.size() && i < size - 9; i++) {
            CustomRole role = roles.get(i);
            ItemStack item = GuiUtil.createItem(
                Material.NAME_TAG,
                "§6" + role.getDisplayName() + " §8(rang " + role.getRank() + ")",
                List.of(
                    "§7Nom interne: §f" + role.getName(),
                    "§7Permissions:",
                    "  " + role.getFormattedPermissions().substring(0, Math.min(50, role.getFormattedPermissions().length())),
                    "",
                    "§eClique pour modifier"
                )
            );
            inv.setItem(i, item);
        }

        ItemStack createBtn = GuiUtil.createItem(
            Material.LIME_DYE,
            "§a§l+ Créer un rôle",
            List.of("§7Limite: §e10 rôles par nation")
        );
        inv.setItem(size - 5, createBtn);

        ItemStack back = GuiUtil.createItem(Material.ARROW, "§7← Retour", List.of());
        inv.setItem(size - 1, back);

        GuiUtil.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        return inv;
    }

    public static Inventory buildRoleEditGui(NationsPlugin plugin, CustomRole role) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_EDIT + ": §6" + role.getDisplayName());

        ItemStack info = GuiUtil.createItem(
            Material.NAME_TAG,
            "§6" + role.getDisplayName(),
            List.of(
                "§7Nom interne: §f" + role.getName(),
                "§7Rang: §e" + role.getRank(),
                "§7(Rang bas = plus de priorité)",
                "",
                "§eClique pour renommer"
            )
        );
        inv.setItem(4, info);

        RolePermission[] perms = RolePermission.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < perms.length && i < slots.length; i++) {
            RolePermission perm = perms[i];
            boolean has = role.hasPermission(perm);
            Material mat = has ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
            String status = has ? "§a§l✔ Activée" : "§c§l✘ Désactivée";
            ItemStack item = GuiUtil.createItem(
                mat,
                perm.getLabel() + " " + status,
                List.of(
                    "§7" + perm.getDescription(),
                    "",
                    "§eClique pour basculer"
                )
            );
            inv.setItem(slots[i], item);
        }

        ItemStack delete = GuiUtil.createItem(
            Material.BARRIER,
            "§c§l✗ Supprimer ce rôle",
            List.of("§7Les joueurs perdront ce rôle.")
        );
        inv.setItem(49, delete);

        ItemStack back = GuiUtil.createItem(Material.ARROW, "§7← Retour", List.of());
        inv.setItem(45, back);

        GuiUtil.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        return inv;
    }

    public static boolean isRoleList(Inventory inv) {
        return inv.getSize() >= 27 && inv.getViewers().isEmpty() == false
            && TITLE_LIST.equals(inv.getViewers().get(0).getOpenInventory().getTitle());
    }
}
