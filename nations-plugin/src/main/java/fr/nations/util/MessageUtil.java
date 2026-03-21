package fr.nations.util;

import fr.nations.NationsPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {

    private static NationsPlugin plugin;

    public static void init(NationsPlugin p) {
        plugin = p;
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(colorize(plugin.getConfigManager().getPrefix() + message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    public static void sendError(CommandSender sender, String message) {
        sender.sendMessage(colorize(plugin.getConfigManager().getPrefix() + "&c" + message));
    }

    public static void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(colorize(plugin.getConfigManager().getPrefix() + "&a" + message));
    }

    public static void sendInfo(CommandSender sender, String message) {
        sender.sendMessage(colorize(plugin.getConfigManager().getPrefix() + "&7" + message));
    }

    public static void sendSeparator(CommandSender sender) {
        sendRaw(sender, "&8&m                                    ");
    }

    public static void sendTitle(CommandSender sender, String title) {
        sendRaw(sender, "&8&m        &r &6&l" + title + " &8&m        ");
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    public static String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "j " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "min";
        if (minutes > 0) return minutes + "min";
        return seconds + "s";
    }

    public static String formatNumber(double number) {
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000);
        return String.format("%.0f", number);
    }
}
