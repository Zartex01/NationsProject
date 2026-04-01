package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.economy.PlayerAccount;
import fr.nations.grade.PlayerGrade;
import fr.nations.nation.Nation;
import fr.nations.season.PlayerStats;
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

        PlayerAccount account = plugin.getEconomyManager().getOrCreateAccount(player.getUniqueId());
        account.setPlayerName(player.getName());

        PlayerGrade grade = plugin.getGradeManager().getOrCreatePlayerGrade(player.getUniqueId(), player.getName());
        grade.setGradeName(plugin.getGradeManager().getEffectiveGrade(player).name());
        grade.setPlayerName(player.getName());

        PlayerStats stats = plugin.getSeasonManager().getOrCreatePlayerStats(player.getUniqueId());
        stats.setPlayerName(player.getName());

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
            MessageUtil.send(player, "§7Grade: " + plugin.getGradeManager().getEffectiveGrade(player).getColoredDisplay() + " §7| Niveau: §e" + grade.getLevel());
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

        if (plugin.getDatabaseManager().isConnected()) {
            plugin.getEconomyManager().saveAccountToDatabase(player.getUniqueId());
            plugin.getGradeManager().saveGradeToDatabase(player.getUniqueId());
            plugin.getSeasonManager().savePlayerStatToDatabase(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDatabaseManager().isConnected()) {
            plugin.getEconomyManager().saveAccountToDatabase(player.getUniqueId());
            plugin.getGradeManager().saveGradeToDatabase(player.getUniqueId());
            plugin.getSeasonManager().savePlayerStatToDatabase(player.getUniqueId());
            fr.nations.nation.Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
            if (nation != null) {
                plugin.getNationManager().saveNationToDatabase(nation);
                fr.nations.nation.NationMember member = nation.getMember(player.getUniqueId());
                if (member != null) {
                    plugin.getNationManager().saveMemberToDatabase(nation.getId(), member);
                }
            }
        }
    }
}
