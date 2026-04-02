package fr.nations.kits;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
import fr.nations.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KitGui implements InventoryHolder {

    private final NationsPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    /** Slots des boutons de kit — rangée centrale (row 1, slots 9-17). */
    private static final int SLOT_JOUEUR  = 10;
    private static final int SLOT_SOUTIEN = 12;
    private static final int SLOT_PREMIUM = 14;
    private static final int SLOT_HEROS   = 16;

    public KitGui(NationsPlugin plugin, Player player) {
        this.plugin    = plugin;
        this.player    = player;
        this.inventory = Bukkit.createInventory(this, 27, "§8⚔ §lKits §8⚔");
        build();
    }

    public void open() {
        GuiManager.registerGui(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    // ─── Construction de l'interface ─────────────────────────────────────────

    private void build() {
        fillBorders();

        GradeType playerGrade = GradeType.fromPermission(player);
        KitManager km = plugin.getKitManager();

        inventory.setItem(SLOT_JOUEUR,  buildKitItem(KitType.JOUEUR,  playerGrade, km));
        inventory.setItem(SLOT_SOUTIEN, buildKitItem(KitType.SOUTIEN, playerGrade, km));
        inventory.setItem(SLOT_PREMIUM, buildKitItem(KitType.PREMIUM, playerGrade, km));
        inventory.setItem(SLOT_HEROS,   buildKitItem(KitType.HEROS,   playerGrade, km));
    }

    private ItemStack buildKitItem(KitType kit, GradeType playerGrade, KitManager km) {
        boolean hasAccess = kit.isAccessibleBy(playerGrade);
        long remaining    = km.remainingCooldown(player.getUniqueId(), kit);
        boolean available = hasAccess && remaining == 0;

        Material mat = hasAccess ? kit.getIcon() : Material.BARRIER;
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(kit.getColoredName());

        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━");

        // Grade requis
        lore.add("§7Grade requis : " + kit.getRequiredGrade().getColoredDisplay());

        // Items du kit
        lore.add("");
        lore.add("§7Contenu du kit :");
        for (ItemStack it : kit.getItems()) {
            lore.add("  §8• §f" + formatItem(it));
        }

        lore.add("");
        // Statut
        if (!hasAccess) {
            lore.add("§c✖ Grade insuffisant");
            lore.add("§7Tu dois être au moins " + kit.getRequiredGrade().getColoredDisplay() + "§7.");
        } else if (remaining > 0) {
            lore.add("§e⏳ En recharge : §f" + KitManager.formatDuration(remaining));
        } else {
            lore.add("§a✔ Disponible — cliquez pour claim !");
        }

        lore.add("§8━━━━━━━━━━━━━━━━━━");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorders() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta  = glass.getItemMeta();
        if (meta != null) { meta.setDisplayName("§r"); glass.setItemMeta(meta); }

        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, glass);
            }
        }
    }

    private static String formatItem(ItemStack item) {
        String name = item.getType().name().toLowerCase().replace('_', ' ');
        // Capitalise la première lettre
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return item.getAmount() > 1 ? name + " x" + item.getAmount() : name;
    }

    // ─── Gestion du clic ─────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        KitType kit = switch (slot) {
            case SLOT_JOUEUR  -> KitType.JOUEUR;
            case SLOT_SOUTIEN -> KitType.SOUTIEN;
            case SLOT_PREMIUM -> KitType.PREMIUM;
            case SLOT_HEROS   -> KitType.HEROS;
            default           -> null;
        };
        if (kit == null) return;

        GradeType playerGrade = GradeType.fromPermission(player);
        KitManager km = plugin.getKitManager();

        if (!kit.isAccessibleBy(playerGrade)) {
            player.sendMessage("§c✖ Tu dois être au moins "
                + kit.getRequiredGrade().getColoredDisplay()
                + " §cpour utiliser ce kit.");
            player.closeInventory();
            return;
        }

        long remaining = km.remainingCooldown(player.getUniqueId(), kit);
        if (remaining > 0) {
            player.sendMessage("§e⏳ Kit §r" + kit.getColoredName()
                + " §een recharge — §f" + KitManager.formatDuration(remaining) + " §erestantes.");
            player.closeInventory();
            return;
        }

        giveKit(kit);
    }

    private void giveKit(KitType kit) {
        List<ItemStack> items = kit.getItems();
        for (ItemStack item : items) {
            if (player.getInventory().firstEmpty() == -1) {
                // Inventaire plein : drop au sol
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            } else {
                player.getInventory().addItem(item);
            }
        }

        plugin.getKitManager().recordClaim(player.getUniqueId(), kit);
        player.closeInventory();

        player.sendMessage("");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("  §a§l✔ KIT RÉCLAMÉ — " + kit.getColoredName());
        player.sendMessage("  §7Tu as reçu le contenu du kit §r" + kit.getColoredName() + "§7.");
        player.sendMessage("  §7Prochain claim dans : §f"
            + KitManager.formatDuration(kit.getCooldownSeconds()));
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
    }
}
