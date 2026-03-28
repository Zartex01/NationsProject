package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.hdv.HdvListing;
import fr.nations.hdv.HdvManager;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HdvSellGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final ItemStack itemToSell;
    private double price = 0;
    private boolean pubEnabled = false;

    public HdvSellGui(NationsPlugin plugin, Player player, ItemStack itemToSell) {
        this.plugin = plugin;
        this.player = player;
        this.itemToSell = itemToSell.clone();
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6&lHDV &8— &eVendre un item", 4);
        GuiUtil.fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        ItemStack preview = itemToSell.clone();
        ItemMeta previewMeta = preview.getItemMeta();
        if (previewMeta != null) {
            List<String> lore = previewMeta.getLore() != null ? previewMeta.getLore() : new ArrayList<>();
            lore.add(MessageUtil.colorize("&8&m-----------"));
            lore.add(MessageUtil.colorize("&7Item à vendre"));
            previewMeta.setLore(lore);
            preview.setItemMeta(previewMeta);
        }
        inv.setItem(13, preview);

        if (price <= 0) {
            inv.setItem(10, GuiUtil.createItem(Material.GOLD_INGOT,
                "&6Prix de vente",
                "&7Aucun prix défini",
                "",
                "&eCliquez pour définir le prix"
            ));
        } else {
            inv.setItem(10, GuiUtil.createItem(Material.GOLD_BLOCK,
                "&6Prix de vente",
                "&7Prix: &e" + MessageUtil.formatNumber(price) + " coins",
                "",
                "&eCliquez pour modifier"
            ));
        }

        if (pubEnabled) {
            inv.setItem(16, GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE,
                "&a&lPublicité: &2ON",
                "&7Votre item sera annoncé dans le tchat",
                "&7lors de sa mise en vente.",
                "",
                "&cCoût pub: &f+2% &7des gains",
                "&eCliquez pour désactiver"
            ));
        } else {
            inv.setItem(16, GuiUtil.createItem(Material.RED_STAINED_GLASS_PANE,
                "&c&lPublicité: &4OFF",
                "&7Pas de pub dans le tchat.",
                "",
                "&7Activer: &f+2% &7de frais supplémentaires",
                "&eCliquez pour activer"
            ));
        }

        double totalFee = price > 0 ? price * HdvManager.SELL_TAX + (pubEnabled ? price * HdvManager.PUB_FEE : 0) : 0;
        double earnings = price > 0 ? price - totalFee : 0;
        String feeText = pubEnabled ? "&c7% &7(5% taxe + 2% pub)" : "&c5% &7(taxe HDV)";

        inv.setItem(22, GuiUtil.createItem(Material.PAPER,
            "&7Récapitulatif",
            "&7Prix affiché: &e" + (price > 0 ? MessageUtil.formatNumber(price) : "non défini") + " coins",
            "&7Frais: " + feeText,
            "&7Vous recevrez: &a" + (price > 0 ? MessageUtil.formatNumber(earnings) : "—") + " coins"
        ));

        if (price > 0) {
            inv.setItem(28, GuiUtil.createItem(Material.BARRIER,
                "&cAnnuler"
            ));
            inv.setItem(34, GuiUtil.createItem(Material.LIME_CONCRETE,
                "&a&lConfirmer la vente",
                "&7Item: &f" + getItemName(itemToSell),
                "&7Prix: &e" + MessageUtil.formatNumber(price) + " coins",
                "&7Frais: " + feeText,
                "",
                "&eCliquez pour mettre en vente"
            ));
        } else {
            inv.setItem(28, GuiUtil.createItem(Material.BARRIER,
                "&cAnnuler"
            ));
            inv.setItem(34, GuiUtil.createItem(Material.GRAY_CONCRETE,
                "&7Confirmer la vente",
                "&cDéfinissez d'abord un prix !"
            ));
        }

        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 10) {
            GuiManager.setPendingGui(player.getUniqueId(), this);
            player.closeInventory();
            MessageUtil.send(player, "&7Entrez le &eprix de vente &7dans le chat: (ex: &e1000&7)");
            MessageUtil.send(player, "&7Tapez &ccancel &7pour annuler.");
            GuiManager.setPendingAction(player.getUniqueId(), "hdv_set_price:");
        } else if (slot == 16) {
            pubEnabled = !pubEnabled;
            player.openInventory(build());
            GuiManager.registerGui(player.getUniqueId(), this);
        } else if (slot == 28) {
            player.closeInventory();
        } else if (slot == 34) {
            if (price <= 0) {
                MessageUtil.sendError(player, "Vous devez définir un prix avant de mettre en vente !");
                return;
            }
            confirmListing();
        }
    }

    private void confirmListing() {
        if (player.getInventory().getItemInMainHand().getType() == Material.AIR ||
            !player.getInventory().getItemInMainHand().isSimilar(itemToSell) &&
            !containsItem(player, itemToSell)) {
            MessageUtil.sendError(player, "Vous n'avez plus l'item à vendre dans votre inventaire !");
            player.closeInventory();
            return;
        }

        removeOneItem(player, itemToSell);

        HdvListing listing = new HdvListing(
            UUID.randomUUID(),
            player.getUniqueId(),
            player.getName(),
            itemToSell,
            price,
            pubEnabled,
            System.currentTimeMillis()
        );

        plugin.getHdvManager().addListing(listing);

        double feePercent = listing.getTotalFeePercent();
        MessageUtil.sendSuccess(player, "Votre item est maintenant en vente pour &e" +
            MessageUtil.formatNumber(price) + " coins &a! (frais: &c" + (int) feePercent + "% &a)");

        if (pubEnabled) {
            String itemName = getItemName(itemToSell);
            String pubMessage = MessageUtil.colorize(
                "&6[&e&lHDV PUB&6] &f" + player.getName() + " &7vend &e" + itemName +
                " &7pour &6" + MessageUtil.formatNumber(price) + " coins &7— /hdv"
            );
            Bukkit.broadcastMessage(pubMessage);
        }

        player.closeInventory();
    }

    private boolean containsItem(Player p, ItemStack item) {
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(item)) return true;
        }
        return false;
    }

    private void removeOneItem(Player p, ItemStack item) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack != null && stack.isSimilar(item)) {
                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                } else {
                    p.getInventory().setItem(i, null);
                }
                return;
            }
        }
    }

    private String getItemName(ItemStack item) {
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().replace("_", " ");
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
