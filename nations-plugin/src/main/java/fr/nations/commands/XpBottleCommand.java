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

import java.util.List;
import java.util.Map;

public class XpBottleCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public XpBottleCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendError(player, "Usage : /xpb <nombre-de-bouteilles>");
            MessageUtil.send(player, "&7Coût : &e1 niveau d'XP &7par bouteille.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Veuillez entrer un nombre entier valide.");
            return true;
        }

        if (amount <= 0) {
            MessageUtil.sendError(player, "Le nombre de bouteilles doit être supérieur à 0.");
            return true;
        }

        int playerLevel = player.getLevel();
        if (playerLevel < amount) {
            MessageUtil.sendError(player,
                "Vous n'avez pas assez de niveaux ! (Vous avez &e" + playerLevel + " &cniveaux, il vous faut &e" + amount + "&c)");
            return true;
        }

        player.setLevel(playerLevel - amount);
        ItemStack bottles = new ItemStack(Material.EXPERIENCE_BOTTLE, amount);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(bottles);
        leftover.values().forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));

        MessageUtil.send(player, "&aConverti &e" + amount + " &aniveau(x) d'XP en &e" + amount + " &abouteille(s) d'expérience !");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("1", "5", "10", "32", "64");
        return List.of();
    }
}
