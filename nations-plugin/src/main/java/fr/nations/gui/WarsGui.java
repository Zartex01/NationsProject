package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import fr.nations.war.War;
import fr.nations.war.WarStatus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WarsGui {

    private final NationsPlugin plugin;
    private final Player player;
    private int page;
    private final List<War> warList;

    public WarsGui(NationsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.page = 0;
        Collection<War> allWars = plugin.getWarManager().getAllWars();
        this.warList = new ArrayList<>();
        for (War w : allWars) {
            if (!w.getStatus().isFinished()) warList.add(w);
        }
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&c&lGuerres", 5);
        GuiUtil.fillBorder(inv);

        int start = page * 21;
        int slot = 10;
        int count = 0;

        for (int i = start; i < warList.size() && count < 21; i++, count++) {
            War war = warList.get(i);
            Nation attacker = plugin.getNationManager().getNationById(war.getAttackerNationId());
            Nation defender = plugin.getNationManager().getNationById(war.getDefenderNationId());

            String attackerName = attacker != null ? attacker.getName() : "Inconnue";
            String defenderName = defender != null ? defender.getName() : "Inconnue";

            Material mat = war.getStatus() == WarStatus.PENDING_VALIDATION
                ? Material.ORANGE_WOOL
                : Material.RED_WOOL;

            inv.setItem(slot, GuiUtil.createItem(mat,
                "&c" + attackerName + " &7vs &c" + defenderName,
                "&7Type: &e" + war.getType().getDisplayName(),
                "&7Statut: &f" + war.getStatus().getDisplayName(),
                "&7Attaquant kills: &c" + war.getAttackerKills(),
                "&7Défenseur kills: &9" + war.getDefenderKills(),
                war.getStatus().isActive() ? "&7Temps restant: &e" + war.getFormattedTimeRemaining() : ""
            ));

            slot++;
            if ((slot % 9) == 8) slot += 2;
        }

        if (page > 0) {
            inv.setItem(39, GuiUtil.createItem(Material.ARROW, "&7← Précédent"));
        }
        if (start + 21 < warList.size()) {
            inv.setItem(41, GuiUtil.createItem(Material.ARROW, "&7Suivant →"));
        }

        inv.setItem(40, GuiUtil.createItem(Material.BARRIER, "&cFermer"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 39 && page > 0) {
            page--;
            player.openInventory(build());
        } else if (slot == 41 && (page + 1) * 21 < warList.size()) {
            page++;
            player.openInventory(build());
        } else if (slot == 40) {
            player.closeInventory();
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
