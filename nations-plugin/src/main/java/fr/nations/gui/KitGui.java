package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
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

public class KitGui {

    private final NationsPlugin plugin;
    private final Player player;

    public KitGui(NationsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6✦ &eKits des Grades &6✦", 4);
        GuiUtil.fillBorder(inv);

        GradeType playerGrade = plugin.getGradeManager().getEffectiveGrade(player);

        inv.setItem(11, buildKitItem(GradeType.HEROS,     playerGrade));
        inv.setItem(13, buildKitItem(GradeType.CHEVALIER, playerGrade));
        inv.setItem(15, buildKitItem(GradeType.PREMIUM,   playerGrade));

        inv.setItem(31, GuiUtil.createItem(Material.BARRIER, "&cFermer"));

        GuiUtil.fillAll(inv);
        return inv;
    }

    private ItemStack buildKitItem(GradeType kit, GradeType playerGrade) {
        boolean hasAccess = playerGrade.ordinal() >= kit.ordinal();
        boolean onCooldown = hasAccess && plugin.getKitManager().isOnCooldown(player, kit);

        Material material;
        String displayName;
        List<String> lore = new ArrayList<>();

        switch (kit) {
            case HEROS -> {
                material = hasAccess ? Material.IRON_HELMET : Material.BARRIER;
                displayName = kit.getColoredDisplay() + " &7— Kit " + kit.getDisplayName();
                lore.add("&7Grade requis : " + GradeType.HEROS.getColoredDisplay());
                lore.add("");
                lore.add("&eContenu :");
                lore.add("&7• Armure fer &bProtection II Unbreaking II");
                lore.add("&7• Épée diamant &bSharpness II Unbreaking II");
                lore.add("&7• Arc &bPower I Unbreaking I");
                lore.add("&7• 64 Flèches · 32 Pains · 16 Steaks");
                lore.add("&7• 8 Pommes dorées · 16 Lingots de fer");
            }
            case CHEVALIER -> {
                material = hasAccess ? Material.IRON_CHESTPLATE : Material.BARRIER;
                displayName = kit.getColoredDisplay() + " &7— Kit " + kit.getDisplayName();
                lore.add("&7Grade requis : " + GradeType.CHEVALIER.getColoredDisplay());
                lore.add("");
                lore.add("&eContenu :");
                lore.add("&7• Armure fer &bProtection III Unbreaking III");
                lore.add("&7• Épée diamant &bSharpness III Fire Aspect I");
                lore.add("&7• Arc &bPower II Punch I Unbreaking II");
                lore.add("&7• 64 Flèches · 64 Pains · 32 Steaks");
                lore.add("&7• 16 Pommes dorées · 32 Lingots de fer · 4 Diamants");
            }
            case PREMIUM -> {
                material = hasAccess ? Material.DIAMOND_CHESTPLATE : Material.BARRIER;
                displayName = kit.getColoredDisplay() + " &7— Kit " + kit.getDisplayName();
                lore.add("&7Grade requis : " + GradeType.PREMIUM.getColoredDisplay());
                lore.add("");
                lore.add("&eContenu :");
                lore.add("&7• Armure diamant &bProtection III Unbreaking III");
                lore.add("&7• Épée diamant &bSharpness IV Looting II Fire Aspect I");
                lore.add("&7• Arc &bPower III Infinity Punch II");
                lore.add("&7• 64 Steaks · 32 Pommes dorées");
                lore.add("&7• 4 Notch Apples · 4 Perles de l'Ender · 8 Diamants");
            }
            default -> {
                material = Material.BARRIER;
                displayName = "&cKit inconnu";
            }
        }

        lore.add("");

        if (!hasAccess) {
            lore.add("&c✘ Grade insuffisant !");
            lore.add("&7Obtenez le grade " + kit.getColoredDisplay() + " &7pour accéder.");
        } else if (onCooldown) {
            long secs = plugin.getKitManager().getRemainingCooldownSeconds(player, kit);
            lore.add("&c⏳ En recharge !");
            lore.add("&7Temps de jeu restant : &e" + plugin.getKitManager().formatCooldown(secs));
        } else {
            lore.add("&a✔ Disponible !");
            lore.add("&eCliquez pour récupérer votre kit !");
        }

        lore.add("");
        lore.add("&7Recharge : &e3h de temps de jeu");

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(displayName));
            meta.setLore(lore.stream().map(MessageUtil::colorize).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        GradeType kit = switch (slot) {
            case 11 -> GradeType.HEROS;
            case 13 -> GradeType.CHEVALIER;
            case 15 -> GradeType.PREMIUM;
            default -> null;
        };

        if (slot == 31) {
            player.closeInventory();
            return;
        }

        if (kit == null) return;

        GradeType playerGrade = plugin.getGradeManager().getEffectiveGrade(player);

        if (playerGrade.ordinal() < kit.ordinal()) {
            MessageUtil.sendError(player, "Ce kit nécessite le grade " + kit.getColoredDisplay() + " &cou supérieur.");
            return;
        }

        if (plugin.getKitManager().isOnCooldown(player, kit)) {
            long remaining = plugin.getKitManager().getRemainingCooldownSeconds(player, kit);
            MessageUtil.sendError(player, "Kit en recharge ! Disponible dans : &e" + plugin.getKitManager().formatCooldown(remaining));
            return;
        }

        player.closeInventory();
        plugin.getKitManager().giveKit(player, kit);
        plugin.getKitManager().setCooldown(player, kit);
        MessageUtil.send(player, "&aKit &6" + kit.getDisplayName() + " &areçu ! &7(Recharge : 24h)");
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
