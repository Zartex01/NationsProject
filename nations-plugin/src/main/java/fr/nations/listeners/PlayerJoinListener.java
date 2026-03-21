package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
import fr.nations.grade.PlayerGrade;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final NationsPlugin plugin;

    public PlayerJoinListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getEconomyManager().getOrCreateAccount(player.getUniqueId());
        PlayerGrade grade = plugin.getGradeManager().getOrCreatePlayerGrade(player.getUniqueId(), player.getName());
        grade.setGradeName(GradeType.fromPermission(player).name());
        plugin.getSeasonManager().getOrCreatePlayerStats(player.getUniqueId());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
            int pendingWars = 0;
            if (nation != null) {
                pendingWars = (int) plugin.getWarManager().getAllWars().stream()
                    .filter(w -> w.getStatus().isPending() && w.isNationInvolved(nation.getId()))
                    .count();
            }

            MessageUtil.sendRaw(player, "");
            MessageUtil.send(player, "§6Bienvenue §f" + player.getName() + "§6 sur §eNationsEpoque§6!");
            MessageUtil.send(player, "§7Grade: " + GradeType.fromPermission(player).getColoredDisplay() + " §7| Niveau: §e" + grade.getLevel());
            if (nation != null) {
                MessageUtil.send(player, "§7Nation: §6" + nation.getName() + " §7| Points: §e" + nation.getSeasonPoints());
                if (pendingWars > 0) {
                    MessageUtil.send(player, "§c⚔ Votre nation a §f" + pendingWars + " §cguerre(s) en attente de validation staff!");
                }
            } else {
                MessageUtil.send(player, "§7Vous n'avez pas de nation. Tapez §e/nation create <nom> §7pour commencer!");
            }
            MessageUtil.sendRaw(player, "");
        }, 20L);

        plugin.getDataManager().savePlayers();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getDataManager().saveAll();
    }
}
