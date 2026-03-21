package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final NationsPlugin plugin;

    public PlayerDeathListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        plugin.getSeasonManager().addPlayerStat(victim.getUniqueId(), "deaths", 1);

        if (killer == null || killer.equals(victim)) return;

        Nation killerNation = plugin.getNationManager().getPlayerNation(killer.getUniqueId());
        Nation victimNation = plugin.getNationManager().getPlayerNation(victim.getUniqueId());

        if (killerNation != null && victimNation != null) {
            if (killerNation.getId().equals(victimNation.getId())) return;

            plugin.getWarManager().recordKill(killer.getUniqueId(), victim.getUniqueId());

            if (plugin.getWarManager().areNationsAtWar(killerNation.getId(), victimNation.getId())) {
                var war = plugin.getWarManager().getActiveWarBetween(killerNation.getId(), victimNation.getId());
                if (war != null) {
                    MessageUtil.send(killer, "§c+1 Kill §7de guerre pour §6" + killerNation.getName() + "§7!");
                }
            }
        } else {
            plugin.getGradeManager().addXp(killer.getUniqueId(), plugin.getConfigManager().getXpPerKill());
            plugin.getSeasonManager().addPlayerStat(killer.getUniqueId(), "kills", 1);
        }
    }
}
