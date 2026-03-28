package fr.nations.kit;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KitManager {
        
    public static final long COOLDOWN_PLAYTIME_MS = 3L * 60 * 60 * 1000;

    private final NationsPlugin plugin;

    public KitManager(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player player, GradeType kit) {
        long claimPt = plugin.getPlaytimeTracker().getKitClaimPlaytime(player.getUniqueId(), kit);
        if (claimPt < 0) return false;
        long currentPt = plugin.getPlaytimeTracker().getPlaytimeMillis(player.getUniqueId());
        return (currentPt - claimPt) < COOLDOWN_PLAYTIME_MS;
    }

    /** Retourne le temps de jeu restant en secondes avant de pouvoir reprendre le kit. */
    public long getRemainingCooldownSeconds(Player player, GradeType kit) {
        long claimPt   = plugin.getPlaytimeTracker().getKitClaimPlaytime(player.getUniqueId(), kit);
        if (claimPt < 0) return 0;
        long currentPt = plugin.getPlaytimeTracker().getPlaytimeMillis(player.getUniqueId());
        long remaining = COOLDOWN_PLAYTIME_MS - (currentPt - claimPt);
        return remaining > 0 ? remaining / 1000 : 0;
    }

    public void setCooldown(Player player, GradeType kit) {
        long currentPt = plugin.getPlaytimeTracker().getPlaytimeMillis(player.getUniqueId());
        plugin.getPlaytimeTracker().setKitClaimPlaytime(player.getUniqueId(), kit, currentPt);
    }

    public void giveKit(Player player, GradeType kit) {
        List<ItemStack> items = buildKit(kit);
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    private List<ItemStack> buildKit(GradeType grade) {
        return switch (grade) {
            case HEROS     -> buildHerosKit();
            case CHEVALIER -> buildChevalierKit();
            case PREMIUM   -> buildPremiumKit();
            default        -> List.of();
        };
    }

    private List<ItemStack> buildHerosKit() {
        List<ItemStack> items = new ArrayList<>();

        items.add(enchant(new ItemStack(Material.IRON_HELMET),
                Enchantment.PROTECTION, 2,
                Enchantment.UNBREAKING, 2));
        items.add(enchant(new ItemStack(Material.IRON_CHESTPLATE),
                Enchantment.PROTECTION, 2,
                Enchantment.UNBREAKING, 2));
        items.add(enchant(new ItemStack(Material.IRON_LEGGINGS),
                Enchantment.PROTECTION, 2,
                Enchantment.UNBREAKING, 2));
        items.add(enchant(new ItemStack(Material.IRON_BOOTS),
                Enchantment.PROTECTION, 2,
                Enchantment.UNBREAKING, 2,
                Enchantment.FEATHER_FALLING, 2));

        items.add(enchant(new ItemStack(Material.DIAMOND_SWORD),
                Enchantment.SHARPNESS, 2,
                Enchantment.UNBREAKING, 2));
        items.add(enchant(new ItemStack(Material.BOW),
                Enchantment.POWER, 1,
                Enchantment.UNBREAKING, 1));

        items.add(stack(Material.ARROW, 64));
        items.add(stack(Material.BREAD, 32));
        items.add(stack(Material.COOKED_BEEF, 16));
        items.add(stack(Material.GOLDEN_APPLE, 8));
        items.add(stack(Material.IRON_INGOT, 16));

        return items;
    }

    private List<ItemStack> buildChevalierKit() {
        List<ItemStack> items = new ArrayList<>();

        items.add(enchant(new ItemStack(Material.IRON_HELMET),
                Enchantment.PROTECTION, 3,
                Enchantment.UNBREAKING, 3));
        items.add(enchant(new ItemStack(Material.IRON_CHESTPLATE),
                Enchantment.PROTECTION, 3,
                Enchantment.UNBREAKING, 3));
        items.add(enchant(new ItemStack(Material.IRON_LEGGINGS),
                Enchantment.PROTECTION, 3,
                Enchantment.UNBREAKING, 3));
        items.add(enchant(new ItemStack(Material.IRON_BOOTS),
                Enchantment.PROTECTION, 3,
                Enchantment.UNBREAKING, 3,
                Enchantment.FEATHER_FALLING, 3));

        items.add(enchant(new ItemStack(Material.DIAMOND_SWORD),
                Enchantment.SHARPNESS, 3,
                Enchantment.UNBREAKING, 3,
                Enchantment.FIRE_ASPECT, 1));
        items.add(enchant(new ItemStack(Material.BOW),
                Enchantment.POWER, 2,
                Enchantment.UNBREAKING, 2,
                Enchantment.PUNCH, 1));

        items.add(stack(Material.ARROW, 64));
        items.add(stack(Material.BREAD, 64));
        items.add(stack(Material.COOKED_BEEF, 32));
        items.add(stack(Material.GOLDEN_APPLE, 16));
        items.add(stack(Material.IRON_INGOT, 32));
        items.add(stack(Material.DIAMOND, 4));

        return items;
    }

    private List<ItemStack> buildPremiumKit() {
        List<ItemStack> items = new ArrayList<>();

        items.add(enchant(new ItemStack(Material.DIAMOND_HELMET),
                Enchantment.PROTECTION, 3,
                Enchantment.UNBREAKING, 3,
                Enchantment.AQUA_AFFINITY, 1,
                Enchantment.RESPIRATION, 2));
        items.add(enchant(new ItemStack(Material.DIAMOND_CHESTPLATE),
                Enchantment.PROTECTION, 3,
                Enchantment.UNBREAKING, 3));
        items.add(enchant(new ItemStack(Material.DIAMOND_LEGGINGS),
                Enchantment.PROTECTION, 3,
                Enchantment.UNBREAKING, 3));
        items.add(enchant(new ItemStack(Material.DIAMOND_BOOTS),
                Enchantment.PROTECTION, 3,
                Enchantment.UNBREAKING, 3,
                Enchantment.FEATHER_FALLING, 4));

        items.add(enchant(new ItemStack(Material.DIAMOND_SWORD),
                Enchantment.SHARPNESS, 4,
                Enchantment.UNBREAKING, 3,
                Enchantment.LOOTING, 2,
                Enchantment.FIRE_ASPECT, 1));
        items.add(enchant(new ItemStack(Material.BOW),
                Enchantment.POWER, 3,
                Enchantment.UNBREAKING, 3,
                Enchantment.PUNCH, 2,
                Enchantment.INFINITY, 1));

        items.add(stack(Material.ARROW, 1));
        items.add(stack(Material.COOKED_BEEF, 64));
        items.add(stack(Material.GOLDEN_APPLE, 32));
        items.add(stack(Material.ENCHANTED_GOLDEN_APPLE, 4));
        items.add(stack(Material.ENDER_PEARL, 4));
        items.add(stack(Material.DIAMOND, 8));

        return items;
    }

    private ItemStack stack(Material mat, int amount) {
        return new ItemStack(mat, amount);
    }

    private ItemStack enchant(ItemStack item, Object... pairs) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        for (int i = 0; i < pairs.length - 1; i += 2) {
            Enchantment ench = (Enchantment) pairs[i];
            int level = (int) pairs[i + 1];
            meta.addEnchant(ench, level, true);
        }
        item.setItemMeta(meta);
        return item;
    }

    public String formatCooldown(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
