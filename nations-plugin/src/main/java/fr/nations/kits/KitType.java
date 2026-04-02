package fr.nations.kits;

import fr.nations.grade.GradeType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public enum KitType {

    JOUEUR(
        "Joueur", "§7", Material.LEATHER_CHESTPLATE,
        GradeType.JOUEUR,
        24 * 3600L
    ),
    SOUTIEN(
        "Soutien", "§a", Material.IRON_CHESTPLATE,
        GradeType.SOUTIEN,
        24 * 3600L
    ),
    PREMIUM(
        "Premium", "§6", Material.DIAMOND_CHESTPLATE,
        GradeType.PREMIUM,
        24 * 3600L
    ),
    HEROS(
        "Héros", "§b", Material.NETHERITE_CHESTPLATE,
        GradeType.CHEVALIER,
        24 * 3600L
    );

    private final String displayName;
    private final String color;
    private final Material icon;
    private final GradeType requiredGrade;
    private final long cooldownSeconds;

    KitType(String displayName, String color, Material icon,
            GradeType requiredGrade, long cooldownSeconds) {
        this.displayName     = displayName;
        this.color           = color;
        this.icon            = icon;
        this.requiredGrade   = requiredGrade;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getDisplayName()    { return displayName; }
    public String getColor()          { return color; }
    public String getColoredName()    { return color + "§l" + displayName; }
    public Material getIcon()         { return icon; }
    public GradeType getRequiredGrade(){ return requiredGrade; }
    public long getCooldownSeconds()  { return cooldownSeconds; }

    /** Vérifie si un grade peut accéder à ce kit (grade ≥ grade requis). */
    public boolean isAccessibleBy(GradeType grade) {
        return grade.ordinal() >= requiredGrade.ordinal();
    }

    /** Construit et retourne une copie fraîche des items du kit. */
    public List<ItemStack> getItems() {
        return switch (this) {
            case JOUEUR  -> buildJoueurItems();
            case SOUTIEN -> buildSoutienItems();
            case PREMIUM -> buildPremiumItems();
            case HEROS   -> buildHerosItems();
        };
    }

    // ─── Contenu des kits ────────────────────────────────────────────────────

    private static List<ItemStack> buildJoueurItems() {
        List<ItemStack> items = new ArrayList<>();

        // Armure complète en cuir
        items.add(enchant(new ItemStack(Material.LEATHER_HELMET),
            "protection", 1));
        items.add(enchant(new ItemStack(Material.LEATHER_CHESTPLATE),
            "protection", 1));
        items.add(enchant(new ItemStack(Material.LEATHER_LEGGINGS),
            "protection", 1));
        items.add(enchant(new ItemStack(Material.LEATHER_BOOTS),
            "protection", 1));

        // Armes & outils
        items.add(enchant(new ItemStack(Material.STONE_SWORD),
            "sharpness", 1));
        items.add(new ItemStack(Material.STONE_PICKAXE));

        // Consommables
        items.add(stack(Material.COOKED_BEEF, 16));
        items.add(stack(Material.TORCH, 16));

        return items;
    }

    private static List<ItemStack> buildSoutienItems() {
        List<ItemStack> items = new ArrayList<>();

        // Armure complète en fer
        items.add(enchant(new ItemStack(Material.IRON_HELMET),
            "protection", 2,
            "unbreaking", 2));
        items.add(enchant(new ItemStack(Material.IRON_CHESTPLATE),
            "protection", 2,
            "unbreaking", 2));
        items.add(enchant(new ItemStack(Material.IRON_LEGGINGS),
            "protection", 2,
            "unbreaking", 2));
        items.add(enchant(new ItemStack(Material.IRON_BOOTS),
            "protection", 2,
            "unbreaking", 2));

        // Armes & outils
        items.add(enchant(new ItemStack(Material.IRON_SWORD),
            "sharpness", 2,
            "unbreaking", 2));
        items.add(enchant(new ItemStack(Material.IRON_PICKAXE),
            "efficiency", 2,
            "unbreaking", 2));

        // Consommables
        items.add(stack(Material.COOKED_BEEF, 32));
        items.add(stack(Material.BREAD, 16));
        items.add(stack(Material.TORCH, 32));

        return items;
    }

    private static List<ItemStack> buildPremiumItems() {
        List<ItemStack> items = new ArrayList<>();

        // Armure complète en diamant
        items.add(enchant(new ItemStack(Material.DIAMOND_HELMET),
            "protection", 3,
            "unbreaking", 3));
        items.add(enchant(new ItemStack(Material.DIAMOND_CHESTPLATE),
            "protection", 3,
            "unbreaking", 3));
        items.add(enchant(new ItemStack(Material.DIAMOND_LEGGINGS),
            "protection", 3,
            "unbreaking", 3));
        items.add(enchant(new ItemStack(Material.DIAMOND_BOOTS),
            "protection", 3,
            "unbreaking", 3,
            "feather_falling", 3));

        // Armes & outils
        items.add(enchant(new ItemStack(Material.DIAMOND_SWORD),
            "sharpness", 3,
            "unbreaking", 3,
            "looting", 2));
        items.add(enchant(new ItemStack(Material.DIAMOND_PICKAXE),
            "efficiency", 3,
            "unbreaking", 3,
            "fortune", 2));

        // Consommables
        items.add(stack(Material.COOKED_BEEF, 48));
        items.add(stack(Material.GOLDEN_APPLE, 4));
        items.add(stack(Material.TORCH, 64));
        items.add(stack(Material.ENDER_PEARL, 4));

        return items;
    }

    private static List<ItemStack> buildHerosItems() {
        List<ItemStack> items = new ArrayList<>();

        // Armure complète en netherite
        items.add(enchant(new ItemStack(Material.NETHERITE_HELMET),
            "protection", 4,
            "unbreaking", 3,
            "respiration", 3));
        items.add(enchant(new ItemStack(Material.NETHERITE_CHESTPLATE),
            "protection", 4,
            "unbreaking", 3));
        items.add(enchant(new ItemStack(Material.NETHERITE_LEGGINGS),
            "protection", 4,
            "unbreaking", 3,
            "swift_sneak", 3));
        items.add(enchant(new ItemStack(Material.NETHERITE_BOOTS),
            "protection", 4,
            "unbreaking", 3,
            "feather_falling", 4,
            "soul_speed", 2));

        // Armes & outils
        items.add(enchant(new ItemStack(Material.NETHERITE_SWORD),
            "sharpness", 5,
            "unbreaking", 3,
            "looting", 3,
            "fire_aspect", 2));
        items.add(enchant(new ItemStack(Material.NETHERITE_PICKAXE),
            "efficiency", 5,
            "unbreaking", 3,
            "fortune", 3));

        // Consommables
        items.add(stack(Material.COOKED_BEEF, 64));
        items.add(stack(Material.GOLDEN_APPLE, 8));
        items.add(stack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        items.add(stack(Material.ENDER_PEARL, 8));
        items.add(stack(Material.TORCH, 64));

        return items;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static ItemStack stack(Material mat, int amount) {
        return new ItemStack(mat, amount);
    }

    /**
     * Applique des enchantements via Registry (compatible Paper 1.21+).
     * @param item   l'item à enchanter
     * @param pairs  paires alternées ("enchant_key", level)
     */
    private static ItemStack enchant(ItemStack item, Object... pairs) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String key   = (String) pairs[i];
            int    level = (int)   pairs[i + 1];
            Enchantment enc = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
            if (enc != null) meta.addEnchant(enc, level, true);
        }
        item.setItemMeta(meta);
        return item;
    }
}
