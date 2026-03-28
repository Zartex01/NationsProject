package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class HdvSellGui {

    private final NationsPlugin plugin;
    private final Player player;

    public HdvSellGui(NationsPlugin plugin, Player player) {
        this.plugin  = plugin;
        this.player  = player;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Build — copies the player's inventory (0-35) into the GUI
    // ─────────────────────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&e✦ Sélectionner un objet à vendre ✦", 5);

        // ── Row 1 : header ──
        ItemStack lime = GuiUtil.createFillerItem(Material.LIME_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inv.setItem(i, lime);

        int used = plugin.getHdvManager().getActiveListingCount(player.getUniqueId());
        int max  = plugin.getHdvManager().getMaxSlots(player);

        inv.setItem(0, GuiUtil.createItem(Material.ARROW, "&eRetour au marché"));
        inv.setItem(4, GuiUtil.createItem(Material.GOLD_BLOCK,
            "&6Mettre un objet en vente",
            "&7Cliquez sur un objet de votre inventaire",
            "&7pour le mettre en vente sur l'HDV.",
            "",
            "&7Emplacements : &e" + used + " &7/ &e" + max
        ));

        if (used >= max) {
            inv.setItem(8, GuiUtil.createItem(Material.BARRIER,
                "&cEmplacements pleins !",
                "&7Retirez une annonce avant d'en créer une nouvelle."
            ));
            GuiUtil.fillAll(inv);
            return inv;
        }

        inv.setItem(8, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
            "&aCliquez sur un objet ci-dessous", "&7pour le sélectionner"));

        // ── Rows 2-5 : player inventory (slots 9-44 = inv slots 0-35) ──
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < 36 && i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            ItemStack display = item.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                String name = meta.hasDisplayName()
                    ? meta.getDisplayName()
                    : "&f" + formatMat(item.getType());
                meta.setDisplayName(MessageUtil.colorize(name));
                List<String> oldLore = meta.getLore() != null ? meta.getLore() : List.of();
                java.util.List<String> newLore = new java.util.ArrayList<>(oldLore);
                newLore.add(MessageUtil.colorize(""));
                newLore.add(MessageUtil.colorize("&eCliquez pour vendre cet objet !"));
                meta.setLore(newLore);
                display.setItemMeta(meta);
            }
            inv.setItem(9 + i, display);
        }

        GuiUtil.fillAll(inv);
        return inv;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Click handling
    // ─────────────────────────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 0) { new HdvGui(plugin, player, 0).open(); return; }
        if (slot < 9 || slot > 44) return;

        int invSlot = slot - 9;
        ItemStack item = player.getInventory().getItem(invSlot);
        if (item == null || item.getType().isAir()) return;

        int used = plugin.getHdvManager().getActiveListingCount(player.getUniqueId());
        int max  = plugin.getHdvManager().getMaxSlots(player);
        if (used >= max) {
            MessageUtil.sendError(player, "Vos emplacements d'annonces sont pleins ! (" + used + "/" + max + ")");
            return;
        }

        player.getInventory().setItem(invSlot, null);
        player.closeInventory();

        plugin.getHdvManager().setPendingPriceInput(player.getUniqueId(), item);

        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
            ? item.getItemMeta().getDisplayName()
            : formatMat(item.getType());

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, MessageUtil.colorize(
            "&8&m                                        "));
        MessageUtil.sendRaw(player, MessageUtil.colorize(
            "  &6&l✦ Hôtel des Ventes &8— &eMise en vente"));
        MessageUtil.sendRaw(player, MessageUtil.colorize(
            "  &7Objet : &e" + stripColor(name) + " &8×" + item.getAmount()));
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, MessageUtil.colorize(
            "  &aEntrez le prix de vente dans le chat :"));
        MessageUtil.sendRaw(player, MessageUtil.colorize(
            "  &8(ou tapez &cAnnuler &8pour annuler)"));
        MessageUtil.sendRaw(player, MessageUtil.colorize(
            "&8&m                                        "));
        MessageUtil.sendRaw(player, "");
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
