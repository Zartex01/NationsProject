package fr.nations.jobs;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public enum JobType {

    MINEUR("Mineur", "§8", Material.DIAMOND_PICKAXE,
        "§7Descendez dans les profondeurs et",
        "§7extrayez minerais précieux et ressources.",
        new HashMap<>(Map.ofEntries(
            Map.entry(Material.COAL_ORE,              5),
            Map.entry(Material.DEEPSLATE_COAL_ORE,    5),
            Map.entry(Material.IRON_ORE,             10),
            Map.entry(Material.DEEPSLATE_IRON_ORE,   10),
            Map.entry(Material.COPPER_ORE,            8),
            Map.entry(Material.DEEPSLATE_COPPER_ORE,  8),
            Map.entry(Material.GOLD_ORE,             15),
            Map.entry(Material.DEEPSLATE_GOLD_ORE,   15),
            Map.entry(Material.LAPIS_ORE,            15),
            Map.entry(Material.DEEPSLATE_LAPIS_ORE,  15),
            Map.entry(Material.REDSTONE_ORE,         12),
            Map.entry(Material.DEEPSLATE_REDSTONE_ORE,12),
            Map.entry(Material.DIAMOND_ORE,          40),
            Map.entry(Material.DEEPSLATE_DIAMOND_ORE,40),
            Map.entry(Material.EMERALD_ORE,          50),
            Map.entry(Material.DEEPSLATE_EMERALD_ORE,50),
            Map.entry(Material.NETHER_GOLD_ORE,      12),
            Map.entry(Material.NETHER_QUARTZ_ORE,     8),
            Map.entry(Material.ANCIENT_DEBRIS,        80)
        ))
    ),

    FARMEUR("Farmeur", "§a", Material.WHEAT,
        "§7Cultivez la terre et récoltez",
        "§7vos cultures pour prospérer.",
        new HashMap<>(Map.ofEntries(
            Map.entry(Material.WHEAT,            5),
            Map.entry(Material.CARROTS,          5),
            Map.entry(Material.POTATOES,         5),
            Map.entry(Material.BEETROOTS,        5),
            Map.entry(Material.MELON,           10),
            Map.entry(Material.PUMPKIN,         10),
            Map.entry(Material.SUGAR_CANE,       3),
            Map.entry(Material.CACTUS,           3),
            Map.entry(Material.NETHER_WART,      8),
            Map.entry(Material.COCOA,            5),
            Map.entry(Material.SWEET_BERRY_BUSH, 5)
        ))
    ),

    CHASSEUR("Chasseur", "§c", Material.CROSSBOW,
        "§7Partez à la chasse et éliminez",
        "§7créatures et monstres dangereux.",
        java.util.Collections.emptyMap()
    ),

    BUCHERON("Bûcheron", "§6", Material.IRON_AXE,
        "§7Parcourez les forêts et abattez",
        "§7les arbres pour en récolter le bois.",
        new HashMap<>(Map.ofEntries(
            Map.entry(Material.OAK_LOG,               5),
            Map.entry(Material.SPRUCE_LOG,            5),
            Map.entry(Material.BIRCH_LOG,             5),
            Map.entry(Material.JUNGLE_LOG,            5),
            Map.entry(Material.ACACIA_LOG,            5),
            Map.entry(Material.DARK_OAK_LOG,          5),
            Map.entry(Material.MANGROVE_LOG,          5),
            Map.entry(Material.CHERRY_LOG,            5),
            Map.entry(Material.BAMBOO,                2),
            Map.entry(Material.MUSHROOM_STEM,         3),
            Map.entry(Material.BROWN_MUSHROOM_BLOCK,  3),
            Map.entry(Material.RED_MUSHROOM_BLOCK,    3)
        ))
    );

    private final String displayName;
    private final String color;
    private final Material icon;
    private final String descLine1;
    private final String descLine2;
    private final Map<Material, Integer> xpMap;

    JobType(String displayName, String color, Material icon,
            String descLine1, String descLine2,
            Map<Material, Integer> xpMap) {
        this.displayName = displayName;
        this.color       = color;
        this.icon        = icon;
        this.descLine1   = descLine1;
        this.descLine2   = descLine2;
        this.xpMap       = xpMap;
    }

    public String              getDisplayName()  { return displayName; }
    public String              getColor()        { return color; }
    public String              getColoredName()  { return color + "§l" + displayName; }
    public Material            getIcon()         { return icon; }
    public String              getDescLine1()    { return descLine1; }
    public String              getDescLine2()    { return descLine2; }
    /** Description courte (première ligne seulement) */
    public String              getDescription()  { return descLine1; }
    public Map<Material, Integer> getXpMap()     { return xpMap; }
    public int getXpForBlock(Material mat)       { return xpMap.getOrDefault(mat, 0); }
}
