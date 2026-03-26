package fr.nations.listeners;

import fr.nations.NationsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AtmPlaytimeListener implements Listener {

    private final NationsPlugin plugin;

    public AtmPlaytimeListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getAtmManager().onPlayerJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getAtmManager().onPlayerQuit(event.getPlayer().getUniqueId());
    }
}
