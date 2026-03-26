package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.economy.PlayerAccount;
import fr.nations.grade.PlayerGrade;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public BalanceCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            showBalance(player, player);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendError(player, "Joueur introuvable ou hors ligne.");
            return true;
        }

        showBalance(player, target);
        return true;
    }

    private void showBalance(Player viewer, Player target) {
        double balance = plugin.getEconomyManager().getBalance(target.getUniqueId());
        PlayerGrade grade = plugin.getGradeManager().getOrCreatePlayerGrade(target.getUniqueId(), target.getName());

        MessageUtil.sendSeparator(viewer);
        if (viewer.equals(target)) {
            MessageUtil.sendTitle(viewer, "Votre compte");
        } else {
            MessageUtil.sendTitle(viewer, "Compte de " + target.getName());
        }
        MessageUtil.sendRaw(viewer, "  §7Balance:  §e" + MessageUtil.formatNumber(balance) + " §6coins");
        MessageUtil.sendRaw(viewer, "  §7Niveau:   §e" + grade.getLevel());
        MessageUtil.sendRaw(viewer, "  §7XP:       §e" + grade.getXp() + " §7/ §e" + grade.getXpForNextLevel());
        MessageUtil.sendSeparator(viewer);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}
