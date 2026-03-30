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
import java.util.Comparator;
import java.util.List;

public class HdvGui {

    // ─────────────────────────────────────────────────────────────────────────
    //  Sort modes
    // ─────────────────────────────────────────────────────────────────────────

    public enum SortMode {
        RECENT,     // plus récent en premier (défaut)
        PRICE_DESC, // plus cher en premier
        PRICE_ASC,  // moins cher en premier
        ALPHA       // ordre alphabétique A-Z
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Constants
    // ─────────────────────────────────────────────────────────────────────────

    private static final int ITEMS_PER_PAGE = 36;
    private static final int ITEM_START     = 9;
    private static final int ITEM_END       = 44;

    // Header slots
    private static final int SLOT_ALL       = 0;
    private static final int SLOT_SORT_RECENT = 1;
    private static final int SLOT_SORT_PRICE  = 3;
    private static final int SLOT_TITLE      = 4;
    private static final int SLOT_SORT_ALPHA  = 5;
    private static final int SLOT_MY_LISTINGS = 8;

    // Nav slots
    private static final int SLOT_PREV     = 47;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int SLOT_NEXT     = 51;
    private static final int SLOT_SELL     = 53;

    // ─────────────────────────────────────────────────────────────────────────
    //  Fields
    // ─────────────────────────────────────────────────────────────────────────

    private final NationsPlugin plugin;
    private final Player player;
    private int page;
    private SortMode sortMode;

    public HdvGui(NationsPlugin plugin, Player player, int page) {
        this(plugin, player, page, SortMode.RECENT);
    }

