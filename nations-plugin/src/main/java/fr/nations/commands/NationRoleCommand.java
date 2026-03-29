package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.role.CustomRole;
import fr.nations.role.CustomRoleManager;
import fr.nations.role.RolePermission;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /nationrole <create|delete|setperm|assign|list|info|rename|setrank>
 */
public class NationRoleCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;
    private static final int MAX_ROLES = 10;

    public NationRoleCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Joueurs uniquement.");
            return true;
        }

        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) {
            MessageUtil.sendError(player, "Vous n'avez pas de nation.");
            return true;
        }

        NationMember member = nation.getMember(player.getUniqueId());
        if (member == null) return true;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        boolean isLeaderOrCo = member.getRole() == NationRole.LEADER || member.getRole() == NationRole.CO_LEADER;
        boolean hasRolesPerm = isLeaderOrCo
            || plugin.getCustomRoleManager().hasPermission(player.getUniqueId(), RolePermission.MANAGE_ROLES);

        switch (args[0].toLowerCase()) {
            case "list", "liste" -> handleList(player, nation);
            case "info" -> handleInfo(player, nation, args);
            case "create", "creer" -> {
                if (args.length == 1) {
                    new fr.nations.gui.NationRolesGui(plugin, player, nation).open();
                    return true;
                }
                if (!hasRolesPerm) { MessageUtil.sendError(player, "Permission insuffisante."); return true; }
                handleCreate(player, nation, args);
            }
            case "delete", "supprimer" -> {
                if (!hasRolesPerm) { MessageUtil.sendError(player, "Permission insuffisante."); return true; }
                handleDelete(player, nation, args);
            }
            case "setperm", "permission" -> {
                if (!hasRolesPerm) { MessageUtil.sendError(player, "Permission insuffisante."); return true; }
                handleSetPerm(player, nation, args);
            }
            case "assign", "attribuer" -> {
                if (!hasRolesPerm) { MessageUtil.sendError(player, "Permission insuffisante."); return true; }
                handleAssign(player, nation, args);
            }
            case "unassign", "retirer" -> {
                if (!hasRolesPerm) { MessageUtil.sendError(player, "Permission insuffisante."); return true; }
                handleUnassign(player, nation, args);
            }
            case "rename", "renommer" -> {
                if (!hasRolesPerm) { MessageUtil.sendError(player, "Permission insuffisante."); return true; }
                handleRename(player, nation, args);
            }
            case "setrank" -> {
                if (!hasRolesPerm) { MessageUtil.sendError(player, "Permission insuffisante."); return true; }
                handleSetRank(player, nation, args);
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleList(Player player, Nation nation) {
        List<CustomRole> roles = plugin.getCustomRoleManager().getRolesForNation(nation.getId());
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Rôles de " + nation.getName() + " §8(" + roles.size() + "/" + MAX_ROLES + ")");
        if (roles.isEmpty()) {
            player.sendMessage("  §7Aucun rôle personnalisé. Créez-en avec §e/nationrole create <nom> <rang>");
        } else {
            for (CustomRole role : roles) {
                player.sendMessage("  §6" + role.getDisplayName()
                    + " §8[rang " + role.getRank() + "] §7— §e/nationrole info " + role.getName());
            }
        }
        MessageUtil.sendSeparator(player);
    }

    private void handleInfo(Player player, Nation nation, String[] args) {
        if (args.length < 2) { MessageUtil.sendError(player, "Usage: /nationrole info <nom>"); return; }
        CustomRole role = plugin.getCustomRoleManager().getRoleByName(nation.getId(), args[1]);
        if (role == null) { MessageUtil.sendError(player, "Rôle introuvable."); return; }

        MessageUtil.sendSeparator(player);
        player.sendMessage("§6§l" + role.getDisplayName() + " §8(rang " + role.getRank() + ")");
        player.sendMessage("§7Nom interne: §f" + role.getName());
        player.sendMessage("§7Permissions:");
        for (RolePermission perm : RolePermission.values()) {
            boolean has = role.hasPermission(perm);
            player.sendMessage("  " + (has ? "§a✔ " : "§c✘ ") + perm.getLabel() + " §8— §7" + perm.getDescription());
        }
        player.sendMessage("§7Changer: §e/nationrole setperm " + role.getName() + " <permission> <true|false>");
        MessageUtil.sendSeparator(player);
    }

    private void handleCreate(Player player, Nation nation, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(player, "Usage: /nationrole create <nom_interne> <rang> [nom_affichage...]");
            return;
        }
        String name = args[1].toLowerCase();
        if (name.length() > 16) { MessageUtil.sendError(player, "Le nom ne peut pas dépasser 16 caractères."); return; }
        if (!name.matches("[a-z0-9_]+")) { MessageUtil.sendError(player, "Le nom ne peut contenir que des lettres, chiffres et underscores."); return; }

        int rank;
        try { rank = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) { MessageUtil.sendError(player, "Le rang doit être un nombre entier."); return; }
        if (rank < 1 || rank > 999) { MessageUtil.sendError(player, "Le rang doit être entre 1 et 999."); return; }

        if (plugin.getCustomRoleManager().getRoleByName(nation.getId(), name) != null) {
            MessageUtil.sendError(player, "Un rôle avec ce nom existe déjà.");
            return;
        }
        if (plugin.getCustomRoleManager().getRoleCount(nation.getId()) >= MAX_ROLES) {
            MessageUtil.sendError(player, "Limite de " + MAX_ROLES + " rôles atteinte.");
            return;
        }

        String displayName = args.length >= 4
            ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
            : name;

        CustomRole role = plugin.getCustomRoleManager().createRole(nation.getId(), name, displayName, rank);
        MessageUtil.sendSuccess(player, "Rôle §6" + displayName + " §acréé avec le rang §e" + rank + "§a.");
        player.sendMessage("§7Configurez ses permissions: §e/nationrole setperm " + name + " <permission> true/false");
    }

    private void handleDelete(Player player, Nation nation, String[] args) {
        if (args.length < 2) { MessageUtil.sendError(player, "Usage: /nationrole delete <nom>"); return; }
        CustomRole role = plugin.getCustomRoleManager().getRoleByName(nation.getId(), args[1]);
        if (role == null) { MessageUtil.sendError(player, "Rôle introuvable."); return; }
        plugin.getCustomRoleManager().deleteRole(role.getId());
        MessageUtil.sendSuccess(player, "Rôle §6" + role.getDisplayName() + " §asupprimé.");
    }

    private void handleSetPerm(Player player, Nation nation, String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(player, "Usage: /nationrole setperm <nom> <permission> <true|false>");
            player.sendMessage("§7Permissions: " + getPermList());
            return;
        }
        CustomRole role = plugin.getCustomRoleManager().getRoleByName(nation.getId(), args[1]);
        if (role == null) { MessageUtil.sendError(player, "Rôle introuvable."); return; }

        RolePermission perm;
        try { perm = RolePermission.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) {
            MessageUtil.sendError(player, "Permission invalide. Valeurs: " + getPermList());
            return;
        }

        boolean value = args[3].equalsIgnoreCase("true") || args[3].equalsIgnoreCase("oui");
        role.setPermission(perm, value);
        plugin.getCustomRoleManager().saveRole(role);

        MessageUtil.sendSuccess(player, "Permission §e" + perm.getLabel() + " §a"
            + (value ? "activée" : "désactivée") + " §apour le rôle §6" + role.getDisplayName() + "§a.");
    }

    private void handleAssign(Player player, Nation nation, String[] args) {
        if (args.length < 3) { MessageUtil.sendError(player, "Usage: /nationrole assign <joueur> <nom_role>"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Joueur introuvable ou hors ligne."); return; }

        if (nation.getMember(target.getUniqueId()) == null) {
            MessageUtil.sendError(player, "Ce joueur n'est pas dans votre nation.");
            return;
        }

        CustomRole role = plugin.getCustomRoleManager().getRoleByName(nation.getId(), args[2]);
        if (role == null) { MessageUtil.sendError(player, "Rôle introuvable."); return; }

        plugin.getCustomRoleManager().assignPlayerRole(target.getUniqueId(), nation.getId(), role.getId());
        MessageUtil.sendSuccess(player, "Rôle §6" + role.getDisplayName() + " §aattribué à §f" + target.getName() + "§a.");
        target.sendMessage("§6[Nation] §7Vous avez reçu le rôle §6" + role.getDisplayName() + " §7dans §e" + nation.getName() + "§7.");
    }

    private void handleUnassign(Player player, Nation nation, String[] args) {
        if (args.length < 2) { MessageUtil.sendError(player, "Usage: /nationrole unassign <joueur>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(player, "Joueur introuvable."); return; }
        plugin.getCustomRoleManager().removePlayerRole(target.getUniqueId(), nation.getId());
        MessageUtil.sendSuccess(player, "Rôle retiré à §f" + target.getName() + "§a.");
    }

    private void handleRename(Player player, Nation nation, String[] args) {
        if (args.length < 3) { MessageUtil.sendError(player, "Usage: /nationrole rename <nom> <nouveau_nom>"); return; }
        CustomRole role = plugin.getCustomRoleManager().getRoleByName(nation.getId(), args[1]);
        if (role == null) { MessageUtil.sendError(player, "Rôle introuvable."); return; }
        String newDisplay = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        role.setDisplayName(newDisplay);
        plugin.getCustomRoleManager().saveRole(role);
        MessageUtil.sendSuccess(player, "Rôle renommé en §6" + newDisplay + "§a.");
    }

    private void handleSetRank(Player player, Nation nation, String[] args) {
        if (args.length < 3) { MessageUtil.sendError(player, "Usage: /nationrole setrank <nom> <rang>"); return; }
        CustomRole role = plugin.getCustomRoleManager().getRoleByName(nation.getId(), args[1]);
        if (role == null) { MessageUtil.sendError(player, "Rôle introuvable."); return; }
        try {
            int rank = Integer.parseInt(args[2]);
            if (rank < 1 || rank > 999) { MessageUtil.sendError(player, "Rang entre 1 et 999."); return; }
            role.setRank(rank);
            plugin.getCustomRoleManager().saveRole(role);
            MessageUtil.sendSuccess(player, "Rang du rôle §6" + role.getDisplayName() + " §amis à §e" + rank + "§a.");
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Rang invalide.");
        }
    }

    private String getPermList() {
        StringBuilder sb = new StringBuilder();
        for (RolePermission perm : RolePermission.values()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(perm.name().toLowerCase());
        }
        return sb.toString();
    }

    private void sendHelp(Player player) {
        MessageUtil.sendSeparator(player);
        MessageUtil.sendTitle(player, "Rôles Personnalisés — Aide");
        player.sendMessage("  §e/nationrole create §7— Ouvrir le gestionnaire de rôles (GUI)");
        player.sendMessage("  §e/nationrole create <nom> <rang> [affichage] §7— Créer un rôle (texte)");
        player.sendMessage("  §e/nationrole list §7— Voir les rôles de votre nation");
        player.sendMessage("  §e/nationrole info <nom> §7— Détails d'un rôle");
        player.sendMessage("  §e/nationrole delete <nom> §7— Supprimer un rôle");
        player.sendMessage("  §e/nationrole setperm <nom> <perm> <true|false> §7— Modifier permission");
        player.sendMessage("  §e/nationrole assign <joueur> <nom> §7— Attribuer un rôle");
        player.sendMessage("  §e/nationrole unassign <joueur> §7— Retirer un rôle");
        player.sendMessage("  §e/nationrole rename <nom> <nouveau> §7— Renommer");
        player.sendMessage("  §e/nationrole setrank <nom> <rang> §7— Modifier le rang");
        player.sendMessage("  §7Permissions: §f" + getPermList());
        MessageUtil.sendSeparator(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player player)) return completions;

        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) return completions;

        if (args.length == 1) {
            List.of("list", "info", "create", "delete", "setperm", "assign", "unassign", "rename", "setrank")
                .stream().filter(s -> s.startsWith(args[0].toLowerCase())).forEach(completions::add);
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
            plugin.getCustomRoleManager().getRolesForNation(nation.getId())
                .stream()
                .filter(r -> r.getName().startsWith(args[1].toLowerCase()))
                .forEach(r -> completions.add(r.getName()));
            if (args[0].equalsIgnoreCase("assign") || args[0].equalsIgnoreCase("unassign")) {
                Bukkit.getOnlinePlayers().stream()
                    .filter(p -> nation.getMember(p.getUniqueId()) != null)
                    .filter(p -> p.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                    .forEach(p -> completions.add(p.getName()));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setperm")) {
            Arrays.stream(RolePermission.values())
                .filter(p -> p.name().toLowerCase().startsWith(args[2].toLowerCase()))
                .forEach(p -> completions.add(p.name().toLowerCase()));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("setperm")) {
            completions.addAll(List.of("true", "false"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("assign")) {
            plugin.getCustomRoleManager().getRolesForNation(nation.getId())
                .stream()
                .filter(r -> r.getName().startsWith(args[2].toLowerCase()))
                .forEach(r -> completions.add(r.getName()));
        }
        return completions;
    }
}
