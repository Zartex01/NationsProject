package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class SellCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public SellCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("hand")) {
            sellHand(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            if (!player.hasPermission("nations.sell.all") && !player.hasPermission("nations.admin")) {
                MessageUtil.sendError(player, "Vous n'avez pas la permission. &7(Grade &6Premium &7requis)");
                return true;
            }
            sellAll(player);
            return true;
        }

        MessageUtil.sendError(player, "Usage : &e/sell hand &7ou &e/sell all");
        return true;
    }

    private void sellHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            MessageUtil.sendError(player, "Vous ne tenez rien dans la main.");
            return;
        }
        double price = plugin.getShopManager().getSellPrice(hand.getType());
        if (price <= 0) {
            MessageUtil.sendError(player, "Cet objet n'est pas vendable dans le shop.");
            return;
        }
        int amount = hand.getAmount();
        double total = price * amount;
        player.getInventory().setItemInMainHand(null);
        plugin.getEconomyManager().deposit(player.getUniqueId(), total);
        MessageUtil.send(player,
            "&aVendu &e" + amount + "x " + formatMaterial(hand.getType())
            + " &apour &e" + MessageUtil.formatNumber(total) + " coins&a !");
    }

    private void sellAll(Player player) {
        double total = 0;
        int itemCount = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;
            double price = plugin.getShopManager().getSellPrice(item.getType());
            if (price <= 0) continue;
            total += price * item.getAmount();
            itemCount += item.getAmount();
            contents[i] = null;
        }

        if (itemCount == 0) {
            MessageUtil.send(player, "&7Aucun objet vendable trouvé dans votre inventaire.");
            return;
        }

        player.getInventory().setContents(contents);
        plugin.getEconomyManager().deposit(player.getUniqueId(), total);
        MessageUtil.send(player,
            "&aVendu &e" + itemCount + " &aobjet(s) pour &e" + MessageUtil.formatNumber(total) + " coins &a!");
    }

    public static String formatMaterial(Material mat) {
        String raw = mat.name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty())
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1)
            return List.of("hand", "all").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        return List.of();
    }
}
