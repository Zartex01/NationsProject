package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
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

        if (args.length > 0 && args[0].equalsIgnoreCase("hand")) {
            smeltHand(player);
            return true;
        }

        player.openInventory(Bukkit.createInventory(player, InventoryType.FURNACE, "§6Fourneau"));
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
            MessageUtil.sendError(player, "&cCet objet ne peut pas être cuit : &7" + item.getType().name());
            return;
        }
        int amount = item.getAmount();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        ItemStack result = new ItemStack(smelted, amount);
        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        leftover.values().forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
        MessageUtil.send(player, "&aCuit &e" + amount + "x " + smelted.name().toLowerCase().replace("_", " ") + " &adepuis votre main !");
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
            case IRON_ORE, RAW_IRON             -> Material.IRON_INGOT;
            case GOLD_ORE, RAW_GOLD             -> Material.GOLD_INGOT;
            case COPPER_ORE, RAW_COPPER         -> Material.COPPER_INGOT;
            case ANCIENT_DEBRIS                 -> Material.NETHERITE_SCRAP;
            case SAND                           -> Material.GLASS;
            case COBBLESTONE                    -> Material.STONE;
            case BEEF                           -> Material.COOKED_BEEF;
            case PORKCHOP                       -> Material.COOKED_PORKCHOP;
            case CHICKEN                        -> Material.COOKED_CHICKEN;
            case MUTTON                         -> Material.COOKED_MUTTON;
            case SALMON                         -> Material.COOKED_SALMON;
            case COD                            -> Material.COOKED_COD;
            case RABBIT                         -> Material.COOKED_RABBIT;
            case POTATO                         -> Material.BAKED_POTATO;
            case KELP                           -> Material.DRIED_KELP;
            case WET_SPONGE                     -> Material.SPONGE;
            default                             -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("hand", "all").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}
