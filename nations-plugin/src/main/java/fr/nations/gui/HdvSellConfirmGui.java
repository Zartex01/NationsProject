package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.hdv.HdvListing;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HdvSellConfirmGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final ItemStack item;
    private final double price;
    private boolean pubEnabled = false;
    private boolean handled = false;

    public HdvSellConfirmGui(NationsPlugin plugin, Player player, ItemStack item, double price) {
        this.plugin  = plugin;
        this.player  = player;
        this.item    = item.clone();
        this.price   = price;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Build
    // ─────────────────────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6Confirmer la mise en vente", 3);

        ItemStack gray = GuiUtil.createFillerItem(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        double taxRate  = HdvListing.TAX_RATE * 100;
        double pubRate  = HdvListing.PUB_RATE * 100;
        double totalFee = pubEnabled ? taxRate + pubRate : taxRate;
        double earnings = price * (1 - totalFee / 100.0);

        // ── Item preview (slot 13) ──
        ItemStack preview = buildPreview();
        inv.setItem(13, preview);

        // ── Confirm (slot 11) ──
        inv.setItem(11, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
            "&a&lCONFIRMER LA VENTE",
            "&7Prix affiché : &e" + MessageUtil.formatNumber(price) + " coins",
            "&7Taxes : &c-" + String.format("%.0f", totalFee) + "%",
            "&7Vous recevrez : &a" + MessageUtil.formatNumber(earnings) + " coins"
        ));

        // ── Pub toggle (slot 15) ──
        if (pubEnabled) {
            inv.setItem(15, GuiUtil.createItem(Material.LIME_DYE,
                "&a&lPUBLICITÉ : &a✔ ACTIVÉE",
                "&7Votre annonce sera broadcastée",
                "&7au chat de tous les joueurs.",
                "",
                "&c⚠ Coût supplémentaire : " + String.format("%.0f", pubRate) + "%",
                "&7(vous recevrez &a" + MessageUtil.formatNumber(earnings) + " coins&7)",
                "",
                "&eCliquez pour désactiver"
            ));
        } else {
            inv.setItem(15, GuiUtil.createItem(Material.GRAY_DYE,
                "&7&lPUBLICITÉ : &c✘ DÉSACTIVÉE",
                "&7Activez pour broadcaster",
                "&7votre annonce à tous les joueurs.",
                "",
                "&7Coût si activé : +&c" + String.format("%.0f", pubRate) + "%",
                "&8(taxe totale : " + String.format("%.0f", taxRate + pubRate) + "%)",
                "",
                "&eCliquez pour activer"
            ));
        }

        // ── Cancel (slot 19) ──
        inv.setItem(19, GuiUtil.createItem(Material.RED_STAINED_GLASS_PANE,
            "&c&lANNULER",
            "&7L'objet vous sera rendu."
        ));

        return inv;
    }

    private ItemStack buildPreview() {
        ItemStack preview = item.clone();
        ItemMeta meta = preview.getItemMeta();
        if (meta == null) return preview;

        double taxRate  = HdvListing.TAX_RATE * 100;
        double pubRate  = HdvListing.PUB_RATE * 100;
        double totalFee = pubEnabled ? taxRate + pubRate : taxRate;
        double earnings = price * (1 - totalFee / 100.0);

        List<String> lore = new ArrayList<>();
        if (item.getAmount() > 1)
            lore.add(MessageUtil.colorize("&7Quantité : &e×" + item.getAmount()));
        lore.add(MessageUtil.colorize("&7Prix : &a" + MessageUtil.formatNumber(price) + " coins"));
        lore.add(MessageUtil.colorize("&7Taxes : &c" + String.format("%.0f", totalFee) + "%"));
        lore.add(MessageUtil.colorize("&7Vous recevez : &a" + MessageUtil.formatNumber(earnings) + " coins"));
        meta.setLore(lore);
        preview.setItemMeta(meta);
        return preview;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Click handling
    // ─────────────────────────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 15) {
            pubEnabled = !pubEnabled;
            player.openInventory(build());
            GuiManager.registerGui(player.getUniqueId(), this);
            return;
        }

        if (slot == 19) {
            handled = true;
            returnItem();
            MessageUtil.send(player, "&7Mise en vente annulée. L'objet vous a été rendu.");
            new HdvGui(plugin, player, 0).open();
            return;
        }

        if (slot == 11) {
            int used = plugin.getHdvManager().getActiveListingCount(player.getUniqueId());
            int max  = plugin.getHdvManager().getMaxSlots(player);
            if (used >= max) {
                MessageUtil.sendError(player, "Vos emplacements d'annonces sont pleins ! (" + used + "/" + max + ")");
                handled = true;
                returnItem();
                new HdvGui(plugin, player, 0).open();
                return;
            }

            handled = true;
            boolean ok = plugin.getHdvManager().addListing(player, item, price, pubEnabled);
            if (ok) {
                double totalFee = (HdvListing.TAX_RATE + (pubEnabled ? HdvListing.PUB_RATE : 0)) * 100;
                double earnings = price * (1 - totalFee / 100.0);
                MessageUtil.send(player,
                    "&a&lHDV &8» &7Objet mis en vente pour &e"
                    + MessageUtil.formatNumber(price) + " coins"
                    + " &8(vous recevrez &a" + MessageUtil.formatNumber(earnings) + " coins &8après taxes)&7 !");
                if (pubEnabled) {
                    MessageUtil.send(player, "&7Votre annonce a été &6broadcastée &7au chat !");
                }
            } else {
                MessageUtil.sendError(player, "Impossible de créer l'annonce. Réessayez.");
                returnItem();
            }
            new HdvGui(plugin, player, 0).open();
        }
    }

    public void returnItem() {
        if (handled) return;
        handled = true;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
