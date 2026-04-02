package fr.nations.listeners;

import fr.nations.NationsPlugin;
import fr.nations.gui.*;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
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
        } else if (gui instanceof HdvSellGui hdvSellGui) {
            hdvSellGui.handleClick(event);
        } else if (gui instanceof HdvBrowseGui hdvBrowseGui) {
            hdvBrowseGui.handleClick(event);
        } else if (gui instanceof fr.nations.gui.NationRoleListGui roleListGui) {
            roleListGui.handleClick(event);
        } else if (gui instanceof fr.nations.gui.CustomRoleEditHolder customRoleGui) {
            customRoleGui.handleClick(event);
        } else if (gui instanceof fr.nations.gui.JobsGui jobsGui) {
            jobsGui.handleClick(event);
        } else if (gui instanceof fr.nations.gui.JobDetailGui jobDetailGui) {
            jobDetailGui.handleClick(event);
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
                    plugin.getNationManager().saveNationToDatabase(nation);
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
                    plugin.getNationManager().saveNationToDatabase(nation);
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
                plugin.getNationManager().saveNationToDatabase(nation);
                MessageUtil.sendSuccess(player, "Nation renommée: §6" + oldName + " §a→ §6" + input);
                new NationManageGui(plugin, player, nation).open();
            }
            case "hdv_set_price" -> {
                try {
                    double price = Double.parseDouble(input);
                    if (price <= 0) { MessageUtil.sendError(player, "Le prix doit être supérieur à 0."); return; }
                    if (price > 1_000_000_000) { MessageUtil.sendError(player, "Le prix ne peut pas dépasser 1 milliard."); return; }
                    Object pending = GuiManager.getPendingGui(player.getUniqueId());
                    GuiManager.clearPendingGui(player.getUniqueId());
                    HdvSellGui sellGui;
                    if (pending instanceof HdvSellGui existing) {
                        sellGui = existing;
                    } else {
                        sellGui = new HdvSellGui(plugin, player, player.getInventory().getItemInMainHand());
                    }
                    sellGui.setPrice(price);
                    sellGui.open();
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(player, "Prix invalide. Entrez un nombre (ex: &e1000&c).");
                }
            }
            case "nation_description" -> {
                Nation nation = plugin.getNationManager().getNationById(java.util.UUID.fromString(param));
                if (nation == null) { MessageUtil.sendError(player, "Nation introuvable."); return; }
                if (input.length() > 100) { MessageUtil.sendError(player, "Description trop longue (max 100 chars)."); return; }
                nation.setDescription(input);
                plugin.getNationManager().saveNationToDatabase(nation);
                MessageUtil.sendSuccess(player, "Description mise à jour.");
                new NationManageGui(plugin, player, nation).open();
            }
            case "custom_role_rename" -> {
                java.util.UUID roleId = java.util.UUID.fromString(param);
                fr.nations.role.CustomRole role = plugin.getCustomRoleManager().getRole(roleId);
                if (role == null) { MessageUtil.sendError(player, "Rôle introuvable."); return; }
                if (input.length() < 2 || input.length() > 24) {
                    MessageUtil.sendError(player, "Nom invalide (2-24 caractères).");
                    return;
                }
                role.setDisplayName(input);
                plugin.getCustomRoleManager().saveRole(role);
                MessageUtil.sendSuccess(player, "Rôle renommé en §6" + input + "§a.");
                Nation nation = plugin.getNationManager().getNationById(role.getNationId());
                if (nation != null) {
                    player.openInventory(fr.nations.gui.RolesGui.buildRoleEditGui(plugin, role));
                    fr.nations.gui.GuiManager.registerGui(player.getUniqueId(),
                        new fr.nations.gui.CustomRoleEditHolder(plugin, player, nation, role));
                }
            }
            case "housing_name" -> {
                // param = "minX,minY,minZ,maxX,maxY,maxZ,world"
                if (input.length() < 2 || input.length() > 32) {
                    MessageUtil.sendError(player, "Nom invalide (2-32 caractères).");
                    return;
                }
                GuiManager.setPendingAction(player.getUniqueId(), "housing_price:" + input + ":" + param);
                player.sendMessage("§eTapez le §6prix §edu logement en coins §8(ou §cannuler§8):");
            }
            case "housing_price" -> {
                // param = "nom:minX,minY,minZ,maxX,maxY,maxZ,world"
                int colonIdx = param.indexOf(':');
                if (colonIdx < 0) { MessageUtil.sendError(player, "Erreur interne."); return; }
                String housingName = param.substring(0, colonIdx);
                String coords = param.substring(colonIdx + 1);
                String[] coordParts = coords.split(",");
                if (coordParts.length < 7) { MessageUtil.sendError(player, "Erreur interne (coords)."); return; }
                try {
                    double price = Double.parseDouble(input);
                    if (price < 0) { MessageUtil.sendError(player, "Le prix ne peut pas être négatif."); return; }
                    if (price > 1_000_000_000) { MessageUtil.sendError(player, "Prix trop élevé."); return; }
                    int minX = Integer.parseInt(coordParts[0]), minY = Integer.parseInt(coordParts[1]), minZ = Integer.parseInt(coordParts[2]);
                    int maxX = Integer.parseInt(coordParts[3]), maxY = Integer.parseInt(coordParts[4]), maxZ = Integer.parseInt(coordParts[5]);
                    String world = coordParts[6];

                    Nation playerNation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
                    if (playerNation == null) { MessageUtil.sendError(player, "Vous n'avez pas de nation."); return; }

                    java.util.UUID housingId = java.util.UUID.randomUUID();
                    fr.nations.housing.Housing housing = new fr.nations.housing.Housing(
                        housingId, playerNation.getId(), housingName, price,
                        minX, minY, minZ, maxX, maxY, maxZ, world
                    );
                    plugin.getHousingManager().createHousing(housing);
                    plugin.getHousingListener().setPendingSign(player.getUniqueId(), housingId);

                    player.sendMessage("§a§l✔ §aLogement §6" + housingName + " §acréé! Prix: §6" + (int) price + " coins§a.");
                    player.sendMessage("§ePlacez maintenant un §6panneau §edans ou à côté de la zone pour finaliser.");
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(player, "Prix invalide. Entrez un nombre (ex: §e1000§c).");
                }
            }
        }
    }
}
