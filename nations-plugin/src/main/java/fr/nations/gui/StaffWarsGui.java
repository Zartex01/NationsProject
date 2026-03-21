package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import fr.nations.war.War;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class StaffWarsGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final List<War> pendingWars;

    public StaffWarsGui(NationsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.pendingWars = new ArrayList<>(plugin.getWarManager().getPendingWars());
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&c&lValidation des Guerres", 5);
        GuiUtil.fillBorder(inv);

        if (pendingWars.isEmpty()) {
            inv.setItem(22, GuiUtil.createItem(Material.LIME_WOOL,
                "&aAucune guerre en attente",
                "&7Toutes les guerres ont été traitées"
            ));
        } else {
            int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
            for (int i = 0; i < Math.min(pendingWars.size(), slots.length); i++) {
                War war = pendingWars.get(i);
                Nation attacker = plugin.getNationManager().getNationById(war.getAttackerNationId());
                Nation defender = plugin.getNationManager().getNationById(war.getDefenderNationId());
                String attackerName = attacker != null ? attacker.getName() : "Inconnue";
                String defenderName = defender != null ? defender.getName() : "Inconnue";

                inv.setItem(slots[i], GuiUtil.createItem(Material.ORANGE_WOOL,
                    "&6" + attackerName + " &7→ &c" + defenderName,
                    "&7Type: &e" + war.getType().getDisplayName(),
                    "&7Raison: &f" + (war.getReason().isEmpty() ? "Non spécifiée" : war.getReason()),
                    "&7Durée: &f" + war.getType().getDurationHours() + "h",
                    "",
                    "&aCliquer: Valider | &cShift-Cliquer: Rejeter",
                    "&8ID: " + war.getId().toString().substring(0, 8)
                ));
            }
        }

        inv.setItem(40, GuiUtil.createItem(Material.BARRIER, "&cFermer"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 40) {
            player.closeInventory();
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        for (int i = 0; i < slots.length && i < pendingWars.size(); i++) {
            if (slots[i] == slot) {
                War war = pendingWars.get(i);
                if (event.isShiftClick()) {
                    plugin.getWarManager().rejectWar(war.getId(), player.getUniqueId(), "Rejetée par le staff");
                    MessageUtil.sendError(player, "Guerre rejetée.");
                } else {
                    plugin.getWarManager().validateWar(war.getId(), player.getUniqueId());
                    MessageUtil.sendSuccess(player, "Guerre validée et démarrée!");

                    Nation attacker = plugin.getNationManager().getNationById(war.getAttackerNationId());
                    Nation defender = plugin.getNationManager().getNationById(war.getDefenderNationId());
                    if (attacker != null && defender != null) {
                        String msg = plugin.getConfigManager().getPrefix()
                            + "§c⚔ Guerre déclarée! §6" + attacker.getName()
                            + " §7contre §c" + defender.getName()
                            + " §7— Type: §e" + war.getType().getDisplayName();
                        plugin.getServer().broadcastMessage(MessageUtil.colorize(msg));
                    }
                }
                pendingWars.remove(i);
                player.openInventory(build());
                return;
            }
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
