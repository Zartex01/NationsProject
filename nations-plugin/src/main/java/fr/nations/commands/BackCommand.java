package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BackCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public BackCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    public void setLastLocation(UUID playerId, Location location) {
        lastLocations.put(playerId, location);
    }

    public Location getLastLocation(UUID playerId) {
        return lastLocations.get(playerId);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Location last = lastLocations.get(player.getUniqueId());
        if (last == null) {
            MessageUtil.sendError(player, "Aucune position précédente enregistrée.");
            return true;
        }

        Location current = player.getLocation();
        lastLocations.put(player.getUniqueId(), current);
        player.teleport(last);
        MessageUtil.send(player, "&aTéléportation à votre dernière position !");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }
}
