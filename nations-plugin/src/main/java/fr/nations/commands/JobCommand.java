package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.gui.JobsGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobCommand implements CommandExecutor {

    private final NationsPlugin plugin;

    public JobCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Joueurs uniquement.");
            return true;
        }
        // Charger les données si pas encore en cache
        if (!plugin.getJobManager().isLoaded(player.getUniqueId())) {
            plugin.getJobManager().loadPlayer(player.getUniqueId());
        }
        new JobsGui(plugin, player).open();
        return true;
    }
}
