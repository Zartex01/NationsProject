package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.config.ConfigManager;
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

import java.util.List;

public class RolePermissionsGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final NationRole role;
    private int page;

    private static final int[] ITEM_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int ITEMS_PER_PAGE = ITEM_SLOTS.length;

    private static final int SLOT_PREV  = 39;
    private static final int SLOT_INFO  = 40;
    private static final int SLOT_NEXT  = 41;
    private static final int SLOT_CLOSE = 44;

    public RolePermissionsGui(NationsPlugin plugin, Player player, NationRole role) {
        this(plugin, player, role, 0);
    }

    public RolePermissionsGui(NationsPlugin plugin, Player player, NationRole role, int page) {
        this.plugin = plugin;
        this.player = player;
        this.role   = role;
        this.page   = page;
    }

    public Inventory build() {
        ConfigManager cfg = plugin.getConfigManager();
        List<String> allKeys = cfg.getRolePermissionKeys();
        int totalPages = Math.max(1, (int) Math.ceil(allKeys.size() / (double) ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;

        int from = page * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, allKeys.size());
        List<String> pageKeys = allKeys.subList(from, to);

        String title = "&8[" + role.getColoredDisplay() + "&8] Permissions &7(p." + (page + 1) + "/" + totalPages + ")";
        Inventory inv = GuiUtil.createGui(title, 5);
        GuiUtil.fillBorder(inv);

        for (int i = 0; i < pageKeys.size(); i++) {
            String key     = pageKeys.get(i);
            boolean active = cfg.getRolePerm(role.name(), key, false);
            String display = cfg.getRolePermissionDisplay(key);
            String desc    = cfg.getRolePermissionDescription(key);
            Material mat   = cfg.getRolePermissionMaterial(key);
            inv.setItem(ITEM_SLOTS[i], buildItem(mat, display, desc, active));
        }

        if (page > 0) {
            inv.setItem(SLOT_PREV, GuiUtil.createItem(Material.ARROW, "&ePage precedente"));
        }
        inv.setItem(SLOT_INFO, GuiUtil.createItem(Material.PLAYER_HEAD,
            role.getColoredDisplay(),
            "&7Page &e" + (page + 1) + " &7/ &e" + totalPages,
            "&7Permissions: &e" + allKeys.size() + " total",
            "",
            "&7Cliquez sur un item pour",
            "&7activer/desactiver la permission"
        ));
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, GuiUtil.createItem(Material.ARROW, "&ePage suivante"));
        }
        inv.setItem(SLOT_CLOSE, GuiUtil.createItem(Material.BARRIER, "&cFermer"));

        GuiUtil.fillAll(inv);
        return inv;
    }

    private ItemStack buildItem(Material mat, String display, String desc, boolean active) {
        String color   = active ? "&a" : "&c";
        String symbol  = active ? "✔" : "✖";
        String status  = active ? "&aAutorise" : "&cRefuse";
        String hint    = active ? "&7► Cliquer pour &cDesactiver" : "&7► Cliquer pour &aActiver";

        ItemStack item = GuiUtil.createItem(mat,
            color + symbol + " " + display,
            "&8" + desc,
            "",
            status,
            hint
        );

        if (active) {
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

        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == SLOT_PREV && page > 0) { openPage(page - 1); return; }
        if (slot == SLOT_NEXT) { openPage(page + 1); return; }

        int indexInPage = -1;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (ITEM_SLOTS[i] == slot) { indexInPage = i; break; }
        }
        if (indexInPage < 0) return;

        ConfigManager cfg = plugin.getConfigManager();
        List<String> allKeys  = cfg.getRolePermissionKeys();
        int globalIndex = page * ITEMS_PER_PAGE + indexInPage;
        if (globalIndex >= allKeys.size()) return;

        String key     = allKeys.get(globalIndex);
        boolean current = cfg.getRolePerm(role.name(), key, false);
        boolean newVal  = !current;

        cfg.setRolePerm(role.name(), key, newVal);

        String display = cfg.getRolePermissionDisplay(key);
        String msg = newVal ? "&a" + display + " activee" : "&c" + display + " desactivee";
        MessageUtil.sendSuccess(player, "Rolle &e" + role.getDisplayName() + " &7- " + msg + "&7.");

        openPage(page);
    }

    private void openPage(int newPage) {
        RolePermissionsGui newGui = new RolePermissionsGui(plugin, player, role, newPage);
        player.openInventory(newGui.build());
        GuiManager.registerGui(player.getUniqueId(), newGui);
    }

    public void open() {
        openPage(0);
    }
}
