package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.hdv.HdvListing;
import fr.nations.hdv.HdvManager;
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

public class HdvGui {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int ITEM_START     = 9;
    private static final int ITEM_END       = 44;

    private final NationsPlugin plugin;
    private final Player player;
    private int page;

    public HdvGui(NationsPlugin plugin, Player player, int page) {
        this.plugin  = plugin;
        this.player  = player;
        this.page    = page;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Build
    // ─────────────────────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6✦ &eHôtel des Ventes &6✦", 6);

        // ── Row 1 : header ──
        ItemStack gold = GuiUtil.createFillerItem(Material.YELLOW_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inv.setItem(i, gold);

        List<HdvListing> all = plugin.getHdvManager().getActiveListings();
        int maxPage = maxPage(all);

        inv.setItem(0, GuiUtil.createItem(Material.CHEST,
            "&a&l✦ Tous les articles",
            "&7" + all.size() + " annonce(s) active(s)"
        ));
        inv.setItem(4, GuiUtil.createItem(Material.GOLD_BLOCK,
            "&6&l✦ Hôtel des Ventes ✦",
            "&7Achetez et vendez des objets",
            "&7entre joueurs !",
            "",
            "&eVotre balance : &a" + MessageUtil.formatNumber(
                plugin.getEconomyManager().getBalance(player.getUniqueId())) + " coins"
        ));
        inv.setItem(8, GuiUtil.createItem(Material.BOOK,
            "&e✦ Mes annonces",
            "&7Voir et gérer vos propres annonces"
        ));

        // ── Rows 2-5 : listings ──
        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < all.size(); i++) {
            inv.setItem(ITEM_START + i, buildListingItem(all.get(start + i)));
        }

        // ── Row 6 : navigation ──
        ItemStack navGlass = GuiUtil.createFillerItem(Material.YELLOW_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) inv.setItem(i, navGlass);

        if (page > 0) {
            inv.setItem(47, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
                "&a« Page précédente", "&7Page " + page + " / " + (maxPage + 1)));
        }
        inv.setItem(49, GuiUtil.createItem(Material.PAPER,
            "&7Page &e" + (page + 1) + " &7/ &e" + (maxPage + 1),
            "&7" + all.size() + " article(s) en vente"
        ));
        if (page < maxPage) {
            inv.setItem(51, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
                "&aPage suivante »", "&7Page " + (page + 2) + " / " + (maxPage + 1)));
        }
        inv.setItem(53, GuiUtil.createItem(Material.EMERALD,
            "&a&l+ Mettre en vente",
            "&7Ouvre votre inventaire pour",
            "&7sélectionner un objet à vendre.",
            "",
            "&7Vos annonces : &e"
                + plugin.getHdvManager().getActiveListingCount(player.getUniqueId())
                + " &7/ &e"
                + plugin.getHdvManager().getMaxSlots(player)
        ));

        GuiUtil.fillAll(inv);
        return inv;
    }

    private ItemStack buildListingItem(HdvListing listing) {
        ItemStack display = listing.getItem();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String name = meta.hasDisplayName()
            ? meta.getDisplayName()
            : "&f" + formatMat(listing.getItem().getType());

        meta.setDisplayName(MessageUtil.colorize("&e&l" + stripColor(name)));

        List<String> lore = new ArrayList<>();
        if (listing.getItem().getAmount() > 1)
            lore.add(MessageUtil.colorize("&7Quantité : &e×" + listing.getItem().getAmount()));
        lore.add(MessageUtil.colorize("&7Vendeur : &6" + listing.getSellerName()));
        lore.add(MessageUtil.colorize("&7Prix : &a" + MessageUtil.formatNumber(listing.getPrice()) + " coins"));
        lore.add(MessageUtil.colorize("&8" + HdvManager.timeAgo(listing.getListedAt())));
        lore.add("");

        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        if (listing.getSellerUuid().equals(player.getUniqueId())) {
            lore.add(MessageUtil.colorize("&7Ceci est votre propre annonce."));
            lore.add(MessageUtil.colorize("&7Allez dans &eMes annonces &7pour la retirer."));
        } else if (balance >= listing.getPrice()) {
            lore.add(MessageUtil.colorize("&eCliquez pour acheter !"));
        } else {
            double missing = listing.getPrice() - balance;
            lore.add(MessageUtil.colorize("&cFonds insuffisants (&e-" + MessageUtil.formatNumber(missing) + " coins&c)"));
        }

        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Click handling
    // ─────────────────────────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Header
        if (slot == 8) { new HdvMyListingsGui(plugin, player, 0).open(); return; }
        if (slot == 4 || slot == 0) return;

        // Navigation
        if (slot == 47 && page > 0) { page--; refresh(); return; }
        if (slot == 51) {
            List<HdvListing> all = plugin.getHdvManager().getActiveListings();
            if (page < maxPage(all)) { page++; refresh(); return; }
        }
        if (slot == 49) return;
        if (slot == 53) { new HdvSellGui(plugin, player).open(); return; }

        // Listing slots
        if (slot >= ITEM_START && slot <= ITEM_END) {
            int idx = page * ITEMS_PER_PAGE + (slot - ITEM_START);
            List<HdvListing> all = plugin.getHdvManager().getActiveListings();
            if (idx >= all.size()) return;
            HdvListing listing = all.get(idx);
            if (listing.getSellerUuid().equals(player.getUniqueId())) return;
            new HdvConfirmGui(plugin, player, listing, this).open();
        }
    }

    public void refresh() {
        Inventory fresh = build();
        Inventory cur = player.getOpenInventory().getTopInventory();
        for (int i = 0; i < fresh.getSize(); i++) cur.setItem(i, fresh.getItem(i));
        GuiManager.registerGui(player.getUniqueId(), this);
    }

    private int maxPage(List<HdvListing> all) {
        return all.isEmpty() ? 0 : (all.size() - 1) / ITEMS_PER_PAGE;
    }

    private static String formatMat(Material mat) {
        String raw = mat.name().replace("_", " ").toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (String w : raw.split(" "))
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private static String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
