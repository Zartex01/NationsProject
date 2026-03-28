package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.gui.HdvGui;
import fr.nations.gui.HdvSellConfirmGui;
import fr.nations.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class HdvChatListener implements Listener {

    private final NationsPlugin plugin;

    public HdvChatListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getHdvManager().hasPendingPriceInput(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("annuler") || message.equalsIgnoreCase("cancel")) {
            ItemStack item = plugin.getHdvManager().getPendingPriceInput(player.getUniqueId());
            plugin.getHdvManager().clearPendingPriceInput(player.getUniqueId());
            if (item != null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                    leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
                });
            }
            MessageUtil.send(player, "&7Mise en vente annulée. L'objet vous a été rendu.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(message.replace(",", ".").replace(" ", ""));
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Prix invalide. Entrez un nombre (ex: &e500&c) ou &eAnnuler&c.");
            return;
        }

        if (price <= 0) {
            MessageUtil.sendError(player, "Le prix doit être supérieur à 0 coin.");
            return;
        }
        if (price > 100_000_000) {
            MessageUtil.sendError(player, "Le prix maximum est &e100 000 000 coins&c.");
            return;
        }

        ItemStack item = plugin.getHdvManager().getPendingPriceInput(player.getUniqueId());
        plugin.getHdvManager().clearPendingPriceInput(player.getUniqueId());

        if (item == null) {
            MessageUtil.sendError(player, "Erreur interne — veuillez réessayer.");
            return;
        }

        final double finalPrice = price;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            new HdvSellConfirmGui(plugin, player, item, finalPrice).open();
        });
    }
}
