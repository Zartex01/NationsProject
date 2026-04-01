package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class FurnaceCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public FurnaceCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            if (!player.hasPermission("nations.grade.premium") && !player.hasPermission("nations.admin")) {
                MessageUtil.sendError(player, "Vous n'avez pas la permission d'utiliser /furnace all.");
                return true;
            }
            smeltAll(player);
            return true;
        }

        smeltHand(player);
        return true;
    }

    private void smeltHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            MessageUtil.sendError(player, "Vous n'avez rien dans la main !");
            return;
        }
        Material smelted = getSmeltResult(item.getType());
        if (smelted == null) {
            MessageUtil.sendError(player, "Cet objet ne peut pas être cuit.");
            return;
        }
        int amount = item.getAmount();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        ItemStack result = new ItemStack(smelted, amount);
        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        leftover.values().forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
        MessageUtil.send(player, "&aCuit &e" + amount + "x " + smelted.name().toLowerCase().replace("_", " ") + " &a!");
    }

    private void smeltAll(Player player) {
        int count = 0;
        Inventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;
            Material smelted = getSmeltResult(item.getType());
            if (smelted == null) continue;
            int amount = item.getAmount();
            contents[i] = null;
            ItemStack result = new ItemStack(smelted, amount);
            java.util.Map<Integer, ItemStack> leftover = inv.addItem(result);
            leftover.values().forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
            count += amount;
        }
        inv.setContents(contents);
        if (count > 0) {
            MessageUtil.send(player, "&aFusionné &e" + count + " &aobjet(s) automatiquement !");
        } else {
            MessageUtil.send(player, "&7Aucun objet fusionnable trouvé dans votre inventaire.");
        }
    }

    private Material getSmeltResult(Material input) {
        return switch (input) {
            // ── Métaux (haut-fourneau) ──────────────────────────────────
            case IRON_ORE, RAW_IRON,
                 DEEPSLATE_IRON_ORE             -> Material.IRON_INGOT;
            case GOLD_ORE, RAW_GOLD,
                 DEEPSLATE_GOLD_ORE,
                 NETHER_GOLD_ORE                -> Material.GOLD_INGOT;
            case COPPER_ORE, RAW_COPPER,
                 DEEPSLATE_COPPER_ORE           -> Material.COPPER_INGOT;
            case ANCIENT_DEBRIS                 -> Material.NETHERITE_SCRAP;
            case RAW_IRON_BLOCK                 -> Material.IRON_BLOCK;
            case RAW_GOLD_BLOCK                 -> Material.GOLD_BLOCK;
            case RAW_COPPER_BLOCK               -> Material.COPPER_BLOCK;

            // ── Minerais divers ─────────────────────────────────────────
            case COAL_ORE,
                 DEEPSLATE_COAL_ORE             -> Material.COAL;
            case NETHER_QUARTZ_ORE              -> Material.QUARTZ;
            case LAPIS_ORE,
                 DEEPSLATE_LAPIS_ORE            -> Material.LAPIS_LAZULI;
            case DIAMOND_ORE,
                 DEEPSLATE_DIAMOND_ORE          -> Material.DIAMOND;
            case EMERALD_ORE,
                 DEEPSLATE_EMERALD_ORE          -> Material.EMERALD;
            case REDSTONE_ORE,
                 DEEPSLATE_REDSTONE_ORE         -> Material.REDSTONE;

            // ── Blocs de construction ───────────────────────────────────
            case SAND, RED_SAND                 -> Material.GLASS;
            case COBBLESTONE                    -> Material.STONE;
            case COBBLED_DEEPSLATE              -> Material.DEEPSLATE;
            case STONE                          -> Material.SMOOTH_STONE;
            case SANDSTONE                      -> Material.SMOOTH_SANDSTONE;
            case RED_SANDSTONE                  -> Material.SMOOTH_RED_SANDSTONE;
            case QUARTZ_BLOCK                   -> Material.SMOOTH_QUARTZ;
            case BASALT                         -> Material.SMOOTH_BASALT;
            case NETHERRACK                     -> Material.NETHER_BRICK;
            case CLAY_BALL                      -> Material.BRICK;
            case CLAY                           -> Material.TERRACOTTA;
            case WET_SPONGE                     -> Material.SPONGE;
            case CACTUS                         -> Material.GREEN_DYE;
            case CHORUS_FRUIT                   -> Material.POPPED_CHORUS_FRUIT;
            case KELP                           -> Material.DRIED_KELP;

            // ── Bois / logs / planches → charbon de bois ────────────────
            case OAK_LOG, OAK_WOOD,
                 STRIPPED_OAK_LOG,
                 STRIPPED_OAK_WOOD,
                 OAK_PLANKS,
                 BIRCH_LOG, BIRCH_WOOD,
                 STRIPPED_BIRCH_LOG,
                 STRIPPED_BIRCH_WOOD,
                 BIRCH_PLANKS,
                 SPRUCE_LOG, SPRUCE_WOOD,
                 STRIPPED_SPRUCE_LOG,
                 STRIPPED_SPRUCE_WOOD,
                 SPRUCE_PLANKS,
                 JUNGLE_LOG, JUNGLE_WOOD,
                 STRIPPED_JUNGLE_LOG,
                 STRIPPED_JUNGLE_WOOD,
                 JUNGLE_PLANKS,
                 ACACIA_LOG, ACACIA_WOOD,
                 STRIPPED_ACACIA_LOG,
                 STRIPPED_ACACIA_WOOD,
                 ACACIA_PLANKS,
                 DARK_OAK_LOG, DARK_OAK_WOOD,
                 STRIPPED_DARK_OAK_LOG,
                 STRIPPED_DARK_OAK_WOOD,
                 DARK_OAK_PLANKS,
                 MANGROVE_LOG, MANGROVE_WOOD,
                 STRIPPED_MANGROVE_LOG,
                 STRIPPED_MANGROVE_WOOD,
                 MANGROVE_PLANKS,
                 CHERRY_LOG, CHERRY_WOOD,
                 STRIPPED_CHERRY_LOG,
                 STRIPPED_CHERRY_WOOD,
                 CHERRY_PLANKS,
                 BAMBOO_BLOCK,
                 STRIPPED_BAMBOO_BLOCK,
                 BAMBOO_PLANKS              -> Material.CHARCOAL;

            // ── Nourriture (fumoir) ─────────────────────────────────────
            case BEEF                           -> Material.COOKED_BEEF;
            case PORKCHOP                       -> Material.COOKED_PORKCHOP;
            case CHICKEN                        -> Material.COOKED_CHICKEN;
            case MUTTON                         -> Material.COOKED_MUTTON;
            case SALMON                         -> Material.COOKED_SALMON;
            case COD                            -> Material.COOKED_COD;
            case RABBIT                         -> Material.COOKED_RABBIT;
            case POTATO                         -> Material.BAKED_POTATO;

            default                             -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("all").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}
