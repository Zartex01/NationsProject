package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.shop.ShopCategory;
import fr.nations.shop.ShopItem;
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

public class ShopCategoryGui {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int ITEM_START     = 9;
    private static final int ITEM_END       = 44;

    private final NationsPlugin plugin;
    private final Player player;
    private final ShopCategory category;
    private int page;

    public ShopCategoryGui(NationsPlugin plugin, Player player, ShopCategory category, int page) {
        this.plugin    = plugin;
        this.player    = player;
        this.category  = category;
        this.page      = page;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Build
    // ─────────────────────────────────────────────────────────────────────────

    public Inventory build() {
        String title = category.getDisplayName() + " &8— &7p." + (page + 1);
        Inventory inv = GuiUtil.createGui(title, 6);

        // ── Row 1 : header ──
        ItemStack glass = GuiUtil.createFillerItem(category.getGlassColor());
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);

        inv.setItem(0, GuiUtil.createItem(Material.ARROW, "&eRetour au Shop"));
        inv.setItem(4, GuiUtil.createItem(category.getIcon(), category.getDisplayName(),
            "&7" + category.getItems().size() + " article(s) dans cette catégorie",
            "&aClic gauche &8→ &7Acheter",
            "&cClic droit &8→ &7Vendre x1",
            "&cMaj+Clic droit &8→ &7Vendre tout"
        ));
        inv.setItem(8, GuiUtil.createItem(Material.GOLD_NUGGET, "&eVotre Balance",
            "&7" + MessageUtil.formatNumber(plugin.getEconomyManager().getBalance(player.getUniqueId())) + " coins"
        ));

        // ── Rows 2-5 : items ──
        List<ShopItem> items = category.getItems();
        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < items.size(); i++) {
            inv.setItem(ITEM_START + i, buildShopItemDisplay(items.get(start + i)));
        }

        // ── Row 6 : navigation ──
        ItemStack navGlass = GuiUtil.createFillerItem(category.getGlassColor());
        for (int i = 45; i < 54; i++) inv.setItem(i, navGlass);

        inv.setItem(45, GuiUtil.createItem(Material.ARROW, "&eRetour au Shop"));

