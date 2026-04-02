package fr.nations.jobs;

import org.bukkit.Material;

import java.util.Map;

public enum JobType {

    MINEUR("Mineur", "§8", Material.DIAMOND_PICKAXE,
        "§7Minez des minerais pour gagner de l'XP.",
        Map.of(
            Material.COAL_ORE, 5,        Material.DEEPSLATE_COAL_ORE, 5,
            Material.IRON_ORE, 10,       Material.DEEPSLATE_IRON_ORE, 10,
            Material.COPPER_ORE, 8,      Material.DEEPSLATE_COPPER_ORE, 8,
            Material.GOLD_ORE, 15,       Material.DEEPSLATE_GOLD_ORE, 15,
            Material.LAPIS_ORE, 15,      Material.DEEPSLATE_LAPIS_ORE, 15,
            Material.REDSTONE_ORE, 12,   Material.DEEPSLATE_REDSTONE_ORE, 12,
            Material.DIAMOND_ORE, 40,    Material.DEEPSLATE_DIAMOND_ORE, 40,
            Material.EMERALD_ORE, 50,    Material.DEEPSLATE_EMERALD_ORE, 50,
            Material.NETHER_GOLD_ORE, 12,Material.NETHER_QUARTZ_ORE, 8,
            Material.ANCIENT_DEBRIS, 80
        )
    ),

    FARMEUR("Farmeur", "§a", Material.WHEAT,
        "§7Récoltez des cultures pour gagner de l'XP.",
        Map.of(
            Material.WHEAT, 5,
            Material.CARROTS, 5,
            Material.POTATOES, 5,
            Material.BEETROOTS, 5,
            Material.MELON, 10,
            Material.PUMPKIN, 10,
            Material.SUGAR_CANE, 3,
            Material.CACTUS, 3,
            Material.NETHER_WART, 8,
            Material.COCOA, 5,
            Material.SWEET_BERRY_BUSH, 5
        )
    ),

    CHASSEUR("Chasseur", "§c", Material.CROSSBOW,
        "§7Éliminez des créatures pour gagner de l'XP.",
        java.util.Collections.emptyMap()
    ),

    BUCHERON("Bûcheron", "§6", Material.IRON_AXE,
        "§7Coupez des arbres pour gagner de l'XP.",
        Map.of(
            Material.OAK_LOG, 5,         Material.SPRUCE_LOG, 5,
            Material.BIRCH_LOG, 5,       Material.JUNGLE_LOG, 5,
            Material.ACACIA_LOG, 5,      Material.DARK_OAK_LOG, 5,
            Material.MANGROVE_LOG, 5,    Material.CHERRY_LOG, 5,
            Material.BAMBOO, 2,          Material.MUSHROOM_STEM, 3,
            Material.BROWN_MUSHROOM_BLOCK, 3, Material.RED_MUSHROOM_BLOCK, 3
        )
    );

    private final String displayName;
    private final String color;
    private final Material icon;
    private final String description;
    private final Map<Material, Integer> xpMap;

    JobType(String displayName, String color, Material icon, String description, Map<Material, Integer> xpMap) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.description = description;
        this.xpMap = xpMap;
    }

    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public String getColoredName() { return color + "§l" + displayName; }
    public Material getIcon() { return icon; }
    public String getDescription() { return description; }
    public Map<Material, Integer> getXpMap() { return xpMap; }
    public int getXpForBlock(Material mat) { return xpMap.getOrDefault(mat, 0); }
}
