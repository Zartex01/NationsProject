package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.gui.FurnaceGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FurnaceCommand implements CommandExecutor {

    private final NationsPlugin plugin;

    public FurnaceCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Joueurs uniquement.");
            return true;
        }
        new FurnaceGui(plugin, player).open();
        return true;
    }
}
