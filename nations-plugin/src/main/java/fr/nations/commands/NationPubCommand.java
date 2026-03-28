package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class NationPubCommand implements CommandExecutor, TabCompleter {

    private static final double COST = 1000.0;

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
            MessageUtil.sendError(player, "Usage : /npub <message>");
            return true;
        }

        UUID uuid = player.getUniqueId();

        Nation nation = plugin.getNationManager().getPlayerNation(uuid);
        if (nation == null) {
            MessageUtil.sendError(player, "Vous devez appartenir à une nation pour faire une publicité.");
            return true;
        }

        if (!plugin.getEconomyManager().has(uuid, COST)) {
            MessageUtil.sendError(player, "Fonds insuffisants ! Il vous faut &e"
                + MessageUtil.formatNumber(COST) + " coins &cpour faire une pub.");
            return true;
        }

        String message = String.join(" ", args);
        if (message.length() > 150) {
            MessageUtil.sendError(player, "Message trop long (max 150 caractères).");
            return true;
        }

        plugin.getEconomyManager().withdraw(uuid, COST);

        plugin.getServer().broadcastMessage(MessageUtil.colorize(
            "&6&l[PUB] &e[" + nation.getName() + "] &r" + player.getName()
            + " &8: &f" + message
        ));

        MessageUtil.send(player,
            "&aPub envoyée ! &8(&c-" + MessageUtil.formatNumber(COST) + " coins&8)");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
