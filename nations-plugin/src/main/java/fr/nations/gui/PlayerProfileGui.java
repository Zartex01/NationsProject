package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
import fr.nations.grade.PlayerGrade;
import fr.nations.nation.Nation;
import fr.nations.season.PlayerStats;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class PlayerProfileGui {

    private final NationsPlugin plugin;
    private final Player viewer;
    private final Player target;

    public PlayerProfileGui(NationsPlugin plugin, Player viewer, Player target) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.target = target;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&6Profil de " + target.getName(), 4);
        GuiUtil.fillBorder(inv);

        PlayerGrade grade = plugin.getGradeManager().getOrCreatePlayerGrade(target.getUniqueId(), target.getName());
        GradeType gradeType = GradeType.fromPermission(target);
        PlayerStats stats = plugin.getSeasonManager().getOrCreatePlayerStats(target.getUniqueId());
        Nation nation = plugin.getNationManager().getPlayerNation(target.getUniqueId());

        double xpPercent = plugin.getGradeManager().getLevelProgressPercent(grade);
        int maxClaims = gradeType.getMaxClaims();

        inv.setItem(13, GuiUtil.createItem(Material.PLAYER_HEAD,
            gradeType.getColoredDisplay() + " &f" + target.getName(),
            "&7Grade: " + gradeType.getColoredDisplay(),
            "&7Niveau: &e" + grade.getLevel() + " §7(§e" + String.format("%.1f", xpPercent) + "% §7vers §e" + (grade.getLevel() + 1) + "§7)",
            "&7XP: &e" + grade.getXp() + " / " + grade.getXpForNextLevel(),
            "&7Nation: " + (nation != null ? "&6" + nation.getName() : "&7Aucune"),
            "&7Claims: &f" + grade.getClaimCount() + "/" + maxClaims
        ));

        inv.setItem(10, GuiUtil.createItem(Material.DIAMOND_SWORD,
            "&cStatistiques de combat",
            "&7Kills: &c" + stats.getKills(),
            "&7Morts: &7" + stats.getDeaths(),
            "&7K/D: &e" + String.format("%.2f", stats.getKillDeathRatio()),
            "&7Guerres gagnées: &a" + stats.getWarsWon()
        ));

        inv.setItem(16, GuiUtil.createItem(Material.GOLD_NUGGET,
            "&6Économie",
            "&7Balance: &e" + MessageUtil.formatNumber(plugin.getEconomyManager().getBalance(target.getUniqueId())) + " coins"
        ));

        inv.setItem(31, GuiUtil.createItem(Material.BARRIER, "&cFermer"));
        GuiUtil.fillAll(inv);
        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == 31) {
            viewer.closeInventory();
        }
    }

    public void open() {
        viewer.openInventory(build());
        GuiManager.registerGui(viewer.getUniqueId(), this);
    }
}
