package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AtmCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public AtmCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        long claimableMinutes = plugin.getAtmManager().getClaimableMinutes(player.getUniqueId());
        long reward = plugin.getAtmManager().getRewardForMinutes(claimableMinutes);

        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "ATM - Retrait de salaire");

        if (claimableMinutes <= 0) {
            MessageUtil.sendRaw(player, "  §7Vous n'avez pas de temps de jeu à convertir.");
            MessageUtil.sendRaw(player, "  §7Revenez après avoir joué quelques minutes !");
            MessageUtil.sendSeparator(player);
            return true;
        }

        double currentBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());

        MessageUtil.sendRaw(player, "  §7Temps de jeu disponible: §e" + claimableMinutes + " min");
        MessageUtil.sendRaw(player, "  §7Récompense (x2):          §a+" + MessageUtil.formatNumber(reward) + " coins");
        MessageUtil.sendRaw(player, "  §7Balance actuelle:         §6" + MessageUtil.formatNumber(currentBalance) + " coins");

        boolean success = plugin.getAtmManager().claimReward(player.getUniqueId());

        if (success) {
            double newBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            MessageUtil.sendRaw(player, "");
            MessageUtil.sendSuccess(player, "§a§lVirement effectué ! §fNouvelle balance: §e" + MessageUtil.formatNumber(newBalance) + " coins");
        } else {
            MessageUtil.sendError(player, "Une erreur est survenue lors du retrait. Réessayez plus tard.");
        }

        MessageUtil.sendSeparator(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
