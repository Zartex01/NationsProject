package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.economy.PlayerAccount;
import fr.nations.grade.PlayerGrade;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class MoneyCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public MoneyCommand(NationsPlugin plugin) {
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

        switch (args[0].toLowerCase()) {
            case "balance", "bal" -> {
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) { MessageUtil.sendError(player, "Joueur introuvable."); return true; }
                    showBalance(player, target);
                } else {
                    showBalance(player, player);
                }
            }
            case "pay", "payer" -> handlePay(player, args);
            case "top" -> handleTop(player);
            case "give", "donner" -> handleGive(player, args);
            case "set", "fixer" -> handleSet(player, args);
            case "take", "retirer" -> handleTake(player, args);
            default -> sendHelp(player);
        }
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
        MessageUtil.sendRaw(viewer, "  §7Balance: §e" + MessageUtil.formatNumber(balance) + " §6coins");
        MessageUtil.sendRaw(viewer, "  §7Niveau: §e" + grade.getLevel());
        MessageUtil.sendRaw(viewer, "  §7XP: §e" + grade.getXp() + " / " + grade.getXpForNextLevel());
        MessageUtil.sendSeparator(viewer);
    }

    private void handlePay(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(player, "Usage: /money pay <joueur> <montant>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Joueur introuvable."); return; }
        if (target.equals(player)) { MessageUtil.sendError(player, "Vous ne pouvez pas vous payer vous-même."); return; }

        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) throw new NumberFormatException();
            if (!plugin.getEconomyManager().transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
                MessageUtil.sendError(player, "Fonds insuffisants. Balance: §e" + MessageUtil.formatNumber(plugin.getEconomyManager().getBalance(player.getUniqueId())) + " coins");
                return;
            }
            MessageUtil.sendSuccess(player, "§e" + MessageUtil.formatNumber(amount) + " coins §aenvoyés à §f" + target.getName() + "§a.");
            MessageUtil.send(target, "§a§f" + player.getName() + " §avous a envoyé §e" + MessageUtil.formatNumber(amount) + " coins§a.");
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Montant invalide.");
        }
    }

    private void handleTop(Player player) {
        List<PlayerAccount> top = plugin.getEconomyManager().getTopAccounts(10);
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Top Richesse");
        for (int i = 0; i < top.size(); i++) {
            PlayerAccount account = top.get(i);
            String name = Bukkit.getOfflinePlayer(account.getPlayerId()).getName();
            if (name == null) name = account.getPlayerId().toString().substring(0, 8);
            MessageUtil.sendRaw(player, "  §7" + (i + 1) + ". §f" + name + " §7— §e" + MessageUtil.formatNumber(account.getBalance()) + " coins");
        }
        MessageUtil.sendSeparator(player);
    }

    private void handleGive(Player player, String[] args) {
        if (!player.hasPermission("nations.admin")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        if (args.length < 3) {
            MessageUtil.sendError(player, "Usage: /money give <joueur> <montant>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Joueur introuvable."); return; }
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) throw new NumberFormatException();
            plugin.getEconomyManager().deposit(target.getUniqueId(), amount);
            MessageUtil.sendSuccess(player, "§e" + MessageUtil.formatNumber(amount) + " coins §adonnés à §f" + target.getName() + "§a.");
            MessageUtil.send(target, "§aVous avez reçu §e" + MessageUtil.formatNumber(amount) + " coins §ade l'administration.");
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Montant invalide.");
        }
    }

    private void handleSet(Player player, String[] args) {
        if (!player.hasPermission("nations.admin")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        if (args.length < 3) {
            MessageUtil.sendError(player, "Usage: /money set <joueur> <montant>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Joueur introuvable."); return; }
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount < 0) throw new NumberFormatException();
            plugin.getEconomyManager().setBalance(target.getUniqueId(), amount);
            MessageUtil.sendSuccess(player, "Balance de §f" + target.getName() + " §afixée à §e" + MessageUtil.formatNumber(amount) + " coins§a.");
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Montant invalide.");
        }
    }

    private void handleTake(Player player, String[] args) {
        if (!player.hasPermission("nations.admin")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        if (args.length < 3) {
            MessageUtil.sendError(player, "Usage: /money take <joueur> <montant>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Joueur introuvable."); return; }
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) throw new NumberFormatException();
            plugin.getEconomyManager().withdraw(target.getUniqueId(), amount);
            MessageUtil.sendSuccess(player, "§e" + MessageUtil.formatNumber(amount) + " coins §aretirés de §f" + target.getName() + "§a.");
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Montant invalide.");
        }
    }

    private void sendHelp(Player player) {
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Économie - Aide");
        MessageUtil.sendRaw(player, "  §e/money §7— Voir votre balance");
        MessageUtil.sendRaw(player, "  §e/money bal [joueur] §7— Balance d'un joueur");
        MessageUtil.sendRaw(player, "  §e/money pay <joueur> <montant> §7— Payer un joueur");
        MessageUtil.sendRaw(player, "  §e/money top §7— Top 10 richesse");
        if (player.hasPermission("nations.admin")) {
            MessageUtil.sendRaw(player, "  §e/money give <joueur> <montant> §7— Donner des coins (admin)");
            MessageUtil.sendRaw(player, "  §e/money set <joueur> <montant> §7— Fixer la balance (admin)");
            MessageUtil.sendRaw(player, "  §e/money take <joueur> <montant> §7— Retirer des coins (admin)");
        }
        MessageUtil.sendSeparator(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("balance", "pay", "top"));
            if (sender.hasPermission("nations.admin")) subs.addAll(Arrays.asList("give", "set", "take"));
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "balance", "pay", "give", "set", "take" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("pay") || args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            completions.addAll(Arrays.asList("100", "500", "1000", "5000", "10000"));
        }
        return completions;
    }
}
