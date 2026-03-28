package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.role.CustomRole;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI d'assignation d'un rôle aux membres de la nation.
 *
 * Affiche les membres (tête de joueur) :
 *   Clic gauche  → attribuer ce rôle
 *   Clic droit   → retirer ce rôle (si assigné)
 *   Slot 49      → retour à NationRoleEditGui
 *
 * Layout : 6 rangées — bordure violette, membres en slots intérieurs.
 */
public class NationRoleAssignGui {

    private static final int[] MEMBER_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;
    private final CustomRole role;

    public NationRoleAssignGui(NationsPlugin plugin, Player player, Nation nation, CustomRole role) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
        this.role   = role;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Build
    // ──────────────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&5Assigner: &d" + role.getDisplayName(), 6);
        GuiUtil.fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);

        List<NationMember> members = new ArrayList<>(nation.getMembers());
        // Exclure leader/co-leader (non-assignables à un rôle custom)
        members.removeIf(m -> m.getRole() == NationRole.LEADER || m.getRole() == NationRole.CO_LEADER);

        for (int i = 0; i < Math.min(members.size(), MEMBER_SLOTS.length); i++) {
            NationMember member = members.get(i);
            CustomRole current  = plugin.getCustomRoleManager().getPlayerRole(member.getPlayerId());
            boolean hasThisRole = current != null && current.getId().equals(role.getId());

            ItemStack skull = makeSkull(member.getPlayerId(), member.getPlayerName());
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtil.colorize("&f" + member.getPlayerName()));
                List<String> lore = new ArrayList<>();
                lore.add("&7Rôle nation: &e" + member.getRole().name());
                lore.add("&7Rôle custom: &f" + (current != null ? current.getDisplayName() : "Aucun"));
                lore.add("");
                if (hasThisRole) {
                    lore.add("&a✔ A déjà ce rôle");
                    lore.add("&cClique droit &7→ Retirer ce rôle");
                } else {
                    lore.add("&eClique gauche &7→ Attribuer &d" + role.getDisplayName());
                }
                meta.setLore(lore.stream().map(MessageUtil::colorize).toList());
                skull.setItemMeta(meta);
            }
            inv.setItem(MEMBER_SLOTS[i], skull);
        }

        if (members.isEmpty()) {
            inv.setItem(22, GuiUtil.createItem(Material.BARRIER,
                "&cAucun membre disponible",
                "&7Le leader et le co-leader ne",
                "&7peuvent pas recevoir de rôle custom."));
        }

        inv.setItem(49, GuiUtil.createItem(Material.BARRIER, "&c◄ Retour"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Click
    // ──────────────────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 49) {
            new NationRoleEditGui(plugin, player, nation, role).open();
            return;
        }

        List<NationMember> members = new ArrayList<>(nation.getMembers());
        members.removeIf(m -> m.getRole() == NationRole.LEADER || m.getRole() == NationRole.CO_LEADER);

        for (int i = 0; i < Math.min(members.size(), MEMBER_SLOTS.length); i++) {
            if (slot == MEMBER_SLOTS[i]) {
                NationMember target = members.get(i);
                CustomRole current  = plugin.getCustomRoleManager().getPlayerRole(target.getPlayerId());
                boolean hasThisRole = current != null && current.getId().equals(role.getId());

                if (event.isRightClick() && hasThisRole) {
                    plugin.getCustomRoleManager().removePlayerRole(target.getPlayerId(), nation.getId());
                    MessageUtil.sendSuccess(player, "Rôle &d" + role.getDisplayName()
                        + " &aretié à &f" + target.getPlayerName() + "&a.");
                    notifyIfOnline(target, "&7Votre rôle personnalisé dans &e" + nation.getName() + " &7a été retiré.");
                } else if (!event.isRightClick()) {
                    plugin.getCustomRoleManager().assignPlayerRole(target.getPlayerId(), nation.getId(), role.getId());
                    MessageUtil.sendSuccess(player, "Rôle &d" + role.getDisplayName()
                        + " &aattribué à &f" + target.getPlayerName() + "&a.");
                    notifyIfOnline(target, "&6[Nation] &7Vous avez reçu le rôle &d" + role.getDisplayName()
                        + " &7dans &e" + nation.getName() + "&7.");
                }
                refresh();
                return;
            }
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────

    private void refresh() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }

    private void notifyIfOnline(NationMember member, String message) {
        Player online = Bukkit.getPlayer(member.getPlayerId());
        if (online != null) MessageUtil.send(online, message);
    }

    @SuppressWarnings("deprecation")
    private ItemStack makeSkull(java.util.UUID playerId, String playerName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(playerId);
            meta.setOwningPlayer(op);
            skull.setItemMeta(meta);
        }
        return skull;
    }
}
