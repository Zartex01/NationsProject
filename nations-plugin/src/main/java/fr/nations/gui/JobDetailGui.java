package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.jobs.JobType;
import fr.nations.jobs.PlayerJobData;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobDetailGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final JobType job;

    public JobDetailGui(NationsPlugin plugin, Player player, JobType job) {
        this.plugin = plugin;
        this.player = player;
        this.job = job;
    }

    public Inventory build() {
        PlayerJobData data = plugin.getJobManager().getData(player.getUniqueId(), job);
        String color = job.getColor();
        String title = color + "§l◆ " + job.getDisplayName().toUpperCase() + " ◆";
        Inventory inv = GuiUtil.createGui(title, 6);

        // ── Bordures ──────────────────────────────────────────────────────────
        Material border = getBorderMaterial();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, GuiUtil.createFillerItem(border));
            inv.setItem(45 + i, GuiUtil.createFillerItem(border));
        }
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, GuiUtil.createFillerItem(border));
            inv.setItem(row * 9 + 8, GuiUtil.createFillerItem(border));
        }
        // Remplissage intérieur neutre
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                inv.setItem(row * 9 + col, GuiUtil.createFillerItem(Material.BLACK_STAINED_GLASS_PANE));
            }
        }

        // ── Icône centrale du métier (slot 13) ───────────────────────────────
        inv.setItem(13, buildMainIcon(data));

        // ── Barre de progression (slots 19-25) ───────────────────────────────
        buildProgressBar(inv, data, 19);

        // ── Activités et XP (slot 28) ────────────────────────────────────────
        inv.setItem(28, buildActivitiesItem());

        // ── Paliers de récompenses (slot 30) ─────────────────────────────────
        inv.setItem(30, buildMilestonesItem(data));

        // ── Statistiques (slot 32) ───────────────────────────────────────────
        inv.setItem(32, buildStatsItem(data));

        // ── Prochain niveau (slot 34) ─────────────────────────────────────────
        inv.setItem(34, buildNextLevelItem(data));

        // ── Bouton retour (slot 49) ──────────────────────────────────────────
        inv.setItem(49, GuiUtil.createItem(Material.ARROW, "§7« Retour aux métiers"));

        // Décoration coins
        inv.setItem(10, GuiUtil.createFillerItem(border));
        inv.setItem(16, GuiUtil.createFillerItem(border));
        inv.setItem(37, GuiUtil.createFillerItem(border));
        inv.setItem(43, GuiUtil.createFillerItem(border));

        return inv;
    }

    // ── Icône principale ──────────────────────────────────────────────────────
    private ItemStack buildMainIcon(PlayerJobData data) {
        String color = job.getColor();
        int level = data.getLevel();
        String stars = buildStars(level);

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(job.getDescription());
        lore.add("");
        lore.add("  §7Niveau: " + color + "§l" + level + " §8/ §7" + PlayerJobData.MAX_LEVEL);
        lore.add("  §7Prestige: §e" + stars);
        lore.add("");
        lore.add("  §7XP actuelle: §a" + data.getXp());
        if (!data.isMaxLevel()) {
            lore.add("  §7XP requise: §e" + PlayerJobData.xpToNextLevel(level));
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        ItemStack item = new ItemStack(job.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "§l✦ " + job.getDisplayName() + " §r" + color + "§l✦");
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(MessageUtil.colorize(l));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Barre de progression (7 slots) ────────────────────────────────────────
    private void buildProgressBar(Inventory inv, PlayerJobData data, int startSlot) {
        double pct = data.progressPercent();
        int filled = (int) Math.round(pct * 7);

        Material filledMat = getFilledBarMaterial();
        Material emptyMat  = Material.GRAY_STAINED_GLASS_PANE;
        String color = job.getColor();

        for (int i = 0; i < 7; i++) {
            boolean isFilled = i < filled;
            Material mat = isFilled ? filledMat : emptyMat;
            String pctStr = (int)(pct * 100) + "%";
            String label;
            if (i == 3) {
                // Milieu : afficher le pourcentage
                label = data.isMaxLevel() ? "§5§l MAX " : color + "§l" + pctStr;
            } else {
                label = isFilled ? color + "█" : "§8░";
            }
            inv.setItem(startSlot + i, GuiUtil.createItem(mat, label,
                isFilled
                    ? color + "Progression: §f" + pctStr
                    : "§8░ §7Non complété"
            ));
        }
    }

    // ── Activités et XP ──────────────────────────────────────────────────────
    private ItemStack buildActivitiesItem() {
        String color = job.getColor();
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Gains d'XP par action:");
        lore.add("");

        switch (job) {
            case MINEUR -> {
                lore.add("  §fCharbon: §a+5 XP");
                lore.add("  §fCuivre: §a+8 XP");
                lore.add("  §fFer: §a+10 XP");
                lore.add("  §fOr: §a+15 XP");
                lore.add("  §fLapis: §a+15 XP");
                lore.add("  §fRedstone: §a+12 XP");
                lore.add("  §bDiamant: §a+40 XP");
                lore.add("  §aÉmeraude: §a+50 XP");
                lore.add("  §5Ancient Debris: §a+80 XP");
            }
            case FARMEUR -> {
                lore.add("  §fBlé/Carotte/Pomme de terre: §a+5 XP");
                lore.add("  §fBetterave: §a+5 XP");
                lore.add("  §fMelon/Citrouille: §a+10 XP");
                lore.add("  §fCanne à sucre/Cactus: §a+3 XP");
                lore.add("  §fVerrue du nether: §a+8 XP");
                lore.add("  §7(Uniquement cultures matures)");
            }
            case CHASSEUR -> {
                lore.add("  §fAnimaux paisibles: §a+3-5 XP");
                lore.add("  §fZombie/Squelette: §a+10 XP");
                lore.add("  §fCreeper/Enderman: §a+15-20 XP");
                lore.add("  §fBlaze/Wither Squelette: §a+20-22 XP");
                lore.add("  §cWarden: §a+100 XP");
                lore.add("  §cEnder Dragon: §a+200 XP");
            }
            case BUCHERON -> {
                lore.add("  §fTous les types de bûches: §a+5 XP");
                lore.add("  §fBambou: §a+2 XP");
                lore.add("  §fChampignon géant: §a+3 XP");
            }
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬");
        return GuiUtil.createItem(Material.KNOWLEDGE_BOOK, color + "Activités & XP", lore.toArray(new String[0]));
    }

    // ── Paliers de récompenses ────────────────────────────────────────────────
    private ItemStack buildMilestonesItem(PlayerJobData data) {
        String color = job.getColor();
        int level = data.getLevel();

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Paliers spéciaux:");
        lore.add("");
        lore.add(milestone(level, 5,  "§6+500 coins bonus"));
        lore.add(milestone(level, 10, "§6+2 000 coins bonus"));
        lore.add(milestone(level, 25, "§6+10 000 coins bonus"));
        lore.add(milestone(level, 50, "§5+50 000 coins §5(MAX)"));
        lore.add("");
        lore.add("§7+§e" + color + (level * 100) + " coins §7par niveau");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬");
        return GuiUtil.createItem(Material.GOLD_BLOCK, color + "Paliers & Récompenses", lore.toArray(new String[0]));
    }

    private String milestone(int current, int target, String reward) {
        String status = current >= target ? "§a✔ " : (current < target ? "§8○ §7" : "§e➤ ");
        String color = current >= target ? "§a" : "§7";
        return "  " + status + color + "Niv. " + target + " §8— " + reward;
    }

    // ── Statistiques ─────────────────────────────────────────────────────────
    private ItemStack buildStatsItem(PlayerJobData data) {
        String color = job.getColor();
        return GuiUtil.createItem(Material.PAPER,
            color + "Mes Statistiques",
            "§7Niveau: " + color + "§l" + data.getLevel() + " §8/ §7" + PlayerJobData.MAX_LEVEL,
            "§7XP actuelle: §a" + String.format("%,d", data.getXp()),
            "§7XP total gagné: §a" + String.format("%,d", data.getTotalXpEarned()),
            "§7Coins gagnés: §6" + String.format("%,d", data.getCoinsEarned())
        );
    }

    // ── Prochain niveau ───────────────────────────────────────────────────────
    private ItemStack buildNextLevelItem(PlayerJobData data) {
        String color = job.getColor();
        if (data.isMaxLevel()) {
            return GuiUtil.createItem(Material.BEACON,
                "§5§l✦ NIVEAU MAXIMUM ✦",
                "§7Tu as atteint le niveau maximum",
                "§7de ce métier. Félicitations!",
                "",
                "§5✦ Maître " + job.getDisplayName() + "!"
            );
        }
        int nextLvl = data.getLevel() + 1;
        int reward = PlayerJobData.coinsRewardForLevel(nextLvl);
        return GuiUtil.createItem(Material.EMERALD,
            color + "Prochain Niveau — " + color + "§l" + nextLvl,
            "§7XP manquante: §e" + data.xpNeeded() + " XP",
            "§7Récompense: §6" + String.format("%,d", reward) + " coins",
            "",
            data.progressBar()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String buildStars(int level) {
        if (level < 10)  return "§7Novice";
        if (level < 20)  return "§aApprenti §e✦";
        if (level < 30)  return "§2Confirmé §e✦✦";
        if (level < 40)  return "§bExpert §e✦✦✦";
        if (level < 50)  return "§6Maître §6✦✦✦✦";
        return "§5§l✦ LÉGENDE ✦";
    }

    private Material getBorderMaterial() {
        return switch (job) {
            case MINEUR   -> Material.GRAY_STAINED_GLASS_PANE;
            case FARMEUR  -> Material.GREEN_STAINED_GLASS_PANE;
            case CHASSEUR -> Material.RED_STAINED_GLASS_PANE;
            case BUCHERON -> Material.ORANGE_STAINED_GLASS_PANE;
        };
    }

    private Material getFilledBarMaterial() {
        return switch (job) {
            case MINEUR   -> Material.GRAY_STAINED_GLASS_PANE;
            case FARMEUR  -> Material.LIME_STAINED_GLASS_PANE;
            case CHASSEUR -> Material.RED_STAINED_GLASS_PANE;
            case BUCHERON -> Material.ORANGE_STAINED_GLASS_PANE;
        };
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == 49) {
            new JobsGui(plugin, player).open();
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