    public HdvGui(NationsPlugin plugin, Player player, int page, SortMode sortMode) {
        this.plugin    = plugin;
        this.player    = player;
        this.page      = page;
        this.sortMode  = sortMode;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Build
    // ─────────────────────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6✦ &eHôtel des Ventes &8— &7" + sortLabel(), 6);

        // ── Row 1 : header ──
        ItemStack gold = GuiUtil.createFillerItem(Material.YELLOW_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inv.setItem(i, gold);

        List<HdvListing> all = sortedListings();
        int maxPage = maxPage(all);

        // Slot 0 – total annonces
        inv.setItem(SLOT_ALL, GuiUtil.createItem(Material.CHEST,
            "&a&l✦ Tous les articles",
            "&7" + all.size() + " annonce(s) active(s)"
        ));

        // Slot 1 – tri récent
        inv.setItem(SLOT_SORT_RECENT, buildSortButton(
            SortMode.RECENT,
            Material.CLOCK,
            Material.YELLOW_STAINED_GLASS_PANE,
            "&e⏱ Tri : Plus récent",
            new String[]{"&7Affiche les dernières annonces", "&7en premier (ordre d'arrivée)."},
            new String[]{"&7⬤ Tri actif"}
        ));

        // Slot 3 – tri prix
        inv.setItem(SLOT_SORT_PRICE, buildPriceSortButton());

        // Slot 4 – titre HDV
        inv.setItem(SLOT_TITLE, GuiUtil.createItem(Material.GOLD_BLOCK,
            "&6&l✦ Hôtel des Ventes ✦",
            "&7Achetez et vendez des objets",
            "&7entre joueurs !",
            "",
            "&eVotre balance : &a" + MessageUtil.formatNumber(
                plugin.getEconomyManager().getBalance(player.getUniqueId())) + " coins"
        ));

        // Slot 5 – tri alphabétique
        inv.setItem(SLOT_SORT_ALPHA, buildSortButton(
            SortMode.ALPHA,
            Material.NAME_TAG,
            Material.YELLOW_STAINED_GLASS_PANE,
            "&bA-Z &7Tri alphabétique",
            new String[]{"&7Trie les objets dans l'ordre", "&7alphabétique (A → Z)."},
            new String[]{"&7⬤ Tri actif"}
        ));

        // Slot 8 – mes annonces
        inv.setItem(SLOT_MY_LISTINGS, GuiUtil.createItem(Material.BOOK,
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
            inv.setItem(SLOT_PREV, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
                "&a« Page précédente", "&7Page " + page + " / " + (maxPage + 1)));
        }
        inv.setItem(SLOT_PAGE_INFO, GuiUtil.createItem(Material.PAPER,
            "&7Page &e" + (page + 1) + " &7/ &e" + (maxPage + 1),
            "&7" + all.size() + " article(s) en vente",
            "&7Tri : &e" + sortLabel()
        ));
        if (page < maxPage) {
            inv.setItem(SLOT_NEXT, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
                "&aPage suivante »", "&7Page " + (page + 2) + " / " + (maxPage + 1)));
        }
        inv.setItem(SLOT_SELL, GuiUtil.createItem(Material.EMERALD,
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

    // ─────────────────────────────────────────────────────────────────────────
    //  Sort helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<HdvListing> sortedListings() {
        List<HdvListing> list = new ArrayList<>(plugin.getHdvManager().getActiveListings());
        switch (sortMode) {
            case PRICE_DESC -> list.sort(Comparator.comparingDouble(HdvListing::getPrice).reversed());
            case PRICE_ASC  -> list.sort(Comparator.comparingDouble(HdvListing::getPrice));
            case ALPHA      -> list.sort(Comparator.comparing(l -> itemDisplayName(l).toLowerCase()));
            case RECENT     -> {} // already sorted by recency in HdvManager
        }
        return list;
    }

    private String itemDisplayName(HdvListing listing) {
        ItemStack item = listing.getItem();
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName().replaceAll("§[0-9a-fk-or]", "");
        }
        return formatMat(item.getType());
    }

    private String sortLabel() {
        return switch (sortMode) {
            case RECENT     -> "Plus récent";
            case PRICE_DESC -> "Prix ↓ (+ cher)";
            case PRICE_ASC  -> "Prix ↑ (- cher)";
            case ALPHA      -> "A-Z";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Button builders
    // ─────────────────────────────────────────────────────────────────────────

    private ItemStack buildSortButton(SortMode mode, Material activeMat, Material inactiveMat,
                                      String name, String[] inactiveLore, String[] activeLore) {
        boolean active = sortMode == mode;
        String displayName = (active ? "&a" : "&7") + name.replaceFirst("^&[0-9a-fk-or]", "");
        List<String> lore = new ArrayList<>();
        String[] extra = active ? activeLore : inactiveLore;
        for (String l : extra) lore.add(l);
        if (!active) lore.add("&eCliquez pour activer");
        else lore.add("&eCliquez pour désactiver");
        return GuiUtil.createItem(active ? activeMat : inactiveMat, displayName, lore.toArray(new String[0]));
    }

    private ItemStack buildPriceSortButton() {
        boolean ascending  = sortMode == SortMode.PRICE_ASC;
        boolean descending = sortMode == SortMode.PRICE_DESC;
        boolean active     = ascending || descending;

        Material mat = descending ? Material.ORANGE_DYE : ascending ? Material.LIME_DYE : Material.YELLOW_STAINED_GLASS_PANE;
        String name  = active
            ? (descending ? "&6💰 Prix ↓ (plus cher)" : "&a💰 Prix ↑ (moins cher)")
            : "&7💰 Trier par prix";

        List<String> lore = new ArrayList<>();
        if (!active) {
            lore.add("&7Cliquez une fois : &6Prix ↓ (plus cher)");
            lore.add("&7Cliquez deux fois : &aPrix ↑ (moins cher)");
            lore.add("&7Cliquez trois fois : désactiver");
            lore.add("");
            lore.add("&eCliquez pour activer");
        } else if (descending) {
            lore.add("&7Actuellement : &6Plus cher en premier");
            lore.add("");
            lore.add("&eCliquez → &aPrix ↑ (moins cher)");
        } else {
            lore.add("&7Actuellement : &aMoins cher en premier");
            lore.add("");
            lore.add("&eCliquez → &7désactiver le tri");
        }
        return GuiUtil.createItem(mat, name, lore.toArray(new String[0]));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Listing item builder
    // ─────────────────────────────────────────────────────────────────────────

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

        // ── Header fixed buttons ──
        if (slot == SLOT_MY_LISTINGS) { new HdvMyListingsGui(plugin, player, 0).open(); return; }
        if (slot == SLOT_TITLE || slot == SLOT_ALL) return;

        // ── Sort buttons ──
        if (slot == SLOT_SORT_RECENT) {
            sortMode = SortMode.RECENT;
            page = 0;
            refresh();
            return;
        }

        if (slot == SLOT_SORT_PRICE) {
            sortMode = switch (sortMode) {
                case PRICE_DESC -> SortMode.PRICE_ASC;
                case PRICE_ASC  -> SortMode.RECENT;
                default         -> SortMode.PRICE_DESC;
            };
            page = 0;
            refresh();
            return;
        }

        if (slot == SLOT_SORT_ALPHA) {
            sortMode = (sortMode == SortMode.ALPHA) ? SortMode.RECENT : SortMode.ALPHA;
            page = 0;
            refresh();
            return;
        }

        // ── Navigation ──
        if (slot == SLOT_PREV && page > 0) { page--; refresh(); return; }
        if (slot == SLOT_NEXT) {
            List<HdvListing> all = sortedListings();
            if (page < maxPage(all)) { page++; refresh(); return; }
        }
        if (slot == SLOT_PAGE_INFO) return;
        if (slot == SLOT_SELL) { new HdvSellGui(plugin, player).open(); return; }

        // ── Listing items ──
        if (slot >= ITEM_START && slot <= ITEM_END) {
            int idx = page * ITEMS_PER_PAGE + (slot - ITEM_START);
            List<HdvListing> all = sortedListings();
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

    public SortMode getSortMode() { return sortMode; }
}
