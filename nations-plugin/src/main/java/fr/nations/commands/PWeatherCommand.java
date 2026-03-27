package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class PWeatherCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public PWeatherCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendError(player, "Usage : /pweather <clear|rain|reset>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "clear", "soleil", "sun" -> {
                player.setPlayerWeather(WeatherType.CLEAR);
                MessageUtil.send(player, "&aMétéo personnelle : &eSoleil ☀");
            }
            case "rain", "pluie" -> {
                player.setPlayerWeather(WeatherType.DOWNFALL);
                MessageUtil.send(player, "&aMétéo personnelle : &9Pluie 🌧");
            }
            case "reset", "normal" -> {
                player.resetPlayerWeather();
                MessageUtil.send(player, "&aMétéo personnelle réinitialisée.");
            }
            default -> MessageUtil.sendError(player, "Options : clear, rain, reset");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("clear", "rain", "reset").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}
