package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class NickCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_NICK_LENGTH = 16;
    private final NationsPlugin plugin;

    public NickCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendError(player, "Usage : /nick <pseudo> | /nick reset");
            return true;
        }

        if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("off")) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            MessageUtil.send(player, "&aPseudo réinitialisé à : &e" + player.getName());
            return true;
        }

        String nick = args[0];

        if (nick.length() > MAX_NICK_LENGTH) {
            MessageUtil.sendError(player, "Le pseudo ne peut pas dépasser &e" + MAX_NICK_LENGTH + " &ccaractères.");
            return true;
        }

        if (!nick.matches("[a-zA-Z0-9_§&]+")) {
            MessageUtil.sendError(player, "Le pseudo ne peut contenir que des lettres, chiffres, underscores et codes couleur.");
            return true;
        }

        String colored = nick.replace("&", "§");
        player.setDisplayName(colored);
        player.setPlayerListName(colored.length() > 16 ? colored.substring(0, 16) : colored);
        MessageUtil.send(player, "&aPseudo changé en : " + colored);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("reset");
        return List.of();
    }
}
