package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.gui.*;
import fr.nations.gui.KitGui;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class GuiClickListener implements Listener {

    private final NationsPlugin plugin;

    public GuiClickListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Object gui = GuiManager.getOpenGui(player.getUniqueId());
        if (gui == null) return;

        if (gui instanceof NationMainGui nationGui) {
            nationGui.handleClick(event);
        } else if (gui instanceof MembersGui membersGui) {
            membersGui.handleClick(event);
        } else if (gui instanceof BankGui bankGui) {
            bankGui.handleClick(event);
        } else if (gui instanceof WarsGui warsGui) {
            warsGui.handleClick(event);
        } else if (gui instanceof NationManageGui manageGui) {
            manageGui.handleClick(event);
        } else if (gui instanceof ConfirmDisbandGui disbandGui) {
            disbandGui.handleClick(event);
        } else if (gui instanceof SeasonGui seasonGui) {
            seasonGui.handleClick(event);
        } else if (gui instanceof StaffWarsGui staffGui) {
            staffGui.handleClick(event);
        } else if (gui instanceof PlayerProfileGui profileGui) {
            profileGui.handleClick(event);
        } else if (gui instanceof fr.nations.gui.RolePermissionsGui roleGui) {
            roleGui.handleClick(event);
        } else if (gui instanceof KitGui kitGui) {
            kitGui.handleClick(event);
        } else if (gui instanceof ShopGui shopGui) {
            shopGui.handleClick(event);
        } else if (gui instanceof ShopCategoryGui shopCatGui) {
            shopCatGui.handleClick(event);
        } else if (gui instanceof HdvGui hdvGui) {
            hdvGui.handleClick(event);
        } else if (gui instanceof HdvMyListingsGui hdvMyGui) {
            hdvMyGui.handleClick(event);
        } else if (gui instanceof HdvSellGui hdvSellGui) {
            hdvSellGui.handleClick(event);
        } else if (gui instanceof HdvConfirmGui hdvConfirmGui) {
            hdvConfirmGui.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        GuiManager.closeGui(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!GuiManager.hasPendingAction(playerId)) return;

        String action = GuiManager.getPendingAction(playerId);
        String input = event.getMessage().trim();
        event.setCancelled(true);
        GuiManager.clearPendingAction(playerId);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            handlePendingAction(player, action, input);
        });
    }

    private void handlePendingAction(Player player, String action, String input) {
        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("annuler")) {
            MessageUtil.send(player, "§7Action annulée.");
            return;
        }

        String[] parts = action.split(":", 2);
        String actionType = parts[0];
        String param = parts.length > 1 ? parts[1] : "";

        switch (actionType) {
            case "bank_deposit" -> {
                try {
                    double amount = Double.parseDouble(input);
                    if (amount <= 0) { MessageUtil.sendError(player, "Montant invalide."); return; }
                    Nation nation = plugin.getNationManager().getNationById(java.util.UUID.fromString(param));
                    if (nation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
                    if (!plugin.getEconomyManager().has(player.getUniqueId(), amount)) {
                        MessageUtil.sendError(player, "Fonds insuffisants."); return;
                    }
                    plugin.getEconomyManager().withdraw(player.getUniqueId(), amount);
                    nation.depositToBank(amount);
                    plugin.getDataManager().saveNations();
                    MessageUtil.sendSuccess(player, "§e" + MessageUtil.formatNumber(amount) + " coins §adéposés dans la banque.");
                    new BankGui(plugin, player, nation).open();
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(player, "Montant invalide.");
                }
            }
            case "bank_withdraw" -> {
                try {
                    double amount = Double.parseDouble(input);
                    if (amount <= 0) { MessageUtil.sendError(player, "Montant invalide."); return; }
                    Nation nation = plugin.getNationManager().getNationById(java.util.UUID.fromString(param));
                    if (nation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
                    if (!nation.withdrawFromBank(amount)) {
                        MessageUtil.sendError(player, "Fonds insuffisants dans la banque."); return;
                    }
                    plugin.getEconomyManager().deposit(player.getUniqueId(), amount);
                    plugin.getDataManager().saveNations();
                    MessageUtil.sendSuccess(player, "§e" + MessageUtil.formatNumber(amount) + " coins §aretirés de la banque.");
                    new BankGui(plugin, player, nation).open();
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(player, "Montant invalide.");
                }
            }
            case "nation_rename" -> {
                Nation nation = plugin.getNationManager().getNationById(java.util.UUID.fromString(param));
                if (nation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
                if (!input.matches("[a-zA-Z0-9_]+") || input.length() < 3 || input.length() > 20) {
                    MessageUtil.sendError(player, "Nom invalide (3-20 caractères, lettres/chiffres/_)."); return;
                }
                if (plugin.getNationManager().getNationByName(input) != null) {
                    MessageUtil.sendError(player, "Ce nom est déjà pris."); return;
                }
                String oldName = nation.getName();
                nation.setName(input);
                plugin.getDataManager().saveNations();
                MessageUtil.sendSuccess(player, "Nation renommée: §6" + oldName + " §a→ §6" + input);
                new NationManageGui(plugin, player, nation).open();
            }
            case "nation_description" -> {
                Nation nation = plugin.getNationManager().getNationById(java.util.UUID.fromString(param));
                if (nation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
                if (input.length() > 100) { MessageUtil.sendError(player, "Description trop longue (max 100 chars)."); return; }
                nation.setDescription(input);
                plugin.getDataManager().saveNations();
                MessageUtil.sendSuccess(player, "Description mise à jour.");
                new NationManageGui(plugin, player, nation).open();
            }
        }
    }
}
