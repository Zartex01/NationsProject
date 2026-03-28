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
import java.util.UUID;

public class HdvBrowseGui {

    private final NationsPlugin plugin;
    private final Player player;
    private int page;
    private final List<HdvListing> listings;

    private static final int PAGE_SIZE = 28;

    public HdvBrowseGui(NationsPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.listings = plugin.getHdvManager().getAllListings();
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6&lHDV &8— &eMarchés (" + (page + 1) + ")", 6);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, listings.size());

        int[] slots = getContentSlots();

        for (int i = start; i < end; i++) {
            HdvListing listing = listings.get(i);
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta == null) meta = plugin.getServer().getItemFactory().getItemMeta(display.getType());

            List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
            lore.add(MessageUtil.colorize("&8&m-----------"));
            lore.add(MessageUtil.colorize("&7Vendeur: &f" + listing.getSellerName()));
            lore.add(MessageUtil.colorize("&7Prix: &e" + MessageUtil.formatNumber(listing.getPrice()) + " coins"));
            if (listing.isPubEnabled()) {
                lore.add(MessageUtil.colorize("&6[PUB]"));
            }
            lore.add(MessageUtil.colorize("&8&m-----------"));
            lore.add(MessageUtil.colorize("&eCliquez pour acheter"));
            meta.setLore(lore);
            display.setItemMeta(meta);

            int slotIndex = i - start;
            if (slotIndex < slots.length) {
                inv.setItem(slots[slotIndex], display);
            }
        }

        if (listings.isEmpty()) {
            inv.setItem(22, GuiUtil.createItem(Material.BARRIER,
                "&cAucune annonce",
                "&7Il n'y a rien en vente pour l'instant."
            ));
        }

        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, GuiUtil.createFillerItem(Material.GRAY_STAINED_GLASS_PANE));
        }
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, GuiUtil.createFillerItem(Material.GRAY_STAINED_GLASS_PANE));
        }

        if (page > 0) {
            inv.setItem(45, GuiUtil.createItem(Material.ARROW,
                "&7&l← Page précédente",
                "&7Page " + page + "/" + getTotalPages()
            ));
        }

        inv.setItem(49, GuiUtil.createItem(Material.COMPASS,
            "&6&lHDV",
            "&7Total annonces: &e" + listings.size(),
            "&7Page: &e" + (page + 1) + "/" + Math.max(1, getTotalPages())
        ));

        if ((page + 1) * PAGE_SIZE < listings.size()) {
            inv.setItem(53, GuiUtil.createItem(Material.ARROW,
                "&7&lPage suivante →",
                "&7Page " + (page + 2) + "/" + getTotalPages()
            ));
        }

        GuiUtil.fillAll(inv);
        return inv;
    }

    private int[] getContentSlots() {
        int[] slots = new int[PAGE_SIZE];
        int idx = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 0; col < 7; col++) {
                slots[idx++] = row * 9 + col + 1;
            }
        }
        return slots;
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) listings.size() / PAGE_SIZE);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45 && page > 0) {
            page--;
            player.openInventory(build());
            GuiManager.registerGui(player.getUniqueId(), this);
            return;
        }

        if (slot == 53 && (page + 1) * PAGE_SIZE < listings.size()) {
            page++;
            player.openInventory(build());
            GuiManager.registerGui(player.getUniqueId(), this);
            return;
        }

        int[] contentSlots = getContentSlots();
        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                int listingIndex = page * PAGE_SIZE + i;
                if (listingIndex < listings.size()) {
                    handleBuy(listings.get(listingIndex));
                }
                return;
            }
        }
    }

    private void handleBuy(HdvListing listing) {
        if (listing.getSellerUUID().equals(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous ne pouvez pas acheter votre propre annonce !");
            return;
        }

        double price = listing.getPrice();

        if (!plugin.getEconomyManager().has(player.getUniqueId(), price)) {
            MessageUtil.sendError(player, "Vous n'avez pas assez de coins ! (besoin: &e" +
                MessageUtil.formatNumber(price) + " coins&c)");
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            MessageUtil.sendError(player, "Votre inventaire est plein !");
            return;
        }

        plugin.getEconomyManager().withdraw(player.getUniqueId(), price);

        double earnings = listing.getSellerEarnings();
        plugin.getEconomyManager().deposit(listing.getSellerUUID(), earnings);

        plugin.getEconomyManager().saveAccountToDatabase(player.getUniqueId());
        plugin.getEconomyManager().saveAccountToDatabase(listing.getSellerUUID());

        player.getInventory().addItem(listing.getItem());

        plugin.getHdvManager().removeListing(listing.getId());

        String itemName = getItemName(listing.getItem());
        MessageUtil.sendSuccess(player, "Vous avez acheté &e" + itemName +
            " &apour &e" + MessageUtil.formatNumber(price) + " coins&a !");

        org.bukkit.OfflinePlayer seller = plugin.getServer().getOfflinePlayer(listing.getSellerUUID());
        if (seller.isOnline() && seller.getPlayer() != null) {
            MessageUtil.sendSuccess(seller.getPlayer(),
                "&f" + player.getName() + " &aa acheté votre &e" + itemName +
                " &apour &e" + MessageUtil.formatNumber(price) + " coins &a(vous recevez: &e" +
                MessageUtil.formatNumber(earnings) + " coins&a) !");
        }

        listings.remove(listing);
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }

    private String getItemName(ItemStack item) {
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().replace("_", " ");
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
