package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.NationRole;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;

public class RolePermissionsGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final NationRole role;

    private static final Map<Integer, String[]> PERM_SLOTS;

    static {
        PERM_SLOTS = new LinkedHashMap<>();
        PERM_SLOTS.put(10, new String[]{"can-build",       "Construire",       "Autoriser le rôle à placer/casser des blocs"});
        PERM_SLOTS.put(12, new String[]{"can-invite",      "Inviter",          "Autoriser le rôle à inviter des joueurs"});
        PERM_SLOTS.put(14, new String[]{"can-kick",        "Expulser",         "Autoriser le rôle à expulser des membres"});
        PERM_SLOTS.put(19, new String[]{"can-manage-war",  "Gérer les guerres","Autoriser le rôle à déclarer des guerres"});
        PERM_SLOTS.put(21, new String[]{"can-manage-bank", "Gérer la banque",  "Autoriser le rôle à retirer de la banque"});
        PERM_SLOTS.put(23, new String[]{"can-dissolve",    "Dissoudre",        "Autoriser le rôle à dissoudre la nation"});
    }

    private static final Map<String, Material> PERM_MATERIALS = Map.of(
        "can-build",       Material.GRASS_BLOCK,
        "can-invite",      Material.PAPER,
        "can-kick",        Material.LEATHER_BOOTS,
        "can-manage-war",  Material.IRON_SWORD,
        "can-manage-bank", Material.GOLD_INGOT,
        "can-dissolve",    Material.TNT
    );

    public RolePermissionsGui(NationsPlugin plugin, Player player, NationRole role) {
        this.plugin = plugin;
        this.player = player;
        this.role = role;
    }

    public Inventory build() {
        String title = "&8Permissions " + role.getColoredDisplay();
        Inventory inv = GuiUtil.createGui(title, 4);
        GuiUtil.fillBorder(inv);

        PERM_SLOTS.forEach((slot, data) -> {
            String permKey = data[0];
            String label   = data[1];
            String desc    = data[2];

            boolean enabled = plugin.getConfigManager().getRolePerm(role.name(), permKey, false);
            Material mat = PERM_MATERIALS.getOrDefault(permKey, Material.PAPER);

            ItemStack item = buildPermItem(mat, label, desc, permKey, enabled);
            inv.setItem(slot, item);
        });

        inv.setItem(31, GuiUtil.createItem(Material.BARRIER, "&cFermer"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    private ItemStack buildPermItem(Material mat, String label, String desc, String permKey, boolean enabled) {
        String status = enabled ? "&a✔ Autorisé" : "&c✖ Refusé";
        String hint   = enabled ? "&7Cliquer pour &cDésactiver" : "&7Cliquer pour &aActiver";

        ItemStack item = GuiUtil.createItem(mat,
            (enabled ? "&a" : "&c") + label,
            "&7" + desc,
            "",
            status,
            hint
        );

        if (enabled) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 31) {
            player.closeInventory();
            return;
        }

        String[] data = PERM_SLOTS.get(slot);
        if (data == null) return;

        String permKey = data[0];
        boolean current = plugin.getConfigManager().getRolePerm(role.name(), permKey, false);
        boolean newValue = !current;

        plugin.getConfigManager().setRolePerm(role.name(), permKey, newValue);

        String statusMsg = newValue ? "&aactivée" : "&cdésactivée";
        MessageUtil.sendSuccess(player, "Permission &e" + data[1] + " &7pour &e"
            + role.getDisplayName() + " &7: " + statusMsg + "&7.");

        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
