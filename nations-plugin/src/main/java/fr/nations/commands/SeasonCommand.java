package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.gui.SeasonGui;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class SeasonCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public SeasonCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            new SeasonGui(plugin, player).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(player);
            case "top" -> handleTop(player);
            case "reset" -> {
                if (!player.hasPermission("nations.admin")) {
                    MessageUtil.sendError(player, "Permission insuffisante.");
                    return true;
                }
                plugin.getSeasonManager().endSeason();
                MessageUtil.sendSuccess(player, "Saison réinitialisée. Nouvelle saison: §6" + plugin.getSeasonManager().getCurrentSeason());
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleInfo(Player player) {
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Saison " + plugin.getSeasonManager().getCurrentSeason());
        MessageUtil.sendRaw(player, "  §7Temps restant: §e" + plugin.getSeasonManager().getFormattedSeasonTimeRemaining());
        MessageUtil.sendRaw(player, "  §7Récompenses:");
        MessageUtil.sendRaw(player, "    §6🥇 1er: §e" + MessageUtil.formatNumber(plugin.getConfigManager().getSeasonFirstPlaceReward()) + " coins");
        MessageUtil.sendRaw(player, "    §7🥈 2ème: §e" + MessageUtil.formatNumber(plugin.getConfigManager().getSeasonSecondPlaceReward()) + " coins");
        MessageUtil.sendRaw(player, "    §c🥉 3ème: §e" + MessageUtil.formatNumber(plugin.getConfigManager().getSeasonThirdPlaceReward()) + " coins");
        MessageUtil.sendSeparator(player);
    }

    private void handleTop(Player player) {
        List<Nation> sorted = plugin.getNationManager().getNationsSortedByPoints();
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Classement Saison");
        if (sorted.isEmpty()) {
            MessageUtil.sendRaw(player, "  §7Aucune nation.");
        } else {
            for (int i = 0; i < Math.min(10, sorted.size()); i++) {
                Nation n = sorted.get(i);
                String medal = switch (i) {
                    case 0 -> "§6🥇";
                    case 1 -> "§7🥈";
                    case 2 -> "§c🥉";
                    default -> "§7#" + (i + 1);
                };
                MessageUtil.sendRaw(player, "  " + medal + " §6" + n.getName() + " §7— §e" + n.getSeasonPoints() + " pts §7(" + n.getMemberCount() + " membres)");
            }
        }
        MessageUtil.sendSeparator(player);
    }

    private void sendHelp(Player player) {
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Saison - Aide");
        MessageUtil.sendRaw(player, "  §e/season §7— Interface du classement");
        MessageUtil.sendRaw(player, "  §e/season info §7— Informations sur la saison");
        MessageUtil.sendRaw(player, "  §e/season top §7— Classement des nations");
        if (player.hasPermission("nations.admin")) {
            MessageUtil.sendRaw(player, "  §e/season reset §7— Forcer la fin de saison (admin)");
        }
        MessageUtil.sendSeparator(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("info", "top"));
            if (sender.hasPermission("nations.admin")) subs.add("reset");
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        }
        return completions;
    }
}
