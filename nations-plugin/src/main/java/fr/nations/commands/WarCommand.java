package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.gui.StaffWarsGui;
import fr.nations.gui.WarsGui;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.util.MessageUtil;
import fr.nations.war.*;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class WarCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM HH:mm");

    public WarCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                new WarsGui(plugin, player).open();
            } else {
                handlePendingConsole(sender);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "declare", "declarer" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Joueurs uniquement."); return true; }
                handleDeclare(p, args);
            }
            case "info" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Joueurs uniquement."); return true; }
                handleInfo(p);
            }
            case "list", "liste" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Joueurs uniquement."); return true; }
                new WarsGui(plugin, p).open();
            }
            case "pending", "attente" -> {
                handlePending(sender);
            }
            case "active", "actives" -> {
                handleActive(sender);
            }
            case "validate", "valider" -> handleValidate(sender, args);
            case "reject", "rejeter" -> handleReject(sender, args);
            case "staff" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Joueurs uniquement."); return true; }
                handleStaffGui(p);
            }
            case "surrender", "reddition" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Joueurs uniquement."); return true; }
                handleSurrender(p, args);
            }
            default -> {
                if (sender instanceof Player p) sendHelp(p);
                else sender.sendMessage("Usage: /war <pending|active|validate|reject>");
            }
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

        if (warType.isAssault()) {
            player.sendMessage("§4§l⚠ GUERRE D'ASSAUT ⚠");
            player.sendMessage("§cCette guerre est SANS LIMITE DE TEMPS.");
            player.sendMessage("§cElle ne se termine que par la reddition ou la dissolution de la nation ennemie.");
            player.sendMessage("§cLa validation staff sera très stricte.");
            player.sendMessage("§cCoût: §e" + MessageUtil.formatNumber(warType.getCost()) + " coins");
        }

        String reason = args.length >= 4
            ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
            : "Aucune raison fournie";

        WarManager.WarDeclarationResult result = plugin.getWarManager().declareWar(myNation, targetNation, warType, reason);
        switch (result) {
            case PENDING_VALIDATION -> {
                MessageUtil.sendSuccess(player, "Déclaration de guerre envoyée au staff pour validation!");
                if (warType.isAssault()) {
                    player.sendMessage("§4Cette guerre d'assaut nécessite une validation staff manuelle.");
                }
                notifyStaff("§c" + myNation.getName() + " §7a déclaré une guerre §e"
                    + warType.getColoredName() + " §7contre §c" + targetNation.getName()
                    + (warType.isAssault() ? " §4§l[ASSAUT - VALIDATION STRICTE]" : ""));
            }
            case SAME_NATION -> MessageUtil.sendError(player, "Vous ne pouvez pas vous déclarer la guerre.");
            case IS_ALLY -> MessageUtil.sendError(player, "Vous ne pouvez pas déclarer la guerre à un allié.");
            case ON_COOLDOWN -> MessageUtil.sendError(player, "Cooldown actif avant de déclarer une nouvelle guerre.");
            case MAX_WARS_REACHED -> MessageUtil.sendError(player, "Nombre maximum de guerres actives atteint.");
            case ALREADY_AT_WAR -> MessageUtil.sendError(player, "Déjà en guerre avec cette nation.");
            case INSUFFICIENT_FUNDS -> MessageUtil.sendError(player, "Fonds insuffisants. Coût: §e" + MessageUtil.formatNumber(warType.getCost()) + " coins");
            case NOT_ENOUGH_ONLINE_ATTACKER -> {
                int min = plugin.getConfig().getInt("wars.min-online-to-declare", 2);
                MessageUtil.sendError(player, "Votre nation doit avoir au moins §e" + min
                    + " joueurs connectés §cpour déclarer une guerre.");
            }
            case NOT_ENOUGH_ONLINE_DEFENDER -> {
                int min = plugin.getConfig().getInt("wars.min-online-to-declare", 2);
                MessageUtil.sendError(player, "La nation adverse doit avoir au moins §e" + min
                    + " joueurs connectés §cpour être déclarée en guerre.");
                player.sendMessage("§7La guerre reste suspendue jusqu'à ce qu'ils soient assez connectés.");
            }
        }
    }

    private void handleInfo(Player player) {
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
            String aName = getNationName(war.getAttackerNationId());
            String dName = getNationName(war.getDefenderNationId());
            player.sendMessage("  §c" + aName + " §7vs §9" + dName);
            player.sendMessage("  §7Type: " + war.getType().getColoredName() + " §7| Durée: §f" + war.getFormattedTimeRemaining());
            player.sendMessage("  §7Kills: §c" + war.getAttackerKills() + " §7vs §9" + war.getDefenderKills());
            if (war.getType().isAssault()) {
                player.sendMessage("  §4§l⚠ ASSAUT §4- Se termine par reddition ou dissolution");
                if (war.hasSurrenderRequest()) {
                    player.sendMessage("  §c✦ Reddition demandée! Confirmez: §e/war surrender confirm <id>");
                }
            }
        }
        MessageUtil.sendSeparator(player);
    }

    /**
     * /war pending — Liste les guerres en attente de validation (staff et console)
     */
    private void handlePending(CommandSender sender) {
        if (sender instanceof Player player && !player.hasPermission("nations.staff")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }

        List<War> pending = plugin.getWarManager().getPendingWars();

        if (pending.isEmpty()) {
            sender.sendMessage("§a✔ Aucune guerre en attente de validation.");
            return;
        }

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§l  ⏳ Guerres en attente de validation §8(" + pending.size() + ")");
        sender.sendMessage("§8§m                                        ");

        for (int i = 0; i < pending.size(); i++) {
            War war = pending.get(i);
            String attName = getNationName(war.getAttackerNationId());
            String defName = getNationName(war.getDefenderNationId());
            String date = SDF.format(new Date(war.getDeclaredAt()));

            sender.sendMessage("§7" + (i + 1) + ". " + war.getType().getColoredName()
                + " §c" + attName + " §7→ §9" + defName);
            sender.sendMessage("   §7Raison: §f" + (war.getReason() != null ? war.getReason() : "Aucune"));
            sender.sendMessage("   §7Date: §e" + date);
            if (war.getType().isAssault()) {
                sender.sendMessage("   §4§l⚠ GUERRE D'ASSAUT — Validation très stricte requise!");
            }
            sender.sendMessage("   §7ID: §8" + war.getId());
            sender.sendMessage("   §7Commandes: §a/war validate §f" + war.getId()
                + " §7| §c/war reject §f" + war.getId() + " <raison>");
            sender.sendMessage("§8 ─────────────────────────────────────");
        }
        sender.sendMessage("§8§m                                        ");
    }

    private void handlePendingConsole(CommandSender sender) {
        handlePending(sender);
    }

    /**
     * /war active — Liste les guerres en cours
     */
    private void handleActive(CommandSender sender) {
        if (sender instanceof Player player && !player.hasPermission("nations.staff")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        List<War> active = plugin.getWarManager().getAllActiveWars();
        if (active.isEmpty()) {
            sender.sendMessage("§7Aucune guerre active en ce moment.");
            return;
        }
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§c§l  ⚔ Guerres actives §8(" + active.size() + ")");
        sender.sendMessage("§8§m                                        ");
        for (War war : active) {
            String attName = getNationName(war.getAttackerNationId());
            String defName = getNationName(war.getDefenderNationId());
            sender.sendMessage("§7• " + war.getType().getColoredName()
                + " §c" + attName + " §7(§c" + war.getAttackerKills() + "§7) "
                + "§7vs §9" + defName + " §7(§9" + war.getDefenderKills() + "§7)"
                + " — §f" + war.getFormattedTimeRemaining());
        }
        sender.sendMessage("§8§m                                        ");
    }

    private void handleValidate(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !player.hasPermission("nations.staff")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /war validate <war-id>");
            return;
        }
        try {
            UUID warId = UUID.fromString(args[1]);
            UUID staffId = sender instanceof Player p ? p.getUniqueId() : UUID.randomUUID();
            if (plugin.getWarManager().validateWar(warId, staffId)) {
                War war = plugin.getWarManager().getWar(warId);
                sender.sendMessage("§a✔ Guerre validée et démarrée!");
                if (war != null) {
                    String attName = getNationName(war.getAttackerNationId());
                    String defName = getNationName(war.getDefenderNationId());
                    Bukkit.broadcastMessage("§8[§4Guerre§8] §c⚔ " + war.getType().getColoredName()
                        + " §7déclarée! §c" + attName + " §7vs §9" + defName);
                    if (war.getType().isAssault()) {
                        Bukkit.broadcastMessage("§4§l⚠ ASSAUT EN COURS ⚠ §cSans limite de temps!");
                    }
                }
            } else {
                sender.sendMessage("§cGuerre introuvable ou déjà traitée.");
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cID de guerre invalide.");
        }
    }

    private void handleReject(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !player.hasPermission("nations.staff")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /war reject <war-id> [raison...]");
            return;
        }
        try {
            UUID warId = UUID.fromString(args[1]);
            String note = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Rejetée par le staff";
            UUID staffId = sender instanceof Player p ? p.getUniqueId() : UUID.randomUUID();
            if (plugin.getWarManager().rejectWar(warId, staffId, note)) {
                sender.sendMessage("§a✔ Guerre rejetée. 50% du coût remboursé à l'attaquant.");
            } else {
                sender.sendMessage("§cGuerre introuvable ou déjà traitée.");
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cID invalide.");
        }
    }

    private void handleStaffGui(Player player) {
        if (!player.hasPermission("nations.staff")) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        new StaffWarsGui(plugin, player).open();
    }

    /**
     * /war surrender [confirm] <war-id>
     * Demande ou confirme la reddition pour une guerre d'assaut.
     */
    private void handleSurrender(Player player, String[] args) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }

        NationMember member = nation.getMember(player.getUniqueId());
        if (member == null || !member.getRole().name().equals("LEADER")) {
            MessageUtil.sendError(player, "Seul le leader peut demander/confirmer la reddition.");
            return;
        }

        if (args.length < 2) {
            List<War> assaults = plugin.getWarManager().getActiveWarsForNation(nation.getId()).stream()
                .filter(w -> w.getType().isAssault() && w.getDefenderNationId().equals(nation.getId()))
                .toList();
            if (assaults.isEmpty()) {
                MessageUtil.sendError(player, "Aucune guerre d'assaut en cours contre votre nation.");
                return;
            }
            player.sendMessage("§4§l⚠ Guerres d'assaut actives contre votre nation:");
            for (War w : assaults) {
                player.sendMessage("§c• " + getNationName(w.getAttackerNationId()) + " §7- ID: §8" + w.getId());
                player.sendMessage("  §7/war surrender <war-id> §7— Demander la reddition");
                player.sendMessage("  §7/war surrender confirm <war-id> §7— Confirmer");
            }
            return;
        }

        boolean confirm = args[1].equalsIgnoreCase("confirm");
        int warArgIdx = confirm ? 2 : 1;

        if (args.length <= warArgIdx) {
            MessageUtil.sendError(player, "Fournissez l'ID de la guerre.");
            return;
        }

        UUID warId;
        try { warId = UUID.fromString(args[warArgIdx]); }
        catch (IllegalArgumentException e) { MessageUtil.sendError(player, "ID invalide."); return; }

        if (confirm) {
            if (plugin.getWarManager().confirmSurrender(nation.getId(), warId)) {
                War war = plugin.getWarManager().getWar(warId);
                String attName = war != null ? getNationName(war.getAttackerNationId()) : "?";
                Bukkit.broadcastMessage("§4§l[Assaut] §c" + nation.getName()
                    + " §7s'est rendue à §c" + attName + " §7!");
            } else {
                MessageUtil.sendError(player, "Impossible de confirmer la reddition.");
            }
        } else {
            WarManager.SurrenderResult result = plugin.getWarManager().requestSurrender(nation.getId(), warId);
            switch (result) {
                case REQUESTED -> {
                    player.sendMessage("§6Demande de reddition envoyée.");
                    player.sendMessage("§7Confirmez avec: §c/war surrender confirm " + warId);
                    player.sendMessage("§cAttenion: cette action est irréversible!");
                }
                case WAR_NOT_FOUND -> MessageUtil.sendError(player, "Guerre introuvable.");
                case NOT_DEFENDER -> MessageUtil.sendError(player, "Votre nation n'est pas le défenseur.");
                case NOT_ASSAULT -> MessageUtil.sendError(player, "La reddition n'est disponible que pour les guerres d'assaut.");
                case ALREADY_REQUESTED -> {
                    player.sendMessage("§6Reddition déjà demandée. Confirmez: §c/war surrender confirm " + warId);
                }
            }
        }
    }

    private String getNationName(UUID id) {
        Nation n = plugin.getNationManager().getNationById(id);
        return n != null ? n.getName() : "Inconnue";
    }

    private String getWarTypeList() {
        StringBuilder sb = new StringBuilder();
        for (WarType type : WarType.values()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(type.name().toLowerCase());
        }
        return sb.toString();
    }

    private void notifyStaff(String message) {
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.hasPermission("nations.staff"))
            .forEach(p -> p.sendMessage("§8[Staff] " + message));
    }

    private void sendHelp(Player player) {
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Guerres — Aide");
        player.sendMessage("  §e/war §7— Interface des guerres");
        player.sendMessage("  §e/war declare <nation> <type> [raison] §7— Déclarer une guerre");
        player.sendMessage("  §e/war info §7— Vos guerres actives");
        player.sendMessage("  §e/war list §7— Liste toutes les guerres");
        player.sendMessage("  §e/war surrender [confirm] <war-id> §7— Reddition (guerres d'assaut)");
        if (player.hasPermission("nations.staff")) {
            player.sendMessage("  §e/war pending §7— §6[STAFF] Guerres en attente de validation");
            player.sendMessage("  §e/war active §7— §6[STAFF] Guerres actives");
            player.sendMessage("  §e/war staff §7— §6[STAFF] Interface GUI de validation");
            player.sendMessage("  §e/war validate <id> §7— Valider une guerre");
            player.sendMessage("  §e/war reject <id> [raison] §7— Rejeter une guerre");
        }
        player.sendMessage("  §7Types: §f" + getWarTypeList());
        MessageUtil.sendSeparator(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("declare", "info", "list", "surrender"));
            if (sender.hasPermission("nations.staff")) {
                subs.addAll(Arrays.asList("pending", "active", "staff", "validate", "reject"));
            }
            subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).forEach(completions::add);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("declare")) {
            plugin.getNationManager().getAllNations().stream()
                .filter(n -> n.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                .forEach(n -> completions.add(n.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("declare")) {
            Arrays.stream(WarType.values())
                .filter(t -> t.name().toLowerCase().startsWith(args[2].toLowerCase()))
                .forEach(t -> completions.add(t.name().toLowerCase()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("surrender")) {
            completions.add("confirm");
        }
        return completions;
    }
}
