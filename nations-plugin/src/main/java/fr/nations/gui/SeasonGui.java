package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class SeasonGui {

    private final NationsPlugin plugin;
    private final Player player;

    public SeasonGui(NationsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6&lClassement Saison " + plugin.getSeasonManager().getCurrentSeason(), 5);
        GuiUtil.fillBorder(inv);

        inv.setItem(4, GuiUtil.createItem(Material.CLOCK,
            "&eSaison " + plugin.getSeasonManager().getCurrentSeason(),
            "&7Temps restant: &f" + plugin.getSeasonManager().getFormattedSeasonTimeRemaining(),
            "",
            "&7Récompenses:",
            "&6🥇 1er: &e" + MessageUtil.formatNumber(plugin.getConfigManager().getSeasonFirstPlaceReward()) + " coins",
            "&7🥈 2ème: &e" + MessageUtil.formatNumber(plugin.getConfigManager().getSeasonSecondPlaceReward()) + " coins",
            "&c🥉 3ème: &e" + MessageUtil.formatNumber(plugin.getConfigManager().getSeasonThirdPlaceReward()) + " coins"
        ));

        List<Nation> sorted = plugin.getNationManager().getNationsSortedByPoints();
        int[] slots = {11, 13, 15, 19, 21, 23, 25, 28, 30, 32};
        Material[] medals = {
            Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK,
            Material.STONE, Material.STONE, Material.STONE, Material.STONE,
            Material.STONE, Material.STONE, Material.STONE
        };

        for (int i = 0; i < Math.min(sorted.size(), slots.length); i++) {
            Nation n = sorted.get(i);
            int claims = plugin.getTerritoryManager().getClaimCountForNation(n.getId());
            inv.setItem(slots[i], GuiUtil.createItem(medals[i],
                "&e#" + (i + 1) + " " + n.getName(),
                "&7Points: &6" + n.getSeasonPoints(),
                "&7Membres: &f" + n.getMemberCount(),
                "&7Claims: &f" + claims
            ));
        }

        inv.setItem(40, GuiUtil.createItem(Material.BARRIER, "&cFermer"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == 40) {
            player.closeInventory();
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
