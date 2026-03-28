package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.hdv.HdvListing;
import fr.nations.hdv.HdvManager;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HdvMyListingsGui {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int ITEM_START     = 9;
    private static final int ITEM_END       = 44;

    private final NationsPlugin plugin;
    private final Player player;
    private int page;

    public HdvMyListingsGui(NationsPlugin plugin, Player player, int page) {
        this.plugin  = plugin;
        this.player  = player;
        this.page    = page;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Build
    // ─────────────────────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&e✦ Mes Annonces ✦", 6);

        // ── Row 1 : header ──
        ItemStack gold = GuiUtil.createFillerItem(Material.YELLOW_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inv.setItem(i, gold);

        List<HdvListing> mine = plugin.getHdvManager().getListingsBySeller(player.getUniqueId());
        int maxPage = mine.isEmpty() ? 0 : (mine.size() - 1) / ITEMS_PER_PAGE;
        int used    = plugin.getHdvManager().getActiveListingCount(player.getUniqueId());
        int max     = plugin.getHdvManager().getMaxSlots(player);

        inv.setItem(0, GuiUtil.createItem(Material.CHEST,
            "&7Tous les articles", "&7Retour au marché"));
        inv.setItem(4, GuiUtil.createItem(Material.BOOK,
            "&e&l✦ Mes Annonces",
            "&7Vos emplacements : &e" + used + " &7/ &e" + max,
            "",
            "&cClic droit &7sur une annonce pour la retirer"
        ));
        inv.setItem(8, GuiUtil.createItem(Material.EMERALD,
            "&a&l+ Mettre en vente",
            "&7Sélectionnez un objet à vendre"
        ));

        // ── Rows 2-5 : my listings ──
        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < mine.size(); i++) {
            inv.setItem(ITEM_START + i, buildMyListingItem(mine.get(start + i)));
        }

        if (mine.isEmpty()) {
            inv.setItem(22, GuiUtil.createItem(Material.BARRIER,
                "&7Aucune annonce active",
                "&7Cliquez sur &a+ Mettre en vente &7pour commencer !"
            ));
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
            "&7" + mine.size() + " annonce(s) active(s)"
        ));
        if (page < maxPage) {
            inv.setItem(51, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
                "&aPage suivante »", "&7Page " + (page + 2) + " / " + (maxPage + 1)));
        }
        inv.setItem(45, GuiUtil.createItem(Material.ARROW, "&eRetour au marché"));

        GuiUtil.fillAll(inv);
        return inv;
    }

    private ItemStack buildMyListingItem(HdvListing listing) {
        ItemStack display = listing.getItem();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        meta.setDisplayName(MessageUtil.colorize("&e&l" + stripColor(
            meta.hasDisplayName() ? meta.getDisplayName() : formatMat(listing.getItem().getType())
        )));

        List<String> lore = new ArrayList<>();
        if (listing.getItem().getAmount() > 1)
            lore.add(MessageUtil.colorize("&7Quantité : &e×" + listing.getItem().getAmount()));
        lore.add(MessageUtil.colorize("&7Prix de vente : &a" + MessageUtil.formatNumber(listing.getPrice()) + " coins"));
        lore.add(MessageUtil.colorize("&8Mis en vente " + HdvManager.timeAgo(listing.getListedAt())));
        lore.add("");
        lore.add(MessageUtil.colorize("&cClic droit &7pour retirer l'annonce"));
        lore.add(MessageUtil.colorize("&8(L'objet vous sera rendu)"));

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
        ClickType click = event.getClick();

        if (slot == 0 || slot == 45) { new HdvGui(plugin, player, 0).open(); return; }
        if (slot == 8)               { new HdvSellGui(plugin, player).open(); return; }
        if (slot == 4)               return;

        if (slot == 47 && page > 0)  { page--; refresh(); return; }
        if (slot == 51) {
            List<HdvListing> mine = plugin.getHdvManager().getListingsBySeller(player.getUniqueId());
            int maxP = mine.isEmpty() ? 0 : (mine.size() - 1) / ITEMS_PER_PAGE;
            if (page < maxP) { page++; refresh(); return; }
        }
        if (slot == 49) return;

        if (slot >= ITEM_START && slot <= ITEM_END) {
            if (click != ClickType.RIGHT && click != ClickType.SHIFT_RIGHT) return;
            int idx = page * ITEMS_PER_PAGE + (slot - ITEM_START);
            List<HdvListing> mine = plugin.getHdvManager().getListingsBySeller(player.getUniqueId());
            if (idx >= mine.size()) return;
            HdvListing listing = mine.get(idx);

            HdvListing removed = plugin.getHdvManager().removeListing(listing.getId());
            if (removed != null) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(removed.getItem());
                leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                MessageUtil.send(player, "&aAnnonce retirée ! &7L'objet vous a été rendu.");
                refresh();
            }
        }
    }

    public void refresh() {
        Inventory fresh = build();
        Inventory cur = player.getOpenInventory().getTopInventory();
        for (int i = 0; i < fresh.getSize(); i++) cur.setItem(i, fresh.getItem(i));
        GuiManager.registerGui(player.getUniqueId(), this);
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
