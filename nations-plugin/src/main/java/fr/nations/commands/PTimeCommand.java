package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class PTimeCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public PTimeCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendError(player, "Usage : /ptime <day|night|noon|midnight|reset|<ticks>>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "day", "jour"       -> { player.setPlayerTime(1000,  false); MessageUtil.send(player, "&aHeure personnelle : &eJour ☀"); }
            case "noon", "midi"      -> { player.setPlayerTime(6000,  false); MessageUtil.send(player, "&aHeure personnelle : &eMidi"); }
            case "night", "nuit"     -> { player.setPlayerTime(13000, false); MessageUtil.send(player, "&aHeure personnelle : &9Nuit 🌙"); }
            case "midnight", "minuit"-> { player.setPlayerTime(18000, false); MessageUtil.send(player, "&aHeure personnelle : &9Minuit"); }
            case "reset", "normal"   -> { player.resetPlayerTime();           MessageUtil.send(player, "&aHeure personnelle réinitialisée."); }
            default -> {
                try {
                    long ticks = Long.parseLong(args[0]);
                    player.setPlayerTime(ticks, false);
                    MessageUtil.send(player, "&aHeure personnelle définie à : &e" + ticks + " ticks");
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(player, "Options : day, night, noon, midnight, reset ou un nombre de ticks.");
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("day", "night", "noon", "midnight", "reset").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}
