package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmokingRecipe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FurnaceCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    private volatile Map<Material, Material> smeltCache;

    public FurnaceCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            if (!player.hasPermission("nations.grade.premium") && !player.hasPermission("nations.admin")) {
                MessageUtil.sendError(player, "Vous n'avez pas la permission d'utiliser /furnace all.");
                return true;
            }
            smeltAll(player);
            return true;
        }

        smeltHand(player);
        return true;
    }

    private void smeltHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            MessageUtil.sendError(player, "Vous n'avez rien dans la main !");
            return;
        }
        Material smelted = getSmeltResult(item.getType());
        if (smelted == null) {
            MessageUtil.sendError(player, "Cet objet ne peut pas être cuit.");
            return;
        }
        int amount = item.getAmount();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        ItemStack result = new ItemStack(smelted, amount);
        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        leftover.values().forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
        MessageUtil.send(player, "&aCuit &e" + amount + "x " + smelted.name().toLowerCase().replace("_", " ") + " &a!");
    }

    private void smeltAll(Player player) {
        int count = 0;
        Inventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;
            Material smelted = getSmeltResult(item.getType());
            if (smelted == null) continue;
            int amount = item.getAmount();
            contents[i] = null;
            ItemStack result = new ItemStack(smelted, amount);
            java.util.Map<Integer, ItemStack> leftover = inv.addItem(result);
            leftover.values().forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
            count += amount;
        }
        inv.setContents(contents);
        if (count > 0) {
            MessageUtil.send(player, "&aFusionné &e" + count + " &aobjet(s) automatiquement !");
        } else {
            MessageUtil.send(player, "&7Aucun objet fusionnable trouvé dans votre inventaire.");
        }
    }

    private Material getSmeltResult(Material input) {
        return getSmeltCache().get(input);
    }

    private Map<Material, Material> getSmeltCache() {
        if (smeltCache == null) {
            synchronized (this) {
                if (smeltCache == null) {
                    smeltCache = buildSmeltCache();
                }
            }
        }
        return smeltCache;
    }

    private Map<Material, Material> buildSmeltCache() {
        Map<Material, Material> cache = new HashMap<>();
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();

            Material result;
            RecipeChoice choice;

            if (recipe instanceof FurnaceRecipe fr) {
                result = fr.getResult().getType();
                choice = fr.getInputChoice();
            } else if (recipe instanceof BlastingRecipe br) {
                result = br.getResult().getType();
                choice = br.getInputChoice();
            } else if (recipe instanceof SmokingRecipe sr) {
                result = sr.getResult().getType();
                choice = sr.getInputChoice();
            } else {
                continue;
            }

            if (choice instanceof RecipeChoice.MaterialChoice mc) {
                for (Material m : mc.getChoices()) {
                    cache.putIfAbsent(m, result);
                }
            } else if (choice instanceof RecipeChoice.ExactChoice ec) {
                for (ItemStack is : ec.getChoices()) {
                    cache.putIfAbsent(is.getType(), result);
                }
            }
        }
        return cache;
    }

    public void invalidateCache() {
        smeltCache = null;
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
