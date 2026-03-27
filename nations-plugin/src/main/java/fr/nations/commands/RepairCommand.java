package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RepairCommand implements CommandExecutor, TabCompleter {

    private static final long REPAIR_COOLDOWN_MS     = 60L * 60 * 1000;
    private static final long REPAIR_ALL_COOLDOWN_MS = 24L * 60 * 60 * 1000;

    private final NationsPlugin plugin;
    private final Map<UUID, Long> repairCooldowns    = new HashMap<>();
    private final Map<UUID, Long> repairAllCooldowns = new HashMap<>();

    public RepairCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        boolean isAll = args.length > 0 && args[0].equalsIgnoreCase("all");

        if (isAll) {
            if (!player.hasPermission("nations.grade.premium") && !player.hasPermission("nations.admin")) {
                MessageUtil.sendError(player, "Vous n'avez pas la permission d'utiliser /repair all.");
                return true;
            }
            long remaining = getRemaining(repairAllCooldowns, player.getUniqueId(), REPAIR_ALL_COOLDOWN_MS);
            if (remaining > 0 && !player.hasPermission("nations.admin")) {
                MessageUtil.sendError(player, "Repair all en recharge ! Disponible dans : &e" + formatCooldown(remaining / 1000));
                return true;
            }
            int count = repairAll(player);
            repairAllCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            MessageUtil.send(player, "&aRéparé &e" + count + " &aobjet(s) ! (Recharge : 24h)");
        } else {
            long remaining = getRemaining(repairCooldowns, player.getUniqueId(), REPAIR_COOLDOWN_MS);
            if (remaining > 0 && !player.hasPermission("nations.admin")) {
                MessageUtil.sendError(player, "Repair en recharge ! Disponible dans : &e" + formatCooldown(remaining / 1000));
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                MessageUtil.sendError(player, "Tenez un objet dans votre main principale.");
                return true;
            }
            if (!repairItem(hand)) {
                MessageUtil.sendError(player, "Cet objet ne peut pas être réparé.");
                return true;
            }
            repairCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            MessageUtil.send(player, "&aObjet réparé ! (Recharge : 1h)");
        }
        return true;
    }

    private int repairAll(Player player) {
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR && repairItem(item)) {
                count++;
            }
        }
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            if (item != null && item.getType() != Material.AIR && repairItem(item)) {
                count++;
            }
        }
        return count;
    }

    private boolean repairItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return false;
        if (damageable.getDamage() == 0) return false;
        damageable.setDamage(0);
        item.setItemMeta(meta);
        return true;
    }

    private long getRemaining(Map<UUID, Long> map, UUID id, long cooldownMs) {
        Long last = map.get(id);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0, cooldownMs - elapsed);
    }

    private String formatCooldown(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("all").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}
