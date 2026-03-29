package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.gui.EpoqueGui;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class EpoqueCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public EpoqueCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) {
            MessageUtil.sendError(player, "Vous devez appartenir à une nation pour accéder aux Époques.");
            return true;
        }

        if (plugin.getEpoqueManager().getTotalLevels() == 0) {
            MessageUtil.sendError(player, "Aucun niveau d'époque configuré. Contactez un administrateur.");
            return true;
        }

        new EpoqueGui(plugin, player, nation).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
