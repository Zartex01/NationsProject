package fr.nations.kits;

import fr.nations.NationsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KitCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public KitCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            new KitGui(plugin, player).open();
            return true;
        }

        String kitName = args[0].toUpperCase();
        KitType kit;
        try {
            kit = KitType.valueOf(kitName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cKit inconnu : §f" + args[0]);
            player.sendMessage("§7Kits disponibles : soutien, premium, heros");
            return true;
        }

        KitManager km = plugin.getKitManager();

        if (!kit.isAccessibleBy(fr.nations.grade.GradeType.fromPermission(player))) {
            player.sendMessage("§c✖ Tu dois être au moins "
                + kit.getRequiredGrade().getColoredDisplay()
                + " §cpour utiliser ce kit.");
            return true;
        }

        long remaining = km.remainingCooldown(player.getUniqueId(), kit);
        if (remaining > 0) {
            player.sendMessage("§e⏳ Kit §r" + kit.getColoredName()
                + " §een recharge — §f" + KitManager.formatDuration(remaining) + " §erestantes.");
            return true;
        }

        // Donner les items
        for (org.bukkit.inventory.ItemStack item : kit.getItems()) {
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            } else {
                player.getInventory().addItem(item);
            }
        }

        km.recordClaim(player.getUniqueId(), kit);

        player.sendMessage("");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("  §a§l✔ KIT RÉCLAMÉ — " + kit.getColoredName());
        player.sendMessage("  §7Tu as reçu le contenu du kit §r" + kit.getColoredName() + "§7.");
        player.sendMessage("  §7Prochain claim dans : §f"
            + KitManager.formatDuration(kit.getCooldownSeconds()));
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(KitType.values())
                .map(k -> k.name().toLowerCase())
                .filter(n -> n.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
