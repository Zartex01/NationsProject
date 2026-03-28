package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.gui.HdvBrowseGui;
import fr.nations.gui.HdvSellGui;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class HdvCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public HdvCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            new HdvBrowseGui(plugin, player, 0).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell", "vendre" -> {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() == Material.AIR) {
                    MessageUtil.sendError(player, "Vous devez tenir un item dans la main pour le vendre !");
                    return true;
                }
                new HdvSellGui(plugin, player, item).open();
            }
            case "browse", "marché", "marche" -> {
                new HdvBrowseGui(plugin, player, 0).open();
            }
            default -> {
                MessageUtil.sendError(player, "Sous-commande inconnue. Utilisez: &e/hdv &7ou &e/hdv sell");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("sell", "browse");
        }
        return List.of();
    }
}
