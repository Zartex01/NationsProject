package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
import fr.nations.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class KitCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public KitCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length == 0) {
            sendKitHelp(player);
            return true;
        }

        String kitName = args[0].toLowerCase();
        GradeType playerGrade = plugin.getGradeManager().getEffectiveGrade(player);
        GradeType requiredGrade = resolveKit(kitName);

        if (requiredGrade == null) {
            MessageUtil.sendError(player, "Kit inconnu. Kits disponibles : heros, chevalier, premium.");
            return true;
        }

        if (playerGrade.ordinal() < requiredGrade.ordinal()) {
            MessageUtil.sendError(player,
                "Ce kit nécessite le grade " + requiredGrade.getColoredDisplay() + " &cou supérieur.");
            return true;
        }

        if (plugin.getKitManager().isOnCooldown(player, requiredGrade)) {
            long remaining = plugin.getKitManager().getRemainingCooldownSeconds(player, requiredGrade);
            MessageUtil.sendError(player,
                "Kit en recharge ! Disponible dans : &e" + plugin.getKitManager().formatCooldown(remaining));
            return true;
        }

        plugin.getKitManager().giveKit(player, requiredGrade);
        plugin.getKitManager().setCooldown(player, requiredGrade);
        MessageUtil.send(player, "&aKit &6" + requiredGrade.getDisplayName() + " &areçu ! (Recharge : 24h)");
        return true;
    }

    private GradeType resolveKit(String name) {
        return switch (name) {
            case "heros", "héros", "hero" -> GradeType.HEROS;
            case "chevalier"              -> GradeType.CHEVALIER;
            case "premium"                -> GradeType.PREMIUM;
            default                       -> null;
        };
    }

    private void sendKitHelp(Player player) {
        GradeType grade = plugin.getGradeManager().getEffectiveGrade(player);
        player.sendMessage("§8§m-----§r §6Kits §8§m-----");
        sendKitLine(player, grade, GradeType.HEROS,     "Kit Héros",     "Armure fer Prot II, épée diamant Sharp II...");
        sendKitLine(player, grade, GradeType.CHEVALIER, "Kit Chevalier", "Armure fer Prot III, épée diamant Sharp III...");
        sendKitLine(player, grade, GradeType.PREMIUM,   "Kit Premium",   "Armure diamant Prot III, épée diamant Sharp IV...");
        player.sendMessage("§7Usage : §e/kit <nom>");
    }

    private void sendKitLine(Player player, GradeType playerGrade, GradeType required, String name, String desc) {
        boolean hasAccess = playerGrade.ordinal() >= required.ordinal();
        String status = hasAccess ? "§a✔" : "§c✘ §7(nécessite " + required.getColoredDisplay() + "§7)";
        player.sendMessage("§6" + name + " §8- §7" + desc + " " + status);
        if (hasAccess && plugin.getKitManager().isOnCooldown(player, required)) {
            long rem = plugin.getKitManager().getRemainingCooldownSeconds(player, required);
            player.sendMessage("  §7Recharge : §c" + plugin.getKitManager().formatCooldown(rem));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("heros", "chevalier", "premium").stream()
                .filter(k -> k.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}
