package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.CampfireRecipe;

import java.util.Iterator;

public class FurnaceCommand implements CommandExecutor {

    private final NationsPlugin plugin;

    public FurnaceCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Joueurs uniquement.");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.AIR || itemInHand.getAmount() == 0) {
            MessageUtil.sendError(player, "Tenez un item en main pour le fondre/cuire.");
            return true;
        }

        ItemStack result = getSmeltResult(itemInHand);
        if (result == null) {
            MessageUtil.sendError(player, "§e" + formatName(itemInHand.getType()) + " §cne peut pas être fondu/cuit.");
            return true;
        }

        int inputAmount = itemInHand.getAmount();
        int outputPerItem = result.getAmount();
        int totalOutput = inputAmount * outputPerItem;

        // Remplacer l'item en main par le résultat
        ItemStack outputStack = result.clone();
        outputStack.setAmount(Math.min(totalOutput, 64));

        player.getInventory().setItemInMainHand(outputStack);

        // Si le résultat dépasse 64, donner le reste dans l'inventaire
        if (totalOutput > 64) {
            int overflow = totalOutput - 64;
            ItemStack overflowStack = result.clone();
            overflowStack.setAmount(overflow);
            player.getInventory().addItem(overflowStack).forEach((k, v) ->
                player.getWorld().dropItemNaturally(player.getLocation(), v));
        }

        MessageUtil.sendSuccess(player, "§e" + inputAmount + "x " + formatName(itemInHand.getType())
                + " §a→ §e" + totalOutput + "x " + formatName(result.getType()) + "§a.");
        return true;
    }

    private ItemStack getSmeltResult(ItemStack input) {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof FurnaceRecipe r && r.getInputChoice().test(input)) return r.getResult();
            if (recipe instanceof BlastingRecipe r && r.getInputChoice().test(input)) return r.getResult();
            if (recipe instanceof SmokingRecipe r && r.getInputChoice().test(input)) return r.getResult();
            if (recipe instanceof CampfireRecipe r && r.getInputChoice().test(input)) return r.getResult();
        }
        return null;
    }

    private String formatName(Material mat) {
        String[] words = mat.name().toLowerCase().replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
