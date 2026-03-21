package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.gui.StaffWarsGui;
import fr.nations.gui.WarsGui;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.util.MessageUtil;
import fr.nations.war.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class WarCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public WarCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            new WarsGui(plugin, player).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "declare", "declarer" -> handleDeclare(player, args);
            case "info" -> handleInfo(player, args);
            case "list", "liste" -> handleList(player);
            case "validate", "valider" -> handleValidate(player, args);
            case "reject", "rejeter" -> handleReject(player, args);
            case "staff" -> handleStaffGui(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleDeclare(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(player, "Usage: /war declare <nation> <type> [raison...]");
            return;
        }
        Nation myNation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (myNation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }

        NationMember member = myNation.getMember(player.getUniqueId());
        if (member == null || !member.canManageWar()) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission de déclarer une guerre.");
            return;
        }

        Nation targetNation = plugin.getNationManager().getNationByName(args[1]);
        if (targetNation == null) { MessageUtil.sendError(player, "Nation cible introuvable."); return; }

        WarType warType;
        try {
            warType = WarType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtil.sendError(player, "Type de guerre invalide. Types: " + getWarTypeList());
            return;
        }

        String reason = args.length >= 4
            ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
            : "";

        WarManager.WarDeclarationResult result = plugin.getWarManager().declareWar(myNation, targetNation, warType, reason);
        switch (result) {
            case PENDING_VALIDATION ->
                MessageUtil.sendSuccess(player, "Déclaration de guerre envoyée au staff pour validation!");
            case SAME_NATION -> MessageUtil.sendError(player, "Vous ne pouvez pas déclarer la guerre à votre propre nation.");
            case IS_ALLY -> MessageUtil.sendError(player, "Vous ne pouvez pas déclarer la guerre à un allié.");
            case ON_COOLDOWN -> MessageUtil.sendError(player, "Vous êtes en période de cooldown avant de déclarer une nouvelle guerre.");
            case MAX_WARS_REACHED -> MessageUtil.sendError(player, "Vous avez atteint le nombre maximum de guerres actives.");
            case ALREADY_AT_WAR -> MessageUtil.sendError(player, "Vous êtes déjà en guerre avec cette nation.");
            case INSUFFICIENT_FUNDS -> MessageUtil.sendError(player, "Fonds insuffisants. Coût: §e" + MessageUtil.formatNumber(warType.getCost()) + " coins");
        }
    }

    private void handleInfo(Player player, String[] args) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }

        List<War> wars = plugin.getWarManager().getActiveWarsForNation(nation.getId());
        if (wars.isEmpty()) {
            MessageUtil.sendInfo(player, "Votre nation n'est pas en guerre actuellement.");
            return;
        }

        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Guerres actives");
        for (War war : wars) {
            Nation attacker = plugin.getNationManager().getNationById(war.getAttackerNationId());
            Nation defender = plugin.getNationManager().getNationById(war.getDefenderNationId());
            String attackerName = attacker != null ? attacker.getName() : "?";
            String defenderName = defender != null ? defender.getName() : "?";
            MessageUtil.sendRaw(player, "  §c" + attackerName + " §7vs §c" + defenderName);
            MessageUtil.sendRaw(player, "  §7Type: §e" + war.getType().getDisplayName() + " §7| Temps: §f" + war.getFormattedTimeRemaining());
            MessageUtil.sendRaw(player, "  §7Kills: §c" + war.getAttackerKills() + " §7vs §9" + war.getDefenderKills());
        }
        MessageUtil.sendSeparator(player);
    }

    private void handleList(Player player) {
        new WarsGui(plugin, player).open();
    }

    private void handleValidate(Player player, String[] args) {
        if (!player.hasPermission("nations.staff")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /war validate <war-id>");
            return;
        }
        try {
            UUID warId = UUID.fromString(args[1]);
            if (plugin.getWarManager().validateWar(warId, player.getUniqueId())) {
                War war = plugin.getWarManager().getWar(warId);
                Nation attacker = plugin.getNationManager().getNationById(war.getAttackerNationId());
                Nation defender = plugin.getNationManager().getNationById(war.getDefenderNationId());
                MessageUtil.sendSuccess(player, "Guerre validée!");
                plugin.getServer().broadcastMessage(MessageUtil.colorize(
                    plugin.getConfigManager().getPrefix()
                    + "§c⚔ Guerre déclarée! §6" + (attacker != null ? attacker.getName() : "?")
                    + " §7contre §c" + (defender != null ? defender.getName() : "?")
                    + " §7— §e" + war.getType().getDisplayName()
                ));
            } else {
                MessageUtil.sendError(player, "Guerre introuvable ou déjà traitée.");
            }
        } catch (IllegalArgumentException e) {
            MessageUtil.sendError(player, "ID de guerre invalide.");
        }
    }

    private void handleReject(Player player, String[] args) {
        if (!player.hasPermission("nations.staff")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /war reject <war-id> [raison...]");
            return;
        }
        try {
            UUID warId = UUID.fromString(args[1]);
            String note = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Aucune raison";
            if (plugin.getWarManager().rejectWar(warId, player.getUniqueId(), note)) {
                MessageUtil.sendSuccess(player, "Guerre rejetée.");
            } else {
                MessageUtil.sendError(player, "Guerre introuvable ou déjà traitée.");
            }
        } catch (IllegalArgumentException e) {
            MessageUtil.sendError(player, "ID invalide.");
        }
    }

    private void handleStaffGui(Player player) {
        if (!player.hasPermission("nations.staff")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        new StaffWarsGui(plugin, player).open();
    }

    private String getWarTypeList() {
        StringBuilder sb = new StringBuilder();
        for (WarType type : WarType.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(type.name());
        }
        return sb.toString();
    }

    private void sendHelp(Player player) {
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Guerres - Aide");
        MessageUtil.sendRaw(player, "  §e/war §7— Interface des guerres");
        MessageUtil.sendRaw(player, "  §e/war declare <nation> <type> [raison] §7— Déclarer une guerre");
        MessageUtil.sendRaw(player, "  §e/war info §7— Voir vos guerres actives");
        MessageUtil.sendRaw(player, "  §e/war list §7— Liste toutes les guerres");
        if (player.hasPermission("nations.staff")) {
            MessageUtil.sendRaw(player, "  §e/war staff §7— Interface staff de validation");
            MessageUtil.sendRaw(player, "  §e/war validate <id> §7— Valider une guerre");
            MessageUtil.sendRaw(player, "  §e/war reject <id> [raison] §7— Rejeter une guerre");
        }
        MessageUtil.sendRaw(player, "  §7Types: " + getWarTypeList());
        MessageUtil.sendSeparator(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("declare", "info", "list", "staff", "validate", "reject");
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("declare")) {
            for (Nation n : plugin.getNationManager().getAllNations()) {
                if (n.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(n.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("declare")) {
            for (WarType type : WarType.values()) {
                if (type.name().toLowerCase().startsWith(args[2].toLowerCase())) completions.add(type.name());
            }
        }
        return completions;
    }
}
