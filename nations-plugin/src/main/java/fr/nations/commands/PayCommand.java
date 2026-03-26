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

public class PayCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public PayCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /pay <joueur> <montant>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendError(player, "Joueur introuvable ou hors ligne.");
            return true;
        }

        if (target.equals(player)) {
            MessageUtil.sendError(player, "Vous ne pouvez pas vous payer vous-même.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Montant invalide. Entrez un nombre positif.");
            return true;
        }

        if (amount <= 0) {
            MessageUtil.sendError(player, "Le montant doit être supérieur à 0.");
            return true;
        }

        if (amount != Math.floor(amount) && String.valueOf(amount).split("\\.")[1].length() > 2) {
            MessageUtil.sendError(player, "Le montant ne peut pas avoir plus de 2 décimales.");
            return true;
        }

        double senderBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        if (senderBalance < amount) {
            MessageUtil.sendError(player, "Fonds insuffisants. Votre balance: §e"
                + MessageUtil.formatNumber(senderBalance) + " coins§c.");
            return true;
        }

        boolean success = plugin.getEconomyManager().transfer(player.getUniqueId(), target.getUniqueId(), amount);
        if (!success) {
            MessageUtil.sendError(player, "Le virement a échoué. Vérifiez votre balance.");
            return true;
        }

        MessageUtil.sendSuccess(player, "Vous avez envoyé §e" + MessageUtil.formatNumber(amount)
            + " coins §aà §f" + target.getName() + "§a.");
        MessageUtil.sendSuccess(player, "Nouvelle balance: §e"
            + MessageUtil.formatNumber(plugin.getEconomyManager().getBalance(player.getUniqueId())) + " coins§a.");

        MessageUtil.send(target, "§f" + player.getName() + " §avous a envoyé §e"
            + MessageUtil.formatNumber(amount) + " coins§a.");
        MessageUtil.send(target, "§7Nouvelle balance: §e"
            + MessageUtil.formatNumber(plugin.getEconomyManager().getBalance(target.getUniqueId())) + " coins§7.");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("10", "50", "100", "500", "1000"));
        }
        return completions;
    }
}
