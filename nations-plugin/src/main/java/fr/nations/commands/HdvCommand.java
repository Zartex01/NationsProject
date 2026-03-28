package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.gui.HdvGui;
import fr.nations.gui.HdvSellGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class HdvCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public HdvCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("sell")) {
            new HdvSellGui(plugin, player).open();
        } else {
            new HdvGui(plugin, player, 0).open();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1)
            return List.of("sell").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        return List.of();
    }
}
