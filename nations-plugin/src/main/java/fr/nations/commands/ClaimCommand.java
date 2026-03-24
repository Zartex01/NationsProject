package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.territory.TerritoryManager;
import fr.nations.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public ClaimCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("here")) {
            handleClaim(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "unclaim" -> handleUnclaim(player);
            case "info" -> handleInfo(player);
            case "list", "liste" -> handleList(player, args);
            case "unclaimall" -> handleUnclaimAll(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleClaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        TerritoryManager.ClaimResult result = plugin.getTerritoryManager().claimChunk(player, chunk);

        switch (result) {
            case SUCCESS -> {
                Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
                MessageUtil.sendSuccess(player, "Chunk §f[" + chunk.getX() + ", " + chunk.getZ() + "] §aclaim pour §6"
                    + (nation != null ? nation.getName() : "votre nation") + "§a!");
                MessageUtil.sendInfo(player, "Coût: §e" + MessageUtil.formatNumber(plugin.getConfigManager().getClaimPrice()) + " coins §7débités.");
            }
            case ALREADY_CLAIMED -> {
                var claimed = plugin.getTerritoryManager().getClaimedChunk(chunk);
                if (claimed != null) {
                    Nation owner = plugin.getNationManager().getNationById(claimed.getNationId());
                    MessageUtil.sendError(player, "Ce chunk appartient à §6" + (owner != null ? owner.getName() : "une autre nation") + "§c.");
                } else {
                    MessageUtil.sendError(player, "Ce chunk est déjà claimé.");
                }
            }
            case NO_NATION -> MessageUtil.sendError(player, "Vous devez appartenir à une nation pour claimer.");
            case MAX_CLAIMS_REACHED -> {
                int max = plugin.getConfigManager().getMaxClaimsForGrade(
                    plugin.getGradeManager().getPlayerGradeName(player)
                );
                MessageUtil.sendError(player, "Vous avez atteint votre limite de " + max + " claims. Améliorez votre grade!");
            }
            case INSUFFICIENT_FUNDS -> MessageUtil.sendError(player, "Fonds insuffisants. Coût: §e"
                + MessageUtil.formatNumber(plugin.getConfigManager().getClaimPrice()) + " coins");
        }
    }

    private void handleUnclaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        if (plugin.getTerritoryManager().unclaimChunk(player, chunk)) {
            MessageUtil.sendSuccess(player, "Chunk §f[" + chunk.getX() + ", " + chunk.getZ() + "] §aunclamé.");
        } else {
            MessageUtil.sendError(player, "Ce chunk n'appartient pas à votre nation.");
        }
    }

    private void handleInfo(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        var claimed = plugin.getTerritoryManager().getClaimedChunk(chunk);

        MessageUtil.sendSeparator(player);
        if (claimed == null) {
            MessageUtil.sendRaw(player, "  §7Ce chunk est §climbre§7 (non claimé).");
        } else {
            Nation nation = plugin.getNationManager().getNationById(claimed.getNationId());
            MessageUtil.sendRaw(player, "  §7Chunk: §f[" + chunk.getX() + ", " + chunk.getZ() + "]");
            MessageUtil.sendRaw(player, "  §7Nation: §6" + (nation != null ? nation.getName() : "Inconnue"));
        }
        MessageUtil.sendSeparator(player);
    }

    private void handleList(Player player, String[] args) {
        Nation nation;
        if (args.length >= 2) {
            nation = plugin.getNationManager().getNationByName(args[1]);
            if (nation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
        } else {
            nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
            if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        }

        int count = plugin.getTerritoryManager().getClaimCountForNation(nation.getId());
        MessageUtil.sendSeparator(player);
        MessageUtil.sendRaw(player, "  §7Nation §6" + nation.getName() + " §7possède §f" + count + " §7chunks claimés.");
        MessageUtil.sendSeparator(player);
    }

    private void handleUnclaimAll(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }
        if (!nation.isLeader(player.getUniqueId())) { MessageUtil.sendError(player, "Seul le chef peut tout unclaimer."); return; }

        int count = plugin.getTerritoryManager().getClaimCountForNation(nation.getId());
        plugin.getTerritoryManager().unclaimAllForNation(nation.getId());
        MessageUtil.sendSuccess(player, "§f" + count + " §achunks unclamés.");
    }

    private void sendHelp(Player player) {
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Claim - Aide");
        MessageUtil.sendRaw(player, "  §e/claim §7— Claimer le chunk actuel");
        MessageUtil.sendRaw(player, "  §e/claim unclaim §7— Unclaimer le chunk actuel");
        MessageUtil.sendRaw(player, "  §e/claim info §7— Infos sur le chunk actuel");
        MessageUtil.sendRaw(player, "  §e/claim list [nation] §7— Liste des claims");
        MessageUtil.sendRaw(player, "  §e/claim unclaimall §7— Unclaimer tous les chunks (chef only)");
        MessageUtil.sendSeparator(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("unclaim", "info", "list", "unclaimall")) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            for (Nation n : plugin.getNationManager().getAllNations()) {
                if (n.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(n.getName());
            }
        }
        return completions;
    }
}
