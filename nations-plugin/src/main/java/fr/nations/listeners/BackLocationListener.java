package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.commands.BackCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BackLocationListener implements Listener {

    private final NationsPlugin plugin;
    private final BackCommand backCommand;

    public BackLocationListener(NationsPlugin plugin, BackCommand backCommand) {
        this.plugin = plugin;
        this.backCommand = backCommand;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        backCommand.setLastLocation(
            event.getEntity().getUniqueId(),
            event.getEntity().getLocation()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getPlayer().hasPermission("nations.grade.chevalier")
                || event.getPlayer().hasPermission("nations.admin")) {
            backCommand.setLastLocation(
                event.getPlayer().getUniqueId(),
                event.getFrom()
            );
        }
    }
}
