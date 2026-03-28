package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class NationPubCommand implements CommandExecutor, TabCompleter {

    private static final double PUB_COST = 1000.0;

    private final NationsPlugin plugin;

    public NationPubCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendError(player, "Usage: &e/npub <message>");
            return true;
        }

        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) {
            MessageUtil.sendError(player, "Vous n'appartenez à aucune nation !");
            return true;
        }

        if (!plugin.getEconomyManager().has(player.getUniqueId(), PUB_COST)) {
            MessageUtil.sendError(player, "Vous n'avez pas assez de coins ! (coût: &e" +
                MessageUtil.formatNumber(PUB_COST) + " coins&c)");
            return true;
        }

        String message = String.join(" ", args);

        if (message.length() > 150) {
            MessageUtil.sendError(player, "Votre message est trop long (max 150 caractères) !");
            return true;
        }

        plugin.getEconomyManager().withdraw(player.getUniqueId(), PUB_COST);
        plugin.getEconomyManager().saveAccountToDatabase(player.getUniqueId());

        String broadcast = MessageUtil.colorize(
            "&6[&e&lNATION PUB&6] &7[&6" + nation.getName() + "&7] &f" + message
        );
        Bukkit.broadcastMessage(broadcast);

        MessageUtil.sendSuccess(player, "Votre pub a été envoyée ! (&c-" +
            MessageUtil.formatNumber(PUB_COST) + " coins&a)");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("<message>");
        return List.of();
    }
}
