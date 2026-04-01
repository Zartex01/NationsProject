package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class NationManageGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;

    public NationManageGui(NationsPlugin plugin, Player player, Nation nation) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6Gestion: " + nation.getName(), 4);
        GuiUtil.fillBorder(inv);

        NationMember member = nation.getMember(player.getUniqueId());
        boolean isLeader = nation.isLeader(player.getUniqueId());

        inv.setItem(10, GuiUtil.createItem(Material.NAME_TAG,
            "&eChanger le nom",
            "&7Modifier le nom de la nation"
        ));

        inv.setItem(12, GuiUtil.createItem(Material.BOOK,
            "&eChanger la description",
            "&7Modifier la description"
        ));

        boolean isOpen = nation.isOpen();
        inv.setItem(14, GuiUtil.createItem(
            isOpen ? Material.LIME_DYE : Material.RED_DYE,
            "&eAccès: " + (isOpen ? "&aOuvert" : "&cFermé"),
            "&7La nation est actuellement",
            isOpen ? "&aouverte à tous" : "&cfermée (sur invitation)"
        ));

        if (isLeader) {
            inv.setItem(16, GuiUtil.createItem(Material.TNT,
                "&c&lDissoudre la nation",
                "&7ATTENTION: Action irréversible!",
                "&7Toutes les données seront perdues"
            ));
        }

        inv.setItem(31, GuiUtil.createItem(Material.BARRIER, "&cRetour"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        switch (slot) {
            case 10 -> {
                player.closeInventory();
                MessageUtil.send(player, "&7Entrez le &anouveau nom &7de la nation dans le chat:");
                GuiManager.setPendingAction(player.getUniqueId(), "nation_rename:" + nation.getId());
            }
            case 12 -> {
                player.closeInventory();
                MessageUtil.send(player, "&7Entrez la &anouvelle description &7dans le chat:");
                GuiManager.setPendingAction(player.getUniqueId(), "nation_description:" + nation.getId());
            }
            case 14 -> {
                nation.setOpen(!nation.isOpen());
                plugin.getNationManager().saveNationToDatabase(nation);
                MessageUtil.sendSuccess(player, "La nation est maintenant " + (nation.isOpen() ? "ouverte" : "fermée") + ".");
                player.openInventory(build());
            }
            case 16 -> {
                if (nation.isLeader(player.getUniqueId())) {
                    new ConfirmDisbandGui(plugin, player, nation).open();
                }
            }
            case 31 -> new NationMainGui(plugin, player, nation).open();
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
