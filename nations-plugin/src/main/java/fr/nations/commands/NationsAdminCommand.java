package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class NationsAdminCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public NationsAdminCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nations.admin")) {
            sender.sendMessage(MessageUtil.colorize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getConfigManager().reload();
                plugin.getDataManager().saveAll();
                plugin.getDataManager().loadAll();
                MessageUtil.sendSuccess(sender, "Plugin rechargé.");
            }
            case "save" -> {
                plugin.getDataManager().saveAll();
                MessageUtil.sendSuccess(sender, "Données sauvegardées.");
            }
            case "nation" -> handleNationAdmin(sender, args);
            case "player", "joueur" -> handlePlayerAdmin(sender, args);
            case "setgrade", "setrang" -> handleSetGrade(sender, args);
            case "setlevel", "setniveau" -> handleSetLevel(sender, args);
            case "addxp" -> handleAddXp(sender, args);
            case "stats" -> handleStats(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleNationAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: /nadmin nation <disband|addmoney> <nation> [valeur]");
            return;
        }
        Nation nation = plugin.getNationManager().getNationByName(args[2]);
        if (nation == null) { MessageUtil.sendError(sender, "Nation introuvable."); return; }

        switch (args[1].toLowerCase()) {
            case "disband" -> {
                plugin.getNationManager().disbandNation(nation.getId());
                MessageUtil.sendSuccess(sender, "Nation §6" + args[2] + " §adissoute.");
            }
            case "addmoney", "addcoins" -> {
                if (args.length < 4) { MessageUtil.sendError(sender, "Montant requis."); return; }
                try {
                    double amount = Double.parseDouble(args[3]);
                    nation.depositToBank(amount);
                    plugin.getDataManager().saveNations();
                    MessageUtil.sendSuccess(sender, "§e" + MessageUtil.formatNumber(amount) + " coins §aajoutés à la banque de §6" + nation.getName() + "§a.");
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(sender, "Montant invalide.");
                }
            }
            case "points" -> {
                if (args.length < 4) { MessageUtil.sendError(sender, "Points requis."); return; }
                try {
                    int points = Integer.parseInt(args[3]);
                    nation.addSeasonPoints(points);
                    plugin.getDataManager().saveNations();
                    MessageUtil.sendSuccess(sender, "§e" + points + " pts §aajoutés à §6" + nation.getName() + "§a.");
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(sender, "Nombre invalide.");
                }
            }
        }
    }

    private void handlePlayerAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: /nadmin player <joueur> <info>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) { MessageUtil.sendError(sender, "Joueur introuvable."); return; }

        Nation nation = plugin.getNationManager().getPlayerNation(target.getUniqueId());
        var grade = plugin.getGradeManager().getOrCreatePlayerGrade(target.getUniqueId(), target.getName());
        double balance = plugin.getEconomyManager().getBalance(target.getUniqueId());

        MessageUtil.sendSeparator(sender);
        MessageUtil.sendRaw(sender, "  §7Joueur: §f" + target.getName());
        MessageUtil.sendRaw(sender, "  §7Nation: " + (nation != null ? "§6" + nation.getName() : "§7Aucune"));
        MessageUtil.sendRaw(sender, "  §7Grade: §e" + grade.getGradeName());
        MessageUtil.sendRaw(sender, "  §7Niveau: §e" + grade.getLevel() + " (XP: " + grade.getXp() + ")");
        MessageUtil.sendRaw(sender, "  §7Balance: §e" + MessageUtil.formatNumber(balance) + " coins");
        MessageUtil.sendRaw(sender, "  §7Claims: §f" + grade.getClaimCount());
        MessageUtil.sendSeparator(sender);
    }

    private void handleSetGrade(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: /nadmin setgrade <joueur> <grade>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(sender, "Joueur introuvable."); return; }

        String gradeName = args[2].toUpperCase();
        try {
            GradeType.valueOf(gradeName);
        } catch (IllegalArgumentException e) {
            MessageUtil.sendError(sender, "Grade invalide. Grades: JOUEUR, SOUTIEN, PREMIUM, CHEVALIER, ROI");
            return;
        }
        var grade = plugin.getGradeManager().getOrCreatePlayerGrade(target.getUniqueId(), target.getName());
        grade.setGradeName(gradeName);
        plugin.getDataManager().savePlayers();
        MessageUtil.sendSuccess(sender, "Grade de §f" + target.getName() + " §afixé à §e" + gradeName + "§a.");
        MessageUtil.send(target, "§aVotre grade a été mis à jour: §e" + gradeName);
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: /nadmin setlevel <joueur> <niveau>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(sender, "Joueur introuvable."); return; }
        try {
            int level = Integer.parseInt(args[2]);
            if (level < 1 || level > plugin.getConfigManager().getMaxLevel()) {
                MessageUtil.sendError(sender, "Niveau invalide (1-" + plugin.getConfigManager().getMaxLevel() + ").");
                return;
            }
            var grade = plugin.getGradeManager().getOrCreatePlayerGrade(target.getUniqueId(), target.getName());
            grade.setLevel(level);
            grade.setXp(0);
            plugin.getDataManager().savePlayers();
            MessageUtil.sendSuccess(sender, "Niveau de §f" + target.getName() + " §afixé à §e" + level + "§a.");
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Niveau invalide.");
        }
    }

    private void handleAddXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: /nadmin addxp <joueur> <montant>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(sender, "Joueur introuvable."); return; }
        try {
            long xp = Long.parseLong(args[2]);
            plugin.getGradeManager().addXp(target.getUniqueId(), xp);
            MessageUtil.sendSuccess(sender, "§e" + xp + " XP §aajoutés à §f" + target.getName() + "§a.");
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Montant invalide.");
        }
    }

    private void handleStats(CommandSender sender) {
        MessageUtil.sendSeparator(sender);
        MessageUtil.sendTitle(sender, "Statistiques du serveur");
        MessageUtil.sendRaw(sender, "  §7Nations: §f" + plugin.getNationManager().getAllNations().size());
        MessageUtil.sendRaw(sender, "  §7Coalitions: §f" + plugin.getNationManager().getAllCoalitions().size());
        MessageUtil.sendRaw(sender, "  §7Claims: §f" + plugin.getTerritoryManager().getAllClaims().size());
        MessageUtil.sendRaw(sender, "  §7Guerres actives: §f" + plugin.getWarManager().getAllWars().stream().filter(w -> w.getStatus().isActive()).count());
        MessageUtil.sendRaw(sender, "  §7Guerres en attente: §f" + plugin.getWarManager().getPendingWars().size());
        MessageUtil.sendRaw(sender, "  §7Saison: §e" + plugin.getSeasonManager().getCurrentSeason());
        MessageUtil.sendRaw(sender, "  §7Joueurs: §f" + plugin.getEconomyManager().getAllAccounts().size());
        MessageUtil.sendSeparator(sender);
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendSeparator(sender);
        MessageUtil.sendTitle(sender, "Admin - Aide");
        MessageUtil.sendRaw(sender, "  §e/nadmin reload §7— Recharger la config");
        MessageUtil.sendRaw(sender, "  §e/nadmin save §7— Sauvegarder les données");
        MessageUtil.sendRaw(sender, "  §e/nadmin nation <action> <nom> §7— Gérer une nation");
        MessageUtil.sendRaw(sender, "  §e/nadmin player <joueur> §7— Infos joueur");
        MessageUtil.sendRaw(sender, "  §e/nadmin setgrade <joueur> <grade> §7— Changer le grade");
        MessageUtil.sendRaw(sender, "  §e/nadmin setlevel <joueur> <niveau> §7— Changer le niveau");
        MessageUtil.sendRaw(sender, "  §e/nadmin addxp <joueur> <xp> §7— Ajouter de l'XP");
        MessageUtil.sendRaw(sender, "  §e/nadmin stats §7— Statistiques globales");
        MessageUtil.sendSeparator(sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nations.admin")) return Collections.emptyList();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (String s : Arrays.asList("reload", "save", "nation", "player", "setgrade", "setlevel", "addxp", "stats")) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "nation" -> {
                    for (String s : Arrays.asList("disband", "addmoney", "points")) {
                        if (s.startsWith(args[1].toLowerCase())) completions.add(s);
                    }
                }
                case "player", "setgrade", "setlevel", "addxp" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "nation" -> {
                    for (Nation n : plugin.getNationManager().getAllNations()) {
                        if (n.getName().toLowerCase().startsWith(args[2].toLowerCase())) completions.add(n.getName());
                    }
                }
                case "setgrade" -> {
                    for (GradeType g : GradeType.values()) {
                        if (g.name().toLowerCase().startsWith(args[2].toLowerCase())) completions.add(g.name());
                    }
                }
                case "setlevel" -> completions.addAll(Arrays.asList("1", "10", "25", "50", "100"));
                case "addxp" -> completions.addAll(Arrays.asList("100", "500", "1000", "5000", "10000"));
            }
        }
        return completions;
    }
}
