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

public class HdvConfirmGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final HdvListing listing;
    private final HdvGui parentGui;

    public HdvConfirmGui(NationsPlugin plugin, Player player, HdvListing listing, HdvGui parentGui) {
        this.plugin     = plugin;
        this.player     = player;
        this.listing    = listing;
        this.parentGui  = parentGui;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Build
    // ─────────────────────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6Confirmer l'achat ?", 3);

        ItemStack grayFill = GuiUtil.createFillerItem(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, grayFill);

        // ── Confirm button ──
        inv.setItem(11, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
            "&a&lCONFIRMER L'ACHAT",
            "&7Prix : &a" + MessageUtil.formatNumber(listing.getPrice()) + " coins",
            "&7Votre balance après : &e" + MessageUtil.formatNumber(
                plugin.getEconomyManager().getBalance(player.getUniqueId()) - listing.getPrice()) + " coins"
        ));

        // ── Item preview ──
        inv.setItem(13, buildPreview());

        // ── Cancel button ──
        inv.setItem(15, GuiUtil.createItem(Material.RED_STAINED_GLASS_PANE,
            "&c&lANNULER",
            "&7Retourner au marché"
        ));

        return inv;
    }

    private ItemStack buildPreview() {
        ItemStack preview = listing.getItem();
        ItemMeta meta = preview.getItemMeta();
        if (meta == null) return preview;

        List<String> lore = new ArrayList<>();
        if (listing.getItem().getAmount() > 1)
            lore.add(MessageUtil.colorize("&7Quantité : &e×" + listing.getItem().getAmount()));
        lore.add(MessageUtil.colorize("&7Vendeur : &6" + listing.getSellerName()));
        lore.add(MessageUtil.colorize("&7Prix : &a" + MessageUtil.formatNumber(listing.getPrice()) + " coins"));
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
            parentGui.open();
            return;
        }

        if (slot == 11) {
            boolean success = plugin.getHdvManager().buyListing(player, listing.getId());
            if (success) {
                MessageUtil.send(player,
                    "&aAchat réussi ! &7Vous avez acheté &e"
                    + formatMat(listing.getItem().getType())
                    + " &7pour &e" + MessageUtil.formatNumber(listing.getPrice()) + " coins&7 !");
            } else {
                if (plugin.getHdvManager().getListing(listing.getId()) == null) {
                    MessageUtil.sendError(player, "Cette annonce n'est plus disponible.");
                } else {
                    double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
                    if (balance < listing.getPrice()) {
                        MessageUtil.sendError(player, "Fonds insuffisants ! Il vous manque &e"
                            + MessageUtil.formatNumber(listing.getPrice() - balance) + " coins&c.");
                    } else {
                        MessageUtil.sendError(player, "Inventaire plein ! Faites de la place et réessayez.");
                    }
                }
            }
            parentGui.open();
        }
    }

    private static String formatMat(Material mat) {
        String raw = mat.name().replace("_", " ").toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (String w : raw.split(" "))
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
