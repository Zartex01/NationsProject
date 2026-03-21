package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class ConfirmDisbandGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;

    public ConfirmDisbandGui(NationsPlugin plugin, Player player, Nation nation) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&c&lDissoudre " + nation.getName() + "?", 3);
        GuiUtil.fillBorder(inv);

        inv.setItem(11, GuiUtil.createItem(Material.LIME_TERRACOTTA,
            "&aConfirmer la dissolution",
            "&7Cliquez pour dissoudre définitivement",
            "&7la nation &c" + nation.getName()
        ));

        inv.setItem(13, GuiUtil.createItem(Material.BARRIER,
            "&7Annuler",
            "&7Retourner en sécurité"
        ));

        inv.setItem(15, GuiUtil.createItem(Material.RED_TERRACOTTA,
            "&cAnnuler",
            "&7Ne pas dissoudre la nation"
        ));

        GuiUtil.fillAll(inv);
        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 11) {
            String nationName = nation.getName();
            plugin.getNationManager().disbandNation(nation.getId());
            player.closeInventory();
            MessageUtil.sendSuccess(player, "La nation §6" + nationName + " §aa été dissoute.");
            Bukkit.broadcastMessage(MessageUtil.colorize(plugin.getConfigManager().getPrefix()
                + "&cLa nation &6" + nationName + " &ca été dissoute!"));
        } else if (slot == 13 || slot == 15) {
            new NationMainGui(plugin, player, nation).open();
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
