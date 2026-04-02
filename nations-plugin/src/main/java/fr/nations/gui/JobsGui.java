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

public class JobsGui {

    private final NationsPlugin plugin;
    private final Player player;

    // Slots des cartes de métiers
    private static final int[] JOB_ICON_SLOTS  = {10, 12, 14, 16};
    private static final int[] JOB_BAR_SLOTS   = {19, 21, 23, 25};
    private static final int[] JOB_STAT_SLOTS  = {28, 30, 32, 34};
    private static final int[] JOB_RWD_SLOTS   = {37, 39, 41, 43};

    private static final JobType[] JOBS = JobType.values();

    // Couleurs des bordures par job
    private static final Material[] JOB_GLASS = {
        Material.GRAY_STAINED_GLASS_PANE,   // Mineur
        Material.GREEN_STAINED_GLASS_PANE,  // Farmeur
        Material.RED_STAINED_GLASS_PANE,    // Chasseur
        Material.ORANGE_STAINED_GLASS_PANE  // Bûcheron
    };

    public JobsGui(NationsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("§8« §6§lMÉTIERS §8»", 6);

        // ── Bordures extérieures ─────────────────────────────────────────────
        Material border = Material.BLUE_STAINED_GLASS_PANE;
        // Ligne du haut et du bas
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, GuiUtil.createFillerItem(border));
            inv.setItem(45 + i, GuiUtil.createFillerItem(border));
        }
        // Colonnes séparatrices (0, 2, 4, 6, 8) des lignes 1-4
        for (int row = 1; row <= 4; row++) {
            for (int col : new int[]{0, 2, 4, 6, 8}) {
                int slot = row * 9 + col;
                inv.setItem(slot, GuiUtil.createFillerItem(Material.GRAY_STAINED_GLASS_PANE));
            }
        }

        // ── Ligne de titre ───────────────────────────────────────────────────
        ItemStack titleItem = GuiUtil.createItem(Material.NETHER_STAR, "§6§l✦ Système de Métiers §6§l✦",
            "§7Choisissez un métier pour",
            "§7progresser et gagner des récompenses.",
            "§8",
            "§e§lCliquez §esur un métier pour les détails."
        );
        inv.setItem(4, titleItem);

        // ── Colonnes déco par job (entre séparateurs) ────────────────────────
        for (int i = 0; i < 4; i++) {
            int col = 1 + i * 2; // cols 1,3,5,7
            // Deco glass column
            for (int row = 1; row <= 4; row++) {
                int slot = row * 9 + col;
                // Seuls les slots d'icônes/info sont remplis, pas de deco ici
            }
        }

        // ── Remplissage des cartes ────────────────────────────────────────────
        Map<JobType, PlayerJobData> allData = plugin.getJobManager().getAllData(player.getUniqueId());

        for (int i = 0; i < JOBS.length; i++) {
            JobType job = JOBS[i];
            PlayerJobData data = allData.get(job);
            String color = job.getColor();

            // Icône principale
            inv.setItem(JOB_ICON_SLOTS[i], buildJobIcon(job, data));
            // Barre XP
            inv.setItem(JOB_BAR_SLOTS[i], buildXpBar(job, data));
            // Statistiques
            inv.setItem(JOB_STAT_SLOTS[i], buildStats(job, data));
            // Récompenses
            inv.setItem(JOB_RWD_SLOTS[i], buildRewards(job, data));

            // Couleur décorative en haut et bas de chaque colonne
            inv.setItem(1 + i * 2, GuiUtil.createFillerItem(JOB_GLASS[i]));
            inv.setItem(46 + i * 2, GuiUtil.createFillerItem(JOB_GLASS[i]));
        }

        // Fermer
        inv.setItem(49, GuiUtil.createItem(Material.BARRIER, "§cFermer"));

        return inv;
    }

    // ── Icône principale du métier ────────────────────────────────────────────
    private ItemStack buildJobIcon(JobType job, PlayerJobData data) {
        String color = job.getColor();
        String level = data.isMaxLevel() ? "§5§lMAX" : "§fNiveau §e§l" + data.getLevel();
        String xpInfo = data.isMaxLevel() ? "§5✦ Niveau maximum atteint!" :
            "§7XP: §a" + data.getXp() + " §7/ §e" + PlayerJobData.xpToNextLevel(data.getLevel());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(job.getDescription());
        lore.add("");
        lore.add("  " + level);
        lore.add("  " + xpInfo);
        lore.add("");
        lore.add("  " + data.progressBar());
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§e§l▶ §eCliquez pour les détails");

        ItemStack item = new ItemStack(job.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "§l✦ " + job.getDisplayName().toUpperCase() + " §r" + color + "§l✦");
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(MessageUtil.colorize(l));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Barre de progression XP ───────────────────────────────────────────────
    private ItemStack buildXpBar(JobType job, PlayerJobData data) {
        String color = job.getColor();
        double pct = data.progressPercent();
        int filled = (int) Math.round(pct * 10);
        String bar = "§a" + "█".repeat(filled) + "§8" + "█".repeat(10 - filled);

        String xpNeeded = data.isMaxLevel() ? "§5Maximum atteint!" :
            "§7Il manque §e" + data.xpNeeded() + " XP §7pour le prochain niveau";

        return GuiUtil.createItem(Material.EXPERIENCE_BOTTLE,
            color + "Progression — Niv. " + data.getLevel(),
            "  " + bar + " §f" + (int)(pct * 100) + "%",
            "",
            xpNeeded,
            "§7XP total gagné: §a" + String.format("%,d", data.getTotalXpEarned())
        );
    }

    // ── Statistiques ─────────────────────────────────────────────────────────
    private ItemStack buildStats(JobType job, PlayerJobData data) {
        String color = job.getColor();
        String nextMilestone = getNextMilestone(data.getLevel());

        return GuiUtil.createItem(Material.BOOK,
            color + "Statistiques",
            "§7Niveau actuel: " + color + "§l" + data.getLevel() + " §8/ §7" + PlayerJobData.MAX_LEVEL,
            "§7Coins gagnés via ce métier: §6" + String.format("%,d", data.getCoinsEarned()),
            "§7XP total accumulé: §a" + String.format("%,d", data.getTotalXpEarned()),
            "",
            "§7Prochain palier: " + color + nextMilestone
        );
    }

    // ── Récompenses ───────────────────────────────────────────────────────────
    private ItemStack buildRewards(JobType job, PlayerJobData data) {
        String color = job.getColor();
        int nextLvl = Math.min(data.getLevel() + 1, PlayerJobData.MAX_LEVEL);
        int nextReward = PlayerJobData.coinsRewardForLevel(nextLvl);

        return GuiUtil.createItem(Material.GOLD_INGOT,
            color + "Récompenses",
            "§7Prochain niveau §e(§fNiv. " + nextLvl + "§e)§7:",
            "  §6+" + String.format("%,d", nextReward) + " coins",
            "",
            "§8Paliers spéciaux:",
            "  §eNiveau  5 §7→ §6+500 bonus",
            "  §eNiveau 10 §7→ §6+2 000 bonus",
            "  §eNiveau 25 §7→ §6+10 000 bonus",
            "  §5Niveau 50 §7→ §5+50 000 bonus (MAX)"
        );
    }

    private String getNextMilestone(int currentLevel) {
        if (currentLevel < 5)  return "§eNiveau 5";
        if (currentLevel < 10) return "§eNiveau 10";
        if (currentLevel < 25) return "§eNiveau 25";
        if (currentLevel < 50) return "§5Niveau 50 (MAX)";
        return "§5✦ Maximum atteint!";
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        // Clic sur une icône de métier → ouvrir le détail
        for (int i = 0; i < JOBS.length; i++) {
            if (slot == JOB_ICON_SLOTS[i] || slot == JOB_BAR_SLOTS[i]
                    || slot == JOB_STAT_SLOTS[i] || slot == JOB_RWD_SLOTS[i]) {
                new JobDetailGui(plugin, player, JOBS[i]).open();
                return;
            }
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
