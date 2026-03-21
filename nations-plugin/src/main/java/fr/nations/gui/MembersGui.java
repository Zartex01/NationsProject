package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.util.GuiUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class MembersGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;
    private int page;
    private final List<NationMember> memberList;

    public MembersGui(NationsPlugin plugin, Player player, Nation nation) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
        this.page = 0;
        this.memberList = new ArrayList<>(nation.getMembers().values());
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6Membres de " + nation.getName(), 5);
        GuiUtil.fillBorder(inv);

        int startIndex = page * 21;
        int slot = 10;
        int count = 0;

        for (int i = startIndex; i < memberList.size() && count < 21; i++, count++) {
            NationMember member = memberList.get(i);
            boolean isLeader = nation.isLeader(member.getPlayerId());

            Material mat = isLeader ? Material.GOLDEN_HELMET : Material.PLAYER_HEAD;
            inv.setItem(slot, GuiUtil.createItem(mat,
                member.getRole().getColoredDisplay() + " &f" + member.getPlayerName(),
                "&7Rôle: " + member.getRole().getDisplayName(),
                "&7UUID: &8" + member.getPlayerId().toString().substring(0, 8) + "..."
            ));

            slot++;
            if ((slot % 9) == 8) slot += 2;
        }

        if (page > 0) {
            inv.setItem(39, GuiUtil.createItem(Material.ARROW, "&7← Page précédente"));
        }
        if (startIndex + 21 < memberList.size()) {
            inv.setItem(41, GuiUtil.createItem(Material.ARROW, "&7Page suivante →"));
        }

        inv.setItem(40, GuiUtil.createItem(Material.BARRIER, "&cRetour"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 39 && page > 0) {
            page--;
            player.openInventory(build());
        } else if (slot == 41 && (page + 1) * 21 < memberList.size()) {
            page++;
            player.openInventory(build());
        } else if (slot == 40) {
            new NationMainGui(plugin, player, nation).open();
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
