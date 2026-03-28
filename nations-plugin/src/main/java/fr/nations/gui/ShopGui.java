package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.shop.ShopCategory;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ShopGui {

    private final NationsPlugin plugin;
    private final Player player;

    private static final int[] CATEGORY_SLOTS = {19, 21, 23, 25, 28, 30, 32, 34};

    public ShopGui(NationsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6✦ &eBoutique du Serveur &6✦", 5);

        // ── Row 1 : header dorée ──
        ItemStack gold = GuiUtil.createFillerItem(Material.YELLOW_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inv.setItem(i, gold);

        // Centre row 1 : titre déco
        inv.setItem(4, GuiUtil.createItem(Material.CHEST,
            "&6&l✦ Boutique Nations ✦",
            "&7Bienvenue ! Cliquez sur une catégorie",
            "&7pour acheter et vendre des objets.",
            "",
            "&aVotre balance : &e" + MessageUtil.formatNumber(plugin.getEconomyManager().getBalance(player.getUniqueId())) + " coins"
        ));

        // ── Row 2 : séparateur ──
        ItemStack gray = GuiUtil.createFillerItem();
        for (int i = 9; i < 18; i++) inv.setItem(i, gray);

        // ── Row 3 & 4 : catégories ──
        // Bordures
        inv.setItem(18, GuiUtil.createFillerItem()); inv.setItem(26, GuiUtil.createFillerItem());
        inv.setItem(20, GuiUtil.createFillerItem()); inv.setItem(22, GuiUtil.createFillerItem());
        inv.setItem(24, GuiUtil.createFillerItem());
        inv.setItem(27, GuiUtil.createFillerItem()); inv.setItem(35, GuiUtil.createFillerItem());
        inv.setItem(29, GuiUtil.createFillerItem()); inv.setItem(31, GuiUtil.createFillerItem());
        inv.setItem(33, GuiUtil.createFillerItem());

        List<ShopCategory> cats = plugin.getShopManager().getCategories();
        for (int i = 0; i < Math.min(cats.size(), CATEGORY_SLOTS.length); i++) {
            inv.setItem(CATEGORY_SLOTS[i], buildCategoryItem(cats.get(i)));
        }

        // ── Row 5 : pied de page ──
        ItemStack footer = GuiUtil.createFillerItem(Material.YELLOW_STAINED_GLASS_PANE);
        for (int i = 36; i < 45; i++) inv.setItem(i, footer);

        // Balance et fermer
        inv.setItem(40, GuiUtil.createItem(Material.GOLD_NUGGET,
            "&eVotre Balance",
            "&7" + MessageUtil.formatNumber(plugin.getEconomyManager().getBalance(player.getUniqueId())) + " coins"
        ));
        inv.setItem(44, GuiUtil.createItem(Material.BARRIER, "&cFermer"));

        GuiUtil.fillAll(inv);
        return inv;
    }

    private ItemStack buildCategoryItem(ShopCategory cat) {
        int buyCount  = (int) cat.getItems().stream().filter(i -> i.isBuyable()).count();
        int sellCount = (int) cat.getItems().stream().filter(i -> i.isSellable()).count();

        ItemStack item = new ItemStack(cat.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(cat.getDisplayName()));
            meta.setLore(List.of(
                MessageUtil.colorize("&7" + cat.getItems().size() + " objet(s) disponible(s)"),
                MessageUtil.colorize("&a▶ Achat : &e" + buyCount + " &aarticles"),
                MessageUtil.colorize("&c◀ Vente : &e" + sellCount + " &carticles"),
                "",
                MessageUtil.colorize("&eCliquez pour ouvrir !")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 44) { player.closeInventory(); return; }
        if (slot == 4 || slot == 40) return;

        List<ShopCategory> cats = plugin.getShopManager().getCategories();
        for (int i = 0; i < Math.min(cats.size(), CATEGORY_SLOTS.length); i++) {
            if (slot == CATEGORY_SLOTS[i]) {
                new ShopCategoryGui(plugin, player, cats.get(i), 0).open();
                return;
            }
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
