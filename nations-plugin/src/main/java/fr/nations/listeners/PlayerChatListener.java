package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    private final NationsPlugin plugin;

    public PlayerChatListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());

        String gradeColor = plugin.getGradeManager().getEffectiveGrade(player).getColor();
        String gradeName  = plugin.getGradeManager().getEffectiveGrade(player).getDisplayName();

        String prefix = "§7[" + gradeColor + gradeName + "§7] ";

        if (nation != null) {
            prefix = "§7[§6" + nation.getName() + "§7] " + prefix;
        }

        event.setFormat(prefix + "§f" + player.getName() + " §8» §f%2$s");
    }
}
