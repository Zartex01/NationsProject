package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.gui.*;
import fr.nations.nation.*;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class NationCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public NationCommand(NationsPlugin plugin) {
        this.plugin = plugin;
        MessageUtil.init(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length == 0) {
            handleGui(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "creer" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "join", "rejoindre" -> handleJoin(player, args);
            case "leave", "quitter" -> handleLeave(player);
            case "kick", "expulser" -> handleKick(player, args);
            case "promote", "promouvoir" -> handlePromote(player, args);
            case "demote", "retrograder" -> handleDemote(player, args);
            case "ally", "allie" -> handleAlly(player, args);
            case "unally", "desallie" -> handleUnally(player, args);
            case "info" -> handleInfo(player, args);
            case "list", "liste" -> handleList(player);
            case "disband", "dissoudre" -> handleDisband(player);
            case "open", "ouvrir" -> handleOpen(player);
            case "role" -> handleRoleGui(player, args);
            case "description", "desc" -> handleDescription(player, args);
            case "rename", "renommer" -> handleRename(player, args);
            case "deposit", "deposer" -> handleBankDeposit(player, args);
            case "withdraw", "retirer" -> handleBankWithdraw(player, args);
            case "accept", "accepter" -> handleAcceptInvite(player, args);
            case "gui" -> handleGui(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleGui(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) {
            MessageUtil.sendError(player, "Vous n'avez pas de nation. Créez-en une avec /nation create <nom>");
            return;
        }
        new NationMainGui(plugin, player, nation).open();
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation create <nom>");
            return;
        }
        if (plugin.getNationManager().hasNation(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous appartenez déjà à une nation.");
            return;
        }
        String name = args[1];
        int minLen = plugin.getConfigManager().getMinNationNameLength();
        int maxLen = plugin.getConfigManager().getMaxNationNameLength();
        if (name.length() < minLen || name.length() > maxLen) {
            MessageUtil.sendError(player, "Le nom doit faire entre " + minLen + " et " + maxLen + " caractères.");
            return;
        }
        if (!name.matches("[a-zA-Z0-9_]+")) {
            MessageUtil.sendError(player, "Le nom ne peut contenir que des lettres, chiffres et underscores.");
            return;
        }
        if (plugin.getNationManager().getNationByName(name) != null) {
            MessageUtil.sendError(player, "Une nation avec ce nom existe déjà.");
            return;
        }
        double cost = plugin.getConfigManager().getNationCreationCost();
        if (!plugin.getEconomyManager().has(player.getUniqueId(), cost)) {
            MessageUtil.sendError(player, "Vous n'avez pas assez d'argent. Coût: §e" + MessageUtil.formatNumber(cost) + " coins");
            return;
        }
        plugin.getEconomyManager().withdraw(player.getUniqueId(), cost);
        Nation nation = plugin.getNationManager().createNation(player, name);
        MessageUtil.sendSuccess(player, "La nation §6" + nation.getName() + " §aa été créée!");
        Bukkit.broadcastMessage(MessageUtil.colorize(plugin.getConfigManager().getPrefix()
            + "§6" + player.getName() + " §7a fondé la nation §e" + nation.getName() + "§7!"));
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation invite <joueur>");
            return;
        }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        NationMember member = nation.getMember(player.getUniqueId());
        if (member == null || !member.canInvite()) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission d'inviter des joueurs.");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Joueur introuvable ou hors ligne."); return; }
        if (plugin.getNationManager().hasNation(target.getUniqueId())) {
            MessageUtil.sendError(player, "Ce joueur appartient déjà à une nation.");
            return;
        }
        if (nation.hasPendingInvite(target.getUniqueId())) {
            MessageUtil.sendError(player, "Ce joueur a déjà une invitation en attente.");
            return;
        }
        nation.addPendingInvite(target.getUniqueId());
        MessageUtil.sendSuccess(player, "Invitation envoyée à §f" + target.getName() + "§a.");
        MessageUtil.send(target, "§6" + player.getName() + " §7vous invite à rejoindre la nation §6" + nation.getName() + "§7.");
        MessageUtil.send(target, "§7Tapez §e/nation accept " + nation.getName() + " §7pour accepter.");
        plugin.getDataManager().saveNations();
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation join <nom>");
            return;
        }
        if (plugin.getNationManager().hasNation(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous appartenez déjà à une nation.");
            return;
        }
        Nation nation = plugin.getNationManager().getNationByName(args[1]);
        if (nation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
        if (!nation.isOpen() && !nation.hasPendingInvite(player.getUniqueId())) {
            MessageUtil.sendError(player, "Cette nation est fermée. Vous avez besoin d'une invitation.");
            return;
        }
        nation.removePendingInvite(player.getUniqueId());
        plugin.getNationManager().addPlayerToNation(nation.getId(), player.getUniqueId(), player.getName());
        MessageUtil.sendSuccess(player, "Vous avez rejoint la nation §6" + nation.getName() + "§a!");
        notifyNationMembers(nation, player.getUniqueId(), "§7→ §a" + player.getName() + " §7a rejoint la nation!");
    }

    private void handleAcceptInvite(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation accept <nom>");
            return;
        }
        if (plugin.getNationManager().hasNation(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous appartenez déjà à une nation.");
            return;
        }
        Nation nation = plugin.getNationManager().getNationByName(args[1]);
        if (nation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
        if (!nation.hasPendingInvite(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous n'avez pas d'invitation de cette nation.");
            return;
        }
        nation.removePendingInvite(player.getUniqueId());
        plugin.getNationManager().addPlayerToNation(nation.getId(), player.getUniqueId(), player.getName());
        MessageUtil.sendSuccess(player, "Vous avez rejoint la nation §6" + nation.getName() + "§a!");
        notifyNationMembers(nation, player.getUniqueId(), "§7→ §a" + player.getName() + " §7a rejoint la nation!");
    }

    private void handleLeave(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        if (nation.isLeader(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous êtes le chef de la nation. Transférez le leadership ou dissolvez-la.");
            return;
        }
        String nationName = nation.getName();
        plugin.getNationManager().removePlayerFromNation(player.getUniqueId());
        MessageUtil.sendSuccess(player, "Vous avez quitté la nation §6" + nationName + "§a.");
        notifyNationMembers(nation, player.getUniqueId(), "§7← §c" + player.getName() + " §7a quitté la nation.");
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation kick <joueur>");
            return;
        }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        NationMember kicker = nation.getMember(player.getUniqueId());
        if (kicker == null || !kicker.canKick()) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission d'expulser.");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        UUID targetId = target != null ? target.getUniqueId()
            : Bukkit.getOfflinePlayer(args[1]).getUniqueId();

        NationMember targetMember = nation.getMember(targetId);
        if (targetMember == null) { MessageUtil.sendError(player, "Ce joueur n'est pas dans votre nation."); return; }
        if (nation.isLeader(targetId)) { MessageUtil.sendError(player, "Impossible d'expulser le chef."); return; }
        if (kicker.getRole().getRank() <= targetMember.getRole().getRank()) {
            MessageUtil.sendError(player, "Vous ne pouvez pas expulser un joueur de rang supérieur ou égal.");
            return;
        }
        plugin.getNationManager().removePlayerFromNation(targetId);
        MessageUtil.sendSuccess(player, "§f" + targetMember.getPlayerName() + " §aa été expulsé de la nation.");
        if (target != null) {
            MessageUtil.sendError(target, "Vous avez été expulsé de la nation §6" + nation.getName() + "§c.");
        }
    }

    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation promote <joueur>");
            return;
        }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        NationMember promoter = nation.getMember(player.getUniqueId());
        if (promoter == null || promoter.getRole().getRank() < NationRole.CO_LEADER.getRank()) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission de promouvoir.");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Joueur introuvable."); return; }
        NationMember targetMember = nation.getMember(target.getUniqueId());
        if (targetMember == null) { MessageUtil.sendError(player, "Ce joueur n'est pas dans votre nation."); return; }

        NationRole[] roles = NationRole.values();
        int currentIndex = -1;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i] == targetMember.getRole()) { currentIndex = i; break; }
        }
        if (currentIndex <= 0 || roles[currentIndex - 1] == NationRole.LEADER) {
            MessageUtil.sendError(player, "Ce joueur ne peut pas être promu davantage.");
            return;
        }
        targetMember.setRole(roles[currentIndex - 1]);
        plugin.getDataManager().saveNations();
        MessageUtil.sendSuccess(player, "§f" + target.getName() + " §aest maintenant §7" + targetMember.getRole().getDisplayName() + "§a.");
        MessageUtil.send(target, "§aVous avez été promu §7" + targetMember.getRole().getDisplayName() + " §adans §6" + nation.getName() + "§a.");
    }

    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation demote <joueur>");
            return;
        }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        NationMember demoter = nation.getMember(player.getUniqueId());
        if (demoter == null || !nation.isLeader(player.getUniqueId()) && demoter.getRole().getRank() < NationRole.CO_LEADER.getRank()) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission de rétrograder.");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Joueur introuvable."); return; }
        NationMember targetMember = nation.getMember(target.getUniqueId());
        if (targetMember == null) { MessageUtil.sendError(player, "Ce joueur n'est pas dans votre nation."); return; }

        NationRole[] roles = NationRole.values();
        int currentIndex = -1;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i] == targetMember.getRole()) { currentIndex = i; break; }
        }
        if (currentIndex >= roles.length - 1) {
            MessageUtil.sendError(player, "Ce joueur est déjà au rang le plus bas.");
            return;
        }
        targetMember.setRole(roles[currentIndex + 1]);
        plugin.getDataManager().saveNations();
        MessageUtil.sendSuccess(player, "§f" + target.getName() + " §aest maintenant §7" + targetMember.getRole().getDisplayName() + "§a.");
        MessageUtil.send(target, "§cVous avez été rétrogradé §7" + targetMember.getRole().getDisplayName() + " §cdans §6" + nation.getName() + "§c.");
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation ally <nation>");
            return;
        }
        Nation myNation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (myNation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        NationMember member = myNation.getMember(player.getUniqueId());
        if (member == null || member.getRole().getRank() < NationRole.CO_LEADER.getRank()) {
            MessageUtil.sendError(player, "Seuls les co-chefs et chefs peuvent gérer les alliances.");
            return;
        }
        Nation targetNation = plugin.getNationManager().getNationByName(args[1]);
        if (targetNation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
        if (myNation.getId().equals(targetNation.getId())) { MessageUtil.sendError(player, "Vous ne pouvez pas vous allier à vous-même."); return; }

        if (targetNation.hasAllyRequest(myNation.getId())) {
            plugin.getNationManager().acceptAlliance(targetNation, myNation);
            MessageUtil.sendSuccess(player, "Alliance acceptée avec §6" + targetNation.getName() + "§a!");
            notifyNationMembers(myNation, null, "§7⚑ Alliance formée avec §6" + targetNation.getName() + "§7!");
            notifyNationMembers(targetNation, null, "§7⚑ Alliance formée avec §6" + myNation.getName() + "§7!");
        } else {
            plugin.getNationManager().requestAlliance(myNation, targetNation);
            MessageUtil.sendSuccess(player, "Demande d'alliance envoyée à §6" + targetNation.getName() + "§a.");
            Player leaderOnline = plugin.getServer().getPlayer(targetNation.getLeaderId());
            if (leaderOnline != null) {
                MessageUtil.send(leaderOnline, "§6" + myNation.getName() + " §7demande une alliance. Tapez §e/nation ally " + myNation.getName() + " §7pour accepter.");
            }
        }
    }

    private void handleUnally(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation unally <nation>");
            return;
        }
        Nation myNation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (myNation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        Nation targetNation = plugin.getNationManager().getNationByName(args[1]);
        if (targetNation == null || !myNation.isAlly(targetNation.getId())) {
            MessageUtil.sendError(player, "Cette nation n'est pas votre alliée.");
            return;
        }
        plugin.getNationManager().breakAlliance(myNation, targetNation);
        MessageUtil.sendSuccess(player, "Alliance rompue avec §6" + targetNation.getName() + "§a.");
        notifyNationMembers(targetNation, null, "§c⚑ L'alliance avec §6" + myNation.getName() + " §ca été rompue.");
    }

    private void handleInfo(Player player, String[] args) {
        Nation nation;
        if (args.length < 2) {
            nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
            if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        } else {
            nation = plugin.getNationManager().getNationByName(args[1]);
            if (nation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
        }
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, nation.getName());
        NationMember leaderMember = nation.getMember(nation.getLeaderId());
        String leaderName = leaderMember != null ? leaderMember.getPlayerName() : "Inconnu";
        MessageUtil.sendRaw(player, "  §7Chef: §f" + leaderName);
        MessageUtil.sendRaw(player, "  §7Membres: §f" + nation.getMemberCount());
        MessageUtil.sendRaw(player, "  §7Claims: §f" + plugin.getTerritoryManager().getClaimCountForNation(nation.getId()));
        MessageUtil.sendRaw(player, "  §7Points saison: §e" + nation.getSeasonPoints());
        MessageUtil.sendRaw(player, "  §7Alliés: §f" + nation.getAllies().size());
        MessageUtil.sendRaw(player, "  §7Statut: " + (nation.isOpen() ? "§aOuverte" : "§cFermée"));
        if (!nation.getDescription().isEmpty()) {
            MessageUtil.sendRaw(player, "  §7Description: §f" + nation.getDescription());
        }
        MessageUtil.sendSeparator(player);
    }

    private void handleList(Player player) {
        List<Nation> nations = new ArrayList<>(plugin.getNationManager().getAllNations());
        nations.sort(Comparator.comparingInt(Nation::getSeasonPoints).reversed());
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Nations");
        if (nations.isEmpty()) {
            MessageUtil.sendRaw(player, "  §7Aucune nation créée.");
        } else {
            for (int i = 0; i < Math.min(10, nations.size()); i++) {
                Nation n = nations.get(i);
                MessageUtil.sendRaw(player, "  §7" + (i + 1) + ". §6" + n.getName() + " §7— §f" + n.getMemberCount() + " membres §7| §e" + n.getSeasonPoints() + " pts");
            }
            if (nations.size() > 10) {
                MessageUtil.sendRaw(player, "  §7... et " + (nations.size() - 10) + " autres nations.");
            }
        }
        MessageUtil.sendSeparator(player);
    }

    private void handleDisband(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        if (!nation.isLeader(player.getUniqueId())) { MessageUtil.sendError(player, "Seul le chef peut dissoudre la nation."); return; }
        new ConfirmDisbandGui(plugin, player, nation).open();
    }

    private void handleOpen(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        if (!nation.isLeader(player.getUniqueId())) { MessageUtil.sendError(player, "Seul le chef peut changer ce paramètre."); return; }
        nation.setOpen(!nation.isOpen());
        plugin.getDataManager().saveNations();
        MessageUtil.sendSuccess(player, "La nation est maintenant " + (nation.isOpen() ? "§aOuverte" : "§cFermée") + "§a.");
    }

    private void handleDescription(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation description <texte>");
            return;
        }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        NationMember member = nation.getMember(player.getUniqueId());
        if (member == null || member.getRole().getRank() < NationRole.CO_LEADER.getRank()) {
            MessageUtil.sendError(player, "Permission insuffisante.");
            return;
        }
        String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (desc.length() > plugin.getConfig().getInt("nations.max-description-length", 100)) {
            MessageUtil.sendError(player, "La description est trop longue.");
            return;
        }
        nation.setDescription(desc);
        plugin.getDataManager().saveNations();
        MessageUtil.sendSuccess(player, "Description mise à jour.");
    }

    private void handleRename(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation rename <nouveau-nom>");
            return;
        }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        if (!nation.isLeader(player.getUniqueId())) { MessageUtil.sendError(player, "Seul le chef peut renommer la nation."); return; }
        String newName = args[1];
        if (!newName.matches("[a-zA-Z0-9_]+") || newName.length() < 3 || newName.length() > 20) {
            MessageUtil.sendError(player, "Nom invalide.");
            return;
        }
        if (plugin.getNationManager().getNationByName(newName) != null) {
            MessageUtil.sendError(player, "Ce nom est déjà pris.");
            return;
        }
        String oldName = nation.getName();
        nation.setName(newName);
        plugin.getDataManager().saveNations();
        MessageUtil.sendSuccess(player, "Nation renommée de §6" + oldName + " §àvers §6" + newName + "§a.");
    }

    private void handleBankDeposit(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation deposit <montant>");
            return;
        }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) throw new NumberFormatException();
            if (!plugin.getEconomyManager().has(player.getUniqueId(), amount)) {
                MessageUtil.sendError(player, "Fonds insuffisants.");
                return;
            }
            plugin.getEconomyManager().withdraw(player.getUniqueId(), amount);
            nation.depositToBank(amount);
            plugin.getDataManager().saveNations();
            MessageUtil.sendSuccess(player, "§e" + MessageUtil.formatNumber(amount) + " coins §adéposés dans la banque de la nation.");
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Montant invalide.");
        }
    }

    private void handleBankWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation withdraw <montant>");
            return;
        }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        NationMember member = nation.getMember(player.getUniqueId());
        if (member == null || !member.canManageBank()) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission de retirer de l'argent.");
            return;
        }
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) throw new NumberFormatException();
            if (!nation.withdrawFromBank(amount)) {
                MessageUtil.sendError(player, "Fonds insuffisants dans la banque.");
                return;
            }
            plugin.getEconomyManager().deposit(player.getUniqueId(), amount);
            plugin.getDataManager().saveNations();
            MessageUtil.sendSuccess(player, "§e" + MessageUtil.formatNumber(amount) + " coins §aretirés de la banque.");
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Montant invalide.");
        }
    }

    private void notifyNationMembers(Nation nation, UUID exclude, String message) {
        for (UUID memberId : nation.getMembers().keySet()) {
            if (exclude != null && exclude.equals(memberId)) continue;
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                member.sendMessage(MessageUtil.colorize(plugin.getConfigManager().getPrefix() + message));
            }
        }
    }

    private void sendHelp(Player player) {
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Nations - Aide");
        MessageUtil.sendRaw(player, "  §e/nation §7— Ouvrir l'interface");
        MessageUtil.sendRaw(player, "  §e/nation create <nom> §7— Créer une nation");
        MessageUtil.sendRaw(player, "  §e/nation invite <joueur> §7— Inviter un joueur");
        MessageUtil.sendRaw(player, "  §e/nation join <nation> §7— Rejoindre une nation ouverte");
        MessageUtil.sendRaw(player, "  §e/nation leave §7— Quitter votre nation");
        MessageUtil.sendRaw(player, "  §e/nation kick <joueur> §7— Expulser un joueur");
        MessageUtil.sendRaw(player, "  §e/nation promote/demote <joueur> §7— Gérer les rôles");
        MessageUtil.sendRaw(player, "  §e/nation ally/unally <nation> §7— Gérer les alliances");
        MessageUtil.sendRaw(player, "  §e/nation info [nation] §7— Informations");
        MessageUtil.sendRaw(player, "  §e/nation list §7— Liste des nations");
        MessageUtil.sendRaw(player, "  §e/nation deposit/withdraw <montant> §7— Banque");
        MessageUtil.sendSeparator(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "create", "invite", "join", "leave", "kick", "promote", "demote",
                "ally", "unally", "info", "list", "disband", "open", "description",
                "rename", "deposit", "withdraw", "accept", "gui"
            );
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "invite", "kick", "promote", "demote" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(p.getName());
                        }
                    }
                }
                case "join", "info", "ally", "unally", "accept" -> {
                    for (Nation n : plugin.getNationManager().getAllNations()) {
                        if (n.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(n.getName());
                        }
                    }
                }
                case "deposit", "withdraw" -> completions.addAll(Arrays.asList("100", "500", "1000", "5000"));
            }
        }
        return completions;
    }

    private void handleRoleGui(Player player, String[] args) {
        if (!player.hasPermission("nations.admin")) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /nation role <LEADER|CO_LEADER|OFFICER|MEMBER|RECRUIT>");
            return;
        }
        fr.nations.nation.NationRole role;
        try {
            role = fr.nations.nation.NationRole.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtil.sendError(player, "Rôle invalide. Rôles: LEADER, CO_LEADER, OFFICER, MEMBER, RECRUIT");
            return;
        }
        new fr.nations.gui.RolePermissionsGui(plugin, player, role).open();
    }

    private org.bukkit.configuration.file.FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
