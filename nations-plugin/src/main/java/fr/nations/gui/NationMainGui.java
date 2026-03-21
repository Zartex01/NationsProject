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
import org.bukkit.inventory.ItemStack;

public class NationMainGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;

    public NationMainGui(NationsPlugin plugin, Player player, Nation nation) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6&l" + nation.getName(), 4);
        GuiUtil.fillBorder(inv);

        NationMember member = nation.getMember(player.getUniqueId());
        NationRole role = member != null ? member.getRole() : NationRole.RECRUIT;

        inv.setItem(10, GuiUtil.createItem(Material.PLAYER_HEAD, "&6Informations",
            "&7Nom: &f" + nation.getName(),
            "&7Chef: &f" + getLeaderName(),
            "&7Membres: &f" + nation.getMemberCount(),
            "&7Points de saison: &e" + nation.getSeasonPoints(),
            "&7Description: &f" + (nation.getDescription().isEmpty() ? "Aucune" : nation.getDescription())
        ));

        inv.setItem(12, GuiUtil.createItem(Material.BOOK, "&eMembres",
            "&7Voir la liste des membres",
            "&7et gérer les rôles"
        ));

        inv.setItem(14, GuiUtil.createItem(Material.GOLD_INGOT, "&6Banque",
            "&7Solde: &f" + MessageUtil.formatNumber(nation.getBankBalance()) + " coins",
            "",
            "&7Déposer ou retirer de l'argent"
        ));

        inv.setItem(16, GuiUtil.createItem(Material.DIAMOND_SWORD, "&cGuerres",
            "&7Voir les guerres actives",
            "&7et déclarer des guerres"
        ));

        inv.setItem(22, GuiUtil.createItem(Material.MAP, "&aTerritoire",
            "&7Claims: &f" + plugin.getTerritoryManager().getClaimCountForNation(nation.getId()),
            "&7Alliés: &f" + nation.getAllies().size()
        ));

        if (role.isHigherThan(NationRole.MEMBER)) {
            inv.setItem(31, GuiUtil.createItem(Material.COMPARATOR, "&7Gestion",
                "&7Modifier les paramètres",
                "&7de la nation"
            ));
        }

        GuiUtil.fillAll(inv);
        return inv;
    }

    private String getLeaderName() {
        NationMember leaderMember = nation.getMember(nation.getLeaderId());
        return leaderMember != null ? leaderMember.getPlayerName() : "Inconnu";
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        switch (slot) {
            case 12 -> new MembersGui(plugin, player, nation).open();
            case 14 -> new BankGui(plugin, player, nation).open();
            case 16 -> new WarsGui(plugin, player).open();
            case 31 -> {
                NationMember member = nation.getMember(player.getUniqueId());
                if (member != null && member.getRole().isHigherThan(NationRole.MEMBER)) {
                    new NationManageGui(plugin, player, nation).open();
                }
            }
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
