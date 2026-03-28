package fr.nations.shop;

import fr.nations.NationsPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.*;

public class ShopManager {

    private final NationsPlugin plugin;
    private final List<ShopCategory> categories = new ArrayList<>();
    private final Map<Material, Double> sellPriceMap = new HashMap<>();

    public ShopManager(NationsPlugin plugin) {
        this.plugin = plugin;
        buildCategories();
        buildSellMap();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    public List<ShopCategory> getCategories() { return Collections.unmodifiableList(categories); }

    public ShopCategory getCategoryById(String id) {
        return categories.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    public double getSellPrice(Material material) {
        return sellPriceMap.getOrDefault(material, 0.0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Economy helpers
    // ─────────────────────────────────────────────────────────────────────────

    public boolean buy(fr.nations.economy.EconomyManager eco, java.util.UUID playerId, ShopItem item, int purchases) {
        double cost = item.getBuyPrice() * purchases;
        if (!eco.has(playerId, cost)) return false;
        eco.withdraw(playerId, cost);
        return true;
    }

    public double sell(fr.nations.economy.EconomyManager eco, java.util.UUID playerId, Material material, int amount) {
        double price = getSellPrice(material);
        if (price <= 0) return 0;
        double total = price * amount;
        eco.deposit(playerId, total);
        return total;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Category & item definitions
    // ─────────────────────────────────────────────────────────────────────────

    private void buildCategories() {
        categories.add(minerais());
        categories.add(agriculture());
        categories.add(nourriture());
        categories.add(blocs());
        categories.add(mobDrops());
        categories.add(materiaux());
        categories.add(enchantements());
        categories.add(special());
    }

    private void buildSellMap() {
        for (ShopCategory cat : categories) {
            for (ShopItem item : cat.getItems()) {
                if (item.isSellable()) {
                    sellPriceMap.merge(item.getMaterial(), item.getSellPrice(), Math::max);
                }
            }
        }
    }

    // ── 1. Minerais ──────────────────────────────────────────────────────────

    private ShopCategory minerais() {
        return new ShopCategory("minerais", "&b⛏ Minerais & Ressources", Material.IRON_PICKAXE,
            Material.CYAN_STAINED_GLASS_PANE, List.of(
                item(Material.COAL,              3,    1   ),
                item(Material.CHARCOAL,          4,    1   ),
                item(Material.RAW_IRON,          8,    3   ),
                item(Material.IRON_INGOT,        15,   5   ),
                item(Material.IRON_BLOCK,        120,  40  ),
                item(Material.RAW_GOLD,          12,   4   ),
                item(Material.GOLD_INGOT,        25,   8   ),
                item(Material.GOLD_BLOCK,        200,  65  ),
                item(Material.RAW_COPPER,        5,    1   ),
                item(Material.COPPER_INGOT,      10,   3   ),
                item(Material.COPPER_BLOCK,      80,   25  ),
                item(Material.LAPIS_LAZULI,      8,    2   ),
                item(Material.REDSTONE,          6,    2   ),
                item(Material.QUARTZ,            5,    1   ),
                item(Material.AMETHYST_SHARD,    10,   3   ),
                item(Material.DIAMOND,           250,  100 ),
                item(Material.DIAMOND_BLOCK,     2000, 850 ),
                item(Material.EMERALD,           60,   25  ),
                item(Material.EMERALD_BLOCK,     480,  200 ),
                item(Material.NETHERITE_SCRAP,   400,  150 ),
                item(Material.NETHERITE_INGOT,   1800, 700 )
        ));
    }

    // ── 2. Agriculture ───────────────────────────────────────────────────────

    private ShopCategory agriculture() {
        return new ShopCategory("agriculture", "&aGraine Agriculture", Material.WHEAT,
            Material.LIME_STAINED_GLASS_PANE, List.of(
                item(Material.WHEAT_SEEDS,       2,    1   ),
                item(Material.WHEAT,             5,    2   ),
                item(Material.POTATO,            5,    2   ),
                item(Material.CARROT,            5,    2   ),
                item(Material.BEETROOT_SEEDS,    2,    1   ),
                item(Material.BEETROOT,          4,    1   ),
                item(Material.PUMPKIN,           30,   10  ),
                item(Material.MELON_SLICE,       3,    1   ),
                item(Material.SUGAR_CANE,        4,    1   ),
                item(Material.BAMBOO,            2,    0   ),
                item(Material.CACTUS,            3,    1   ),
                item(Material.COCOA_BEANS,       5,    2   ),
                item(Material.NETHER_WART,       10,   3   ),
                item(Material.SWEET_BERRIES,     5,    2   ),
                item(Material.CHORUS_FRUIT,      15,   5   ),
                item(Material.BONE_MEAL,         4,    1   ),
                item(Material.GLOW_BERRIES,      8,    3   ),
                item(Material.SEA_PICKLE,        8,    3   )
        ));
    }

    // ── 3. Nourriture ────────────────────────────────────────────────────────

    private ShopCategory nourriture() {
        return new ShopCategory("nourriture", "&6Nourriture", Material.COOKED_BEEF,
            Material.ORANGE_STAINED_GLASS_PANE, List.of(
                item(Material.BREAD,             15,   5   ),
                item(Material.COOKED_BEEF,       25,   8   ),
                item(Material.COOKED_PORKCHOP,   22,   7   ),
                item(Material.COOKED_CHICKEN,    18,   6   ),
                item(Material.COOKED_MUTTON,     20,   7   ),
                item(Material.COOKED_SALMON,     20,   6   ),
                item(Material.COOKED_COD,        15,   5   ),
                item(Material.COOKED_RABBIT,     18,   6   ),
                item(Material.BAKED_POTATO,      10,   3   ),
                item(Material.MUSHROOM_STEW,     20,   0   ),
                item(Material.RABBIT_STEW,       25,   0   ),
                item(Material.CAKE,              60,   0   ),
                item(Material.PUMPKIN_PIE,       20,   7   ),
                item(Material.COOKIE,            8,    2   ),
                item(Material.APPLE,             10,   3   ),
                item(Material.GOLDEN_APPLE,      350,  0   ),
                item(Material.ENCHANTED_GOLDEN_APPLE, 2500, 0),
                item(Material.HONEY_BOTTLE,      15,   5   ),
                item(Material.DRIED_KELP,        3,    1   )
        ));
    }

    // ── 4. Blocs ─────────────────────────────────────────────────────────────

    private ShopCategory blocs() {
        return new ShopCategory("blocs", "&7Blocs & Construction", Material.BRICKS,
            Material.BROWN_STAINED_GLASS_PANE, List.of(
                item(Material.STONE,             5,    2   ),
                item(Material.SMOOTH_STONE,      6,    2   ),
                item(Material.COBBLESTONE,       3,    1   ),
                item(Material.STONE_BRICKS,      8,    3   ),
                item(Material.MOSSY_STONE_BRICKS,12,   4   ),
                item(Material.DIRT,              2,    1   ),
                item(Material.GRASS_BLOCK,       5,    2   ),
                item(Material.SAND,              5,    2   ),
                item(Material.RED_SAND,          6,    2   ),
                item(Material.GRAVEL,            5,    2   ),
                item(Material.CLAY,              8,    3   ),
                item(Material.GLASS,             8,    2   ),
                item(Material.WHITE_WOOL,        10,   3   ),
                item(Material.WHITE_TERRACOTTA,  8,    3   ),
                item(Material.OBSIDIAN,          50,   20  ),
                item(Material.CRYING_OBSIDIAN,   60,   22  ),
                item(Material.BLACKSTONE,        5,    2   ),
                item(Material.DEEPSLATE,         8,    3   ),
                item(Material.OAK_LOG,           10,   3   ),
                item(Material.BIRCH_LOG,         10,   3   ),
                item(Material.SPRUCE_LOG,        10,   3   ),
                item(Material.JUNGLE_LOG,        12,   4   ),
                item(Material.ACACIA_LOG,        11,   3   ),
                item(Material.DARK_OAK_LOG,      11,   3   ),
                item(Material.CHERRY_LOG,        15,   5   ),
                item(Material.TUFF,              4,    1   ),
                item(Material.CALCITE,           5,    2   ),
                item(Material.DRIPSTONE_BLOCK,   6,    2   ),
                item(Material.NETHERRACK,        3,    1   ),
                item(Material.SOUL_SAND,         8,    3   )
        ));
    }

    // ── 5. Mob Drops ─────────────────────────────────────────────────────────

    private ShopCategory mobDrops() {
        return new ShopCategory("mob_drops", "&cMob Drops", Material.BONE,
            Material.RED_STAINED_GLASS_PANE, List.of(
                item(Material.LEATHER,           20,   7   ),
                item(Material.FEATHER,           5,    2   ),
                item(Material.STRING,            5,    2   ),
                item(Material.BONE,              8,    3   ),
                item(Material.SPIDER_EYE,        10,   3   ),
                item(Material.GUNPOWDER,         15,   5   ),
                item(Material.ARROW,             2,    0   ),
                item(Material.ROTTEN_FLESH,      0,    1   ),
                item(Material.INK_SAC,           8,    3   ),
                item(Material.GLOW_INK_SAC,      15,   5   ),
                item(Material.ENDER_PEARL,       40,   15  ),
                item(Material.ENDER_EYE,          80,   30  ),
                item(Material.BLAZE_ROD,         50,   20  ),
                item(Material.BLAZE_POWDER,      30,   10  ),
                item(Material.SLIME_BALL,        20,   7   ),
                item(Material.MAGMA_CREAM,       25,   8   ),
                item(Material.GHAST_TEAR,        100,  40  ),
                item(Material.SHULKER_SHELL,     200,  80  ),
                item(Material.PHANTOM_MEMBRANE,  30,   10  ),
                item(Material.PRISMARINE_SHARD,  15,   5   ),
                item(Material.PRISMARINE_CRYSTALS,20,  7   ),
                item(Material.NAUTILUS_SHELL,    80,   30  ),
                item(Material.HEART_OF_THE_SEA,  0,    1000),
                item(Material.NETHER_STAR,       0,    5000),
                item(Material.TOTEM_OF_UNDYING,  0,    500 ),
                item(Material.WITHER_SKELETON_SKULL, 0, 500)
        ));
    }

    // ── 6. Matériaux ─────────────────────────────────────────────────────────

    private ShopCategory materiaux() {
        return new ShopCategory("materiaux", "&7Matériaux & Craft", Material.CRAFTING_TABLE,
            Material.GRAY_STAINED_GLASS_PANE, List.of(
                new ShopItem(Material.STICK,     4,    0,    4),
                item(Material.OAK_PLANKS,        3,    1   ),
                item(Material.BIRCH_PLANKS,      3,    1   ),
                item(Material.SPRUCE_PLANKS,     3,    1   ),
                item(Material.JUNGLE_PLANKS,     4,    1   ),
                item(Material.ACACIA_PLANKS,     3,    1   ),
                item(Material.DARK_OAK_PLANKS,   3,    1   ),
                item(Material.GLASS_PANE,        5,    1   ),
                item(Material.IRON_BARS,         8,    3   ),
                item(Material.CHAIN,             10,   3   ),
                item(Material.FLINT,             3,    1   ),
                item(Material.PAPER,             3,    1   ),
                item(Material.BOOK,              10,   3   ),
                item(Material.NAME_TAG,          50,   0   ),
                item(Material.SADDLE,            100,  30  ),
                item(Material.LEAD,              20,   7   ),
                item(Material.COMPASS,           30,   0   ),
                item(Material.CLOCK,             40,   0   ),
                item(Material.LANTERN,           15,   5   ),
                item(Material.SOUL_LANTERN,      18,   6   ),
                item(Material.TNT,               80,   20  ),
                item(Material.FIREWORK_ROCKET,   10,   0   ),
                item(Material.FLINT_AND_STEEL,   25,   0   ),
                item(Material.FISHING_ROD,       20,   5   ),
                item(Material.BUCKET,            30,   10  ),
                item(Material.WATER_BUCKET,      35,   0   ),
                item(Material.LAVA_BUCKET,       40,   0   )
        ));
    }

    // ── 7. Enchantements ─────────────────────────────────────────────────────

    private ShopCategory enchantements() {
        return new ShopCategory("enchantements", "&5✨ Enchantements", Material.EXPERIENCE_BOTTLE,
            Material.PURPLE_STAINED_GLASS_PANE, List.of(
                item(Material.EXPERIENCE_BOTTLE, 15,   0   ),
                item(Material.LAPIS_LAZULI,      8,    2   ),
                enchBook("Protection IV",        Enchantment.PROTECTION,       4, 400),
                enchBook("Solidité III",         Enchantment.UNBREAKING,       3, 300),
                enchBook("Tranchant V",          Enchantment.SHARPNESS,        5, 450),
                enchBook("Efficacité V",         Enchantment.EFFICIENCY,       5, 500),
                enchBook("Fortune III",          Enchantment.FORTUNE,          3, 600),
                enchBook("Toucher de soie",      Enchantment.SILK_TOUCH,       1, 400),
                enchBook("Puissance V",          Enchantment.POWER,            5, 350),
                enchBook("Chute de plume IV",    Enchantment.FEATHER_FALLING,  4, 300),
                enchBook("Fléau des arthropodes V", Enchantment.BANE_OF_ARTHROPODS, 5, 200),
                enchBook("Feu V",                Enchantment.SMITE,            5, 300),
                enchBook("Aspect igné II",       Enchantment.FIRE_ASPECT,      2, 300),
                enchBook("Butin III",            Enchantment.LOOTING,          3, 400),
                enchBook("Infini",               Enchantment.INFINITY,         1, 350),
                enchBook("Recul II",             Enchantment.KNOCKBACK,        2, 150),
                enchBook("Flamme",               Enchantment.FLAME,            1, 200),
                enchBook("Percussion II",        Enchantment.PUNCH,            2, 200),
                enchBook("Respiration III",      Enchantment.RESPIRATION,      3, 200),
                enchBook("Affinité aquatique",   Enchantment.AQUA_AFFINITY,    1, 150),
                enchBook("Marcheur des abysses III", Enchantment.DEPTH_STRIDER,3, 250),
                enchBook("Réparation",           Enchantment.MENDING,          1, 800)
        ));
    }

    // ── 8. Spécial ───────────────────────────────────────────────────────────

    private ShopCategory special() {
        return new ShopCategory("special", "&e⭐ Spécial & Rare", Material.NETHER_STAR,
            Material.YELLOW_STAINED_GLASS_PANE, List.of(
                item(Material.ELYTRA,            5000, 2000),
                item(Material.END_CRYSTAL,       300,  100 ),
                item(Material.BEACON,            8000, 3000),
                item(Material.CONDUIT,           3000, 1200),
                item(Material.SPONGE,            200,  80  ),
                item(Material.SEA_LANTERN,       30,   10  ),
                item(Material.PRISMARINE,        15,   5   ),
                item(Material.NETHER_STAR,       6000, 2500),
                item(Material.DRAGON_EGG,        0,    50000),
                item(Material.TURTLE_EGG,        150,  50  ),
                item(Material.SUSPICIOUS_SAND,   20,   5   ),
                item(Material.ECHO_SHARD,        0,    200 ),
                item(Material.DISC_FRAGMENT_5,   0,    150 ),
                item(Material.TRIDENT,           0,    400 )
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static ShopItem item(Material mat, double buy, double sell) {
        return new ShopItem(mat, buy, sell, 1);
    }

    private static ShopItem item(Material mat, double buy, double sell, int amount) {
        return new ShopItem(mat, buy, sell, amount);
    }

    private static ShopItem enchBook(String label, Enchantment ench, int level, double buy) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(ench, level, true);
            book.setItemMeta(meta);
        }
        return new ShopItem(book, buy, 0, 1);
    }
}
