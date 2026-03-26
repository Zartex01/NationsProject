package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GiveMoneyCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public GiveMoneyCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nations.admin")) {
            if (sender instanceof Player p) MessageUtil.sendError(p, "Permission insuffisante.");
            else sender.sendMessage("Permission insuffisante.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize(
                plugin.getConfigManager().getPrefix() + "&cUsage: /givemoney <joueur> <montant>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.colorize(
                plugin.getConfigManager().getPrefix() + "&cJoueur introuvable ou hors ligne."));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize(
                plugin.getConfigManager().getPrefix() + "&cMontant invalide. Entrez un nombre positif."));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtil.colorize(
                plugin.getConfigManager().getPrefix() + "&cLe montant doit être supérieur à 0. Les virements négatifs sont interdits."));
            return true;
        }

        plugin.getEconomyManager().deposit(target.getUniqueId(), amount);
        double newBalance = plugin.getEconomyManager().getBalance(target.getUniqueId());

        sender.sendMessage(MessageUtil.colorize(
            plugin.getConfigManager().getPrefix() + "&a§e" + MessageUtil.formatNumber(amount)
            + " coins &adonnés à &f" + target.getName()
            + "&a. Sa nouvelle balance: &e" + MessageUtil.formatNumber(newBalance) + " coins&a."));

        MessageUtil.send(target, "&aL'administration vous a donné &e"
            + MessageUtil.formatNumber(amount) + " coins&a.");
        MessageUtil.send(target, "&7Nouvelle balance: &e"
            + MessageUtil.formatNumber(newBalance) + " coins&7.");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nations.admin")) return List.of();
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                    completions.add(p.getName());
            }
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("100", "500", "1000", "5000", "10000", "50000"));
        }
        return completions;
    }
}
