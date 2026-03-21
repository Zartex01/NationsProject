package fr.nations.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GuiUtil {

    public static Inventory createGui(String title, int rows) {
        return Bukkit.createInventory(null, rows * 9, MessageUtil.colorize(title));
    }

    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(MessageUtil.colorize(name));
        if (lore.length > 0) {
            List<String> coloredLore = Arrays.stream(lore)
                .map(MessageUtil::colorize)
                .collect(Collectors.toList());
            meta.setLore(coloredLore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createFillerItem() {
        return createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public static void fillBorder(Inventory inv) {
        int size = inv.getSize();
        int rows = size / 9;
        ItemStack filler = createFillerItem();

        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = size - 9; i < size; i++) inv.setItem(i, filler);
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * 9, filler);
            inv.setItem(row * 9 + 8, filler);
        }
    }

    public static void fillAll(Inventory inv) {
        ItemStack filler = createFillerItem();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    public static ItemStack setAmount(ItemStack item, int amount) {
        item.setAmount(amount);
        return item;
    }
}
