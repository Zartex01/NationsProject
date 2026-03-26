package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
import fr.nations.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class GradeCommandListener implements Listener {

    private final NationsPlugin plugin;

    public GradeCommandListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("nations.admin") || player.hasPermission("nations.bypass")) return;

        String message = event.getMessage();
        if (!message.startsWith("/")) return;
        String commandName = message.split(" ")[0].substring(1).toLowerCase();

        GradeType requiredGrade = getRequiredGrade(commandName);
        if (requiredGrade == null) return;

        GradeType playerGrade = plugin.getGradeManager().getEffectiveGrade(player);

        if (playerGrade.ordinal() < requiredGrade.ordinal()) {
            event.setCancelled(true);
            MessageUtil.sendError(player,
                "Cette commande nécessite le grade " + requiredGrade.getColoredDisplay()
                + " &cou supérieur. Votre grade actuel: " + playerGrade.getColoredDisplay() + "&c.");
        }
    }

    private GradeType getRequiredGrade(String commandName) {
        for (GradeType grade : GradeType.values()) {
            if (grade == GradeType.JOUEUR) continue;
            List<String> cmds = plugin.getConfigManager().getGradeCommands(grade.name().toLowerCase());
            if (cmds.stream().anyMatch(c -> c.equalsIgnoreCase(commandName))) {
                return grade;
            }
        }
        return null;
    }
}
