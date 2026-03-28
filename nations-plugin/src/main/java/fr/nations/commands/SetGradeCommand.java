package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
import fr.nations.grade.PlayerGrade;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SetGradeCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public SetGradeCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nations.admin")) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize("&eUsage: /setgrade <joueur> <grade>"));
            sender.sendMessage(MessageUtil.colorize("&7Grades: &f" + gradeList()));
            return true;
        }

        String targetName = args[0];
        String gradeName  = args[1].toUpperCase();

        GradeType grade;
        try {
            grade = GradeType.valueOf(gradeName);
        } catch (IllegalArgumentException e) {
            MessageUtil.sendError(sender, "Grade invalide. Grades disponibles: " + gradeList());
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetId;
        String displayName;

        if (target != null) {
            targetId    = target.getUniqueId();
            displayName = target.getName();
        } else {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (!offline.hasPlayedBefore()) {
                MessageUtil.sendError(sender, "Joueur introuvable: " + targetName);
                return true;
            }
            targetId    = offline.getUniqueId();
            displayName = offline.getName() != null ? offline.getName() : targetName;
        }

        PlayerGrade playerGrade = plugin.getGradeManager().getOrCreatePlayerGrade(targetId, displayName);
        playerGrade.setGradeName(grade.name());
        plugin.getGradeManager().saveGradeToDatabase(targetId);

        MessageUtil.sendSuccess(sender, "Grade de &e" + displayName + " &adefini à " + grade.getColoredDisplay() + "&a.");

        if (target != null) {
            MessageUtil.send(target, "&aVotre grade a été défini à " + grade.getColoredDisplay() + " &apar un administrateur.");
            plugin.getGradeManager().updateTabDisplay(target);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nations.admin")) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String prefix = args[1].toUpperCase();
            return Arrays.stream(GradeType.values())
                .map(GradeType::name)
                .filter(n -> n.startsWith(prefix))
                .collect(Collectors.toList());
        }
        return List.of();
    }

    private String gradeList() {
        return Arrays.stream(GradeType.values())
            .map(g -> g.getColoredDisplay())
            .collect(Collectors.joining("&7, "));
    }
}