        int maxPage = maxPage();
        if (page > 0) {
            inv.setItem(47, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
                "&a« Page précédente", "&7Page " + page + " / " + (maxPage + 1)));
        }
        inv.setItem(49, GuiUtil.createItem(Material.PAPER,
            "&7Page &e" + (page + 1) + " &7/ &e" + (maxPage + 1)));
        if (page < maxPage) {
            inv.setItem(51, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
                "&aPage suivante »", "&7Page " + (page + 2) + " / " + (maxPage + 1)));
        }

        // Sell All (Premium)
        if (player.hasPermission("nations.sell.all") || player.hasPermission("nations.admin")) {
            inv.setItem(53, GuiUtil.createItem(Material.HOPPER,
                "&6Vendre tout l'inventaire",
                "&7Vend tous les objets vendables",
                "&7de votre inventaire en un clic !",
                "",
                "&eCliquez pour tout vendre !"
            ));
        } else {
            inv.setItem(53, GuiUtil.createItem(Material.BARRIER,
                "&c/sell all",
                "&7Nécessite le grade &6Premium"));
        }

        GuiUtil.fillAll(inv);
        return inv;
    }

    private ItemStack buildShopItemDisplay(ShopItem shopItem) {
        ItemStack base = shopItem.getBaseItem();
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;

        // Keep enchanted book display name; for normal items set a friendly name
        if (!meta.hasDisplayName()) {
            meta.setDisplayName(MessageUtil.colorize("&f" + formatMaterial(shopItem.getMaterial())));
        }

        List<String> lore = new ArrayList<>();
        if (shopItem.getBuyAmount() > 1) {
            lore.add(MessageUtil.colorize("&7Quantité par achat : &e×" + shopItem.getBuyAmount()));
        }
        lore.add("");

        if (shopItem.isBuyable()) {
            lore.add(MessageUtil.colorize("&a▶ ACHETER"));
            lore.add(MessageUtil.colorize("  &7Clic gauche &8→ &ax" + shopItem.getBuyAmount()
                + " pour &e" + MessageUtil.formatNumber(shopItem.getBuyPrice()) + " coins"));
            int qty64 = 64 / shopItem.getBuyAmount();
            double cost64 = qty64 * shopItem.getBuyPrice();
            lore.add(MessageUtil.colorize("  &7Maj+Clic gauche &8→ &ax" + (qty64 * shopItem.getBuyAmount())
                + " pour &e" + MessageUtil.formatNumber(cost64) + " coins"));
        } else {
            lore.add(MessageUtil.colorize("&8✘ Non disponible à l'achat"));
        }

        lore.add("");

        if (shopItem.isSellable()) {
            int inInv = countInInventory(shopItem.getMaterial());
            double totalValue = inInv * shopItem.getSellPrice();
            lore.add(MessageUtil.colorize("&c◀ VENDRE"));
            lore.add(MessageUtil.colorize("  &7Clic droit &8→ &cx1 pour &e"
                + MessageUtil.formatNumber(shopItem.getSellPrice()) + " coins"));
            lore.add(MessageUtil.colorize("  &7Maj+Clic droit &8→ &cVendre tout"));
            lore.add(MessageUtil.colorize("  &8(Inventaire : &7×" + inInv
                + " &8= &e" + MessageUtil.formatNumber(totalValue) + " coins&8)"));
        } else {
            lore.add(MessageUtil.colorize("&8✘ Non rachetable"));
        }

        meta.setLore(lore);
        base.setItemMeta(meta);
        return base;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Click handling
    // ─────────────────────────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        // Header clicks
        if (slot == 0 || slot == 45) { new ShopGui(plugin, player).open(); return; }
        if (slot == 8) return;

        // Nav row
        if (slot == 47 && page > 0) { page--; refresh(); return; }
        if (slot == 51 && page < maxPage()) { page++; refresh(); return; }
        if (slot == 49) return;
        if (slot == 53) { handleSellAll(); return; }

        // Item slots
        if (slot >= ITEM_START && slot <= ITEM_END) {
            int itemIndex = page * ITEMS_PER_PAGE + (slot - ITEM_START);
            List<ShopItem> items = category.getItems();
            if (itemIndex >= items.size()) return;
            ShopItem shopItem = items.get(itemIndex);

            if (click == ClickType.LEFT) {
                handleBuy(shopItem, 1);
            } else if (click == ClickType.SHIFT_LEFT) {
                int qty = 64 / shopItem.getBuyAmount();
                handleBuy(shopItem, Math.max(1, qty));
            } else if (click == ClickType.RIGHT) {
                handleSell(shopItem, 1);
            } else if (click == ClickType.SHIFT_RIGHT) {
                int inInv = countInInventory(shopItem.getMaterial());
                if (inInv > 0) handleSell(shopItem, inInv);
                else MessageUtil.sendError(player, "Vous n'avez pas de " + formatMaterial(shopItem.getMaterial()) + " dans votre inventaire.");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Buy / Sell logic
    // ─────────────────────────────────────────────────────────────────────────

    private void handleBuy(ShopItem shopItem, int purchases) {
        if (!shopItem.isBuyable()) {
            MessageUtil.sendError(player, "Cet article n'est pas disponible à l'achat.");
            return;
        }
        double total = shopItem.getBuyPrice() * purchases;
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        if (balance < total) {
            MessageUtil.sendError(player,
                "Fonds insuffisants ! Vous avez &e" + MessageUtil.formatNumber(balance)
                + " coins &c(&e" + MessageUtil.formatNumber(total) + " coins &cnécessaires).");
            return;
        }
        int totalItems = purchases * shopItem.getBuyAmount();
        ItemStack toGive = shopItem.createStack(totalItems);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
        if (!leftover.isEmpty()) {
            MessageUtil.sendError(player, "Inventaire plein ! Libérez de la place et réessayez.");
            return;
        }
        plugin.getEconomyManager().withdraw(player.getUniqueId(), total);
        MessageUtil.send(player,
            "&aAchat réussi : &e×" + totalItems + " " + formatMaterial(shopItem.getMaterial())
            + " &apour &e" + MessageUtil.formatNumber(total) + " coins&a !");
        refresh();
    }

    private void handleSell(ShopItem shopItem, int amount) {
        if (!shopItem.isSellable()) {
            MessageUtil.sendError(player, "Cet article n'est pas rachetable.");
            return;
        }
        int inInv = countInInventory(shopItem.getMaterial());
        int toSell = Math.min(amount, inInv);
        if (toSell <= 0) {
            MessageUtil.sendError(player, "Vous n'avez aucun &e" + formatMaterial(shopItem.getMaterial()) + " &cà vendre.");
            return;
        }
        removeFromInventory(shopItem.getMaterial(), toSell);
        double earned = shopItem.getSellPrice() * toSell;
        plugin.getEconomyManager().deposit(player.getUniqueId(), earned);
        MessageUtil.send(player,
            "&aVente réussie : &e×" + toSell + " " + formatMaterial(shopItem.getMaterial())
            + " &apour &e" + MessageUtil.formatNumber(earned) + " coins&a !");
        refresh();
    }

    private void handleSellAll() {
        if (!player.hasPermission("nations.sell.all") && !player.hasPermission("nations.admin")) {
            MessageUtil.sendError(player, "Nécessite le grade &6Premium&c.");
            return;
        }
        double total = 0;
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;
            double price = plugin.getShopManager().getSellPrice(item.getType());
            if (price <= 0) continue;
            total += price * item.getAmount();
            count += item.getAmount();
            contents[i] = null;
        }
        if (count == 0) {
            MessageUtil.send(player, "&7Aucun objet vendable dans votre inventaire.");
            return;
        }
        player.getInventory().setContents(contents);
        plugin.getEconomyManager().deposit(player.getUniqueId(), total);
        MessageUtil.send(player,
            "&aVendu &e" + count + " &aobjet(s) pour &e" + MessageUtil.formatNumber(total) + " coins &a!");
        refresh();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void refresh() {
        Inventory fresh = build();
        Inventory current = player.getOpenInventory().getTopInventory();
        for (int i = 0; i < fresh.getSize(); i++) current.setItem(i, fresh.getItem(i));
    }

    private int maxPage() {
        return Math.max(0, (category.getItems().size() - 1) / ITEMS_PER_PAGE);
    }

    private int countInInventory(Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count;
    }

    private void removeFromInventory(Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                contents[i] = null;
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
        player.getInventory().setContents(contents);
    }

    private static String formatMaterial(Material mat) {
        String raw = mat.name().replace("_", " ").toLowerCase();
        String[] words = raw.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
