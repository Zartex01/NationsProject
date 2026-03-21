package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.nation.Coalition;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.util.MessageUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class CoalitionCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public CoalitionCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "creer" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "leave", "quitter" -> handleLeave(player);
            case "info" -> handleInfo(player, args);
            case "list", "liste" -> handleList(player);
            case "disband", "dissoudre" -> handleDisband(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /coalition create <nom>");
            return;
        }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        if (!nation.isLeader(player.getUniqueId())) { MessageUtil.sendError(player, "Seul le chef de nation peut créer une coalition."); return; }
        if (nation.getCoalitionId() != null) { MessageUtil.sendError(player, "Votre nation appartient déjà à une coalition."); return; }

        String coalName = args[1];
        if (plugin.getNationManager().getCoalitionByName(coalName) != null) {
            MessageUtil.sendError(player, "Une coalition avec ce nom existe déjà.");
            return;
        }
        plugin.getNationManager().createCoalition(nation, coalName);
        MessageUtil.sendSuccess(player, "Coalition §6" + coalName + " §acréée!");
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /coalition invite <nation>");
            return;
        }
        Nation myNation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (myNation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        if (myNation.getCoalitionId() == null) { MessageUtil.sendError(player, "Votre nation n'appartient à aucune coalition."); return; }

        Coalition coalition = plugin.getNationManager().getCoalition(myNation.getCoalitionId());
        if (coalition == null || !coalition.getLeaderNationId().equals(myNation.getId())) {
            MessageUtil.sendError(player, "Seule la nation leader peut inviter.");
            return;
        }

        Nation target = plugin.getNationManager().getNationByName(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
        if (target.getCoalitionId() != null) { MessageUtil.sendError(player, "Cette nation appartient déjà à une coalition."); return; }

        plugin.getNationManager().addNationToCoalition(coalition.getId(), target);
        MessageUtil.sendSuccess(player, "§6" + target.getName() + " §aa rejoint la coalition §e" + coalition.getName() + "§a!");

        Player leaderOnline = plugin.getServer().getPlayer(target.getLeaderId());
        if (leaderOnline != null) {
            MessageUtil.send(leaderOnline, "§aVotre nation a été ajoutée à la coalition §6" + coalition.getName() + "§a.");
        }
    }

    private void handleLeave(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        if (nation.getCoalitionId() == null) { MessageUtil.sendError(player, "Votre nation n'appartient à aucune coalition."); return; }

        Coalition coalition = plugin.getNationManager().getCoalition(nation.getCoalitionId());
        String coalName = coalition != null ? coalition.getName() : "?";
        plugin.getNationManager().removeNationFromCoalition(nation);
        MessageUtil.sendSuccess(player, "Votre nation a quitté la coalition §6" + coalName + "§a.");
    }

    private void handleInfo(Player player, String[] args) {
        Coalition coalition;
        if (args.length >= 2) {
            coalition = plugin.getNationManager().getCoalitionByName(args[1]);
        } else {
            Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
            coalition = nation != null && nation.getCoalitionId() != null
                ? plugin.getNationManager().getCoalition(nation.getCoalitionId())
                : null;
        }
        if (coalition == null) { MessageUtil.sendError(player, "Coalition introuvable."); return; }

        Nation leader = plugin.getNationManager().getNationById(coalition.getLeaderNationId());
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, coalition.getName());
        MessageUtil.sendRaw(player, "  §7Nation leader: §6" + (leader != null ? leader.getName() : "?"));
        MessageUtil.sendRaw(player, "  §7Nations: §f" + coalition.getNationCount());
        for (UUID nId : coalition.getMemberNations()) {
            Nation n = plugin.getNationManager().getNationById(nId);
            if (n != null) MessageUtil.sendRaw(player, "    §7- §6" + n.getName());
        }
        MessageUtil.sendSeparator(player);
    }

    private void handleList(Player player) {
        Collection<Coalition> coalitions = plugin.getNationManager().getAllCoalitions();
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Coalitions");
        if (coalitions.isEmpty()) {
            MessageUtil.sendRaw(player, "  §7Aucune coalition.");
        } else {
            for (Coalition c : coalitions) {
                MessageUtil.sendRaw(player, "  §6" + c.getName() + " §7— §f" + c.getNationCount() + " nations");
            }
        }
        MessageUtil.sendSeparator(player);
    }

    private void handleDisband(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        if (nation.getCoalitionId() == null) { MessageUtil.sendError(player, "Vous n'appartenez à aucune coalition."); return; }

        Coalition coalition = plugin.getNationManager().getCoalition(nation.getCoalitionId());
        if (coalition == null || !coalition.getLeaderNationId().equals(nation.getId())) {
            MessageUtil.sendError(player, "Seule la nation leader peut dissoudre la coalition.");
            return;
        }

        String coalName = coalition.getName();
        for (UUID nId : new HashSet<>(coalition.getMemberNations())) {
            Nation member = plugin.getNationManager().getNationById(nId);
            if (member != null) plugin.getNationManager().removeNationFromCoalition(member);
        }
        MessageUtil.sendSuccess(player, "Coalition §6" + coalName + " §adissoute.");
    }

    private void sendHelp(Player player) {
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Coalitions - Aide");
        MessageUtil.sendRaw(player, "  §e/coalition create <nom> §7— Créer une coalition");
        MessageUtil.sendRaw(player, "  §e/coalition invite <nation> §7— Inviter une nation");
        MessageUtil.sendRaw(player, "  §e/coalition leave §7— Quitter la coalition");
        MessageUtil.sendRaw(player, "  §e/coalition info [nom] §7— Infos sur la coalition");
        MessageUtil.sendRaw(player, "  §e/coalition list §7— Liste des coalitions");
        MessageUtil.sendRaw(player, "  §e/coalition disband §7— Dissoudre la coalition");
        MessageUtil.sendSeparator(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("create", "invite", "leave", "info", "list", "disband")) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("invite")) {
                for (Nation n : plugin.getNationManager().getAllNations()) {
                    if (n.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(n.getName());
                }
            } else if (args[0].equalsIgnoreCase("info")) {
                for (Coalition c : plugin.getNationManager().getAllCoalitions()) {
                    if (c.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(c.getName());
                }
            }
        }
        return completions;
    }
}
