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

public class BankGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;

    public BankGui(NationsPlugin plugin, Player player, Nation nation) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6Banque de " + nation.getName(), 4);
        GuiUtil.fillBorder(inv);

        double personalBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        double nationBalance = nation.getBankBalance();

        inv.setItem(13, GuiUtil.createItem(Material.GOLD_BLOCK,
            "&6Solde de la nation",
            "&7Balance: &e" + MessageUtil.formatNumber(nationBalance) + " coins",
            "",
            "&7Balance personnelle: &a" + MessageUtil.formatNumber(personalBalance) + " coins"
        ));

        NationMember member = nation.getMember(player.getUniqueId());
        boolean canManageBank  = member != null && member.canManageBank();
        boolean canDepositBank = member != null && member.canDepositBank();

        if (canDepositBank) {
            inv.setItem(11, GuiUtil.createItem(Material.HOPPER,
                "&aDepôt",
                "&7Deposer de votre argent",
                "&7dans la banque de la nation",
                "",
                "&eCliquer pour deposer"
            ));
        } else {
            inv.setItem(11, GuiUtil.createItem(Material.BARRIER,
                "&cDepôt non autorisé",
                "&7Votre rôle ne peut pas",
                "&7deposer dans la banque"
            ));
        }

        if (canManageBank) {
            inv.setItem(15, GuiUtil.createItem(Material.CHEST,
                "&cRetrait",
                "&7Retirer de l'argent",
                "&7de la banque de la nation",
                "",
                "&eCliquer pour retirer"
            ));
        } else {
            inv.setItem(15, GuiUtil.createItem(Material.BARRIER,
                "&cRetrait non autorisé",
                "&7Seuls les officiers+",
                "&7peuvent retirer de l'argent"
            ));
        }

        inv.setItem(31, GuiUtil.createItem(Material.BARRIER, "&cRetour"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 11) {
            NationMember clickMember = nation.getMember(player.getUniqueId());
            if (clickMember == null || !clickMember.canDepositBank()) {
                fr.nations.util.MessageUtil.sendError(player, "Vous n'avez pas la permission de deposer dans la banque.");
                return;
            }
            player.closeInventory();
            MessageUtil.send(player, "&7Entrez le montant a &aDeposer &7dans le chat: (ex: §e100§7)");
            GuiManager.setPendingAction(player.getUniqueId(), "bank_deposit:" + nation.getId());
        } else if (slot == 15) {
            NationMember member = nation.getMember(player.getUniqueId());
            if (member != null && member.canManageBank()) {
                player.closeInventory();
                MessageUtil.send(player, "&7Entrez le montant à &cRetirer &7dans le chat: (ex: §e100§7)");
                GuiManager.setPendingAction(player.getUniqueId(), "bank_withdraw:" + nation.getId());
            }
        } else if (slot == 31) {
            new NationMainGui(plugin, player, nation).open();
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
