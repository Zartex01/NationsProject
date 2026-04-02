package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.jobs.JobManager;
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

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  GUI principal des Métiers  —  6 rangées × 9 colonnes   ║
 * ║                                                          ║
 * ║  Rangée 0 : header bleu (titre centré)                   ║
 * ║  Rangée 1 : icônes des métiers (slots 10,12,14,16)        ║
 * ║  Rangée 2 : barres XP         (slots 19,21,23,25)        ║
 * ║  Rangée 3 : statistiques      (slots 28,30,32,34)        ║
 * ║  Rangée 4 : boutons action    (slots 37,39,41,43)        ║
 * ║  Rangée 5 : footer bleu (fermer slot 49)                 ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class JobsGui {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int[] ICON_SLOTS   = {10, 12, 14, 16};
    private static final int[] BAR_SLOTS    = {19, 21, 23, 25};
    private static final int[] STAT_SLOTS   = {28, 30, 32, 34};
    private static final int[] ACTION_SLOTS = {37, 39, 41, 43};

    private static final JobType[] JOBS = JobType.values();

    // Verre coloré par métier
    private static final Material[] JOB_GLASS = {
        Material.GRAY_STAINED_GLASS_PANE,   // Mineur
        Material.LIME_STAINED_GLASS_PANE,   // Farmeur
        Material.RED_STAINED_GLASS_PANE,    // Chasseur
        Material.ORANGE_STAINED_GLASS_PANE  // Bûcheron
    };

    // Béton coloré pour les boutons action
    private static final Material[] JOB_CONCRETE = {
        Material.GRAY_CONCRETE,   // Mineur
        Material.LIME_CONCRETE,   // Farmeur
        Material.RED_CONCRETE,    // Chasseur
        Material.ORANGE_CONCRETE  // Bûcheron
    };

    private final NationsPlugin plugin;
    private final Player player;

    public JobsGui(NationsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    // ─── Construction ─────────────────────────────────────────────────────────

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("§8✦ §6§lSYSTÈME DE MÉTIERS §8✦", 6);

        Map<JobType, PlayerJobData> allData = plugin.getJobManager().getAllData(player.getUniqueId());
        int activeCount = (int) allData.values().stream().filter(PlayerJobData::isActive).count();

        // ── Header (rangée 0) ────────────────────────────────────────────────
        Material hdr = Material.BLUE_STAINED_GLASS_PANE;
        for (int i = 0; i < 9; i++) inv.setItem(i, GuiUtil.createFillerItem(hdr));
        inv.setItem(4, buildTitleItem(activeCount));

        // ── Footer (rangée 5) ────────────────────────────────────────────────
        for (int i = 45; i < 54; i++) inv.setItem(i, GuiUtil.createFillerItem(hdr));
        inv.setItem(49, GuiUtil.createItem(Material.BARRIER,
            "§c§lFermer",
            "§7Fermez le menu des métiers."));

        // ── Colonnes de séparation (col 0, 2, 4, 6, 8 des rangées 1-4) ──────
        for (int row = 1; row <= 4; row++) {
            for (int col : new int[]{2, 4, 6}) {
                inv.setItem(row * 9 + col, GuiUtil.createFillerItem(Material.BLACK_STAINED_GLASS_PANE));
            }
        }

        // ── Cartes de métiers ─────────────────────────────────────────────────
        for (int i = 0; i < JOBS.length; i++) {
            JobType   job  = JOBS[i];
            PlayerJobData data = allData.get(job);

            // Bordures gauche (col 0) et droite (col 8) de la colonne du métier
            // col 0 = GRAY pour Mineur, col 8 = ORANGE pour Bûcheron
            if (i == 0) {
                for (int row = 1; row <= 4; row++)
                    inv.setItem(row * 9, GuiUtil.createFillerItem(JOB_GLASS[0]));
            }
            if (i == JOBS.length - 1) {
                for (int row = 1; row <= 4; row++)
                    inv.setItem(row * 9 + 8, GuiUtil.createFillerItem(JOB_GLASS[JOBS.length - 1]));
            }

            // Couleur du job dans le footer et le header (décoration)
            inv.setItem(1 + i * 2, GuiUtil.createFillerItem(JOB_GLASS[i]));
            inv.setItem(47 + i * 2, GuiUtil.createFillerItem(JOB_GLASS[i]));

            // Items des 4 rangées
            inv.setItem(ICON_SLOTS[i],   buildIconItem(job, data));
            inv.setItem(BAR_SLOTS[i],    buildBarItem(job, data));
            inv.setItem(STAT_SLOTS[i],   buildStatItem(job, data));
            inv.setItem(ACTION_SLOTS[i], buildActionButton(job, data, activeCount, i));
        }

        return inv;
    }

    // ── Item de titre ─────────────────────────────────────────────────────────
    private ItemStack buildTitleItem(int activeCount) {
        String slots = activeCount + " §8/ §7" + JobManager.MAX_ACTIVE_JOBS;
        return GuiUtil.createItem(Material.NETHER_STAR,
            "§6§l✦ MÉTIERS §6§l✦",
            "§7Progressez dans vos métiers pour",
            "§7gagner des coins et de l'XP!",
            "",
            "§7Métiers actifs: §e" + slots,
            "§8(Max " + JobManager.MAX_ACTIVE_JOBS + " métiers simultanément)",
            "",
            "§eCliquez §7sur un métier pour plus de détails."
        );
    }

    // ── Icône principale du métier ────────────────────────────────────────────
    private ItemStack buildIconItem(JobType job, PlayerJobData data) {
        String color  = job.getColor();
        int    level  = data.getLevel();
        String tier   = data.getTierName();
        String stars  = data.getTierStars();
        boolean active = data.isActive();

        String statusBadge = active
            ? "§a§l◉ ACTIF"
            : "§8◎ Inactif";

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(job.getDescLine1());
        lore.add(job.getDescLine2());
        lore.add("");
        lore.add("  §7Niveau:  " + color + "§l" + level + " §8/ §750");
        lore.add("  §7Rang:    " + tier);
        lore.add("  §7Prestige: " + stars);
        lore.add("  §7Statut:  " + statusBadge);
        if (!data.isMaxLevel()) {
            lore.add("");
            lore.add("  §7XP: §a" + String.format("%,d", data.getXp())
                + " §8/ §e" + String.format("%,d", PlayerJobData.xpToNextLevel(level)));
        } else {
            lore.add("");
            lore.add("  §5✦ Niveau maximum atteint!");
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§e§l▶ §eCliquez pour les détails");

        ItemStack item = new ItemStack(job.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(color + "§l« " + job.getDisplayName().toUpperCase() + " »"));
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(MessageUtil.colorize(l));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Barre XP ──────────────────────────────────────────────────────────────
    private ItemStack buildBarItem(JobType job, PlayerJobData data) {
        String color = job.getColor();
        int    pct   = (int)(data.progressPercent() * 100);
        String bar   = data.progressBar(12, color);
        String mult  = String.format("%.2f", data.getXpMultiplier());

        String xpLine = data.isMaxLevel()
            ? "§5§l✦ XP Maximum!"
            : "§7XP: §a" + String.format("%,d", data.getXp())
              + " §8/ §e" + String.format("%,d", PlayerJobData.xpToNextLevel(data.getLevel()));

        return GuiUtil.createItem(Material.EXPERIENCE_BOTTLE,
            color + "Progression — Niv. " + data.getLevel(),
            "",
            "  " + bar + " §f" + pct + "%",
            "",
            xpLine,
            "§7Multiplicateur: §b×" + mult,
            data.isMaxLevel() ? "" : "§7Il manque §e" + String.format("%,d", data.xpNeeded()) + " XP"
        );
    }

    // ── Statistiques ──────────────────────────────────────────────────────────
    private ItemStack buildStatItem(JobType job, PlayerJobData data) {
        String color = job.getColor();
        String actionLabel = switch (job) {
            case MINEUR   -> "Blocs minés";
            case FARMEUR  -> "Récoltes";
            case CHASSEUR -> "Mobs tués";
            case BUCHERON -> "Bûches coupées";
        };
        String nextMilestone = getNextMilestoneLabel(data.getLevel());

        return GuiUtil.createItem(Material.BOOK,
            color + "Statistiques",
            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
            "§7" + actionLabel + ": §f" + String.format("%,d", data.getActionsCount()),
            "§7XP total: §a" + String.format("%,d", data.getTotalXpEarned()),
            "§7Coins gagnés: §6" + String.format("%,d", data.getCoinsEarned()),
            "",
            "§7Prochain palier: " + nextMilestone,
            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        );
    }

    // ── Bouton Rejoindre / Quitter / Complet ──────────────────────────────────
    private ItemStack buildActionButton(JobType job, PlayerJobData data,
                                        int activeCount, int jobIndex) {
        String color = job.getColor();

        if (data.isActive()) {
            // ── QUITTER ────────────────────────────────────────────────────
            return GuiUtil.createItem(Material.RED_CONCRETE,
                "§c§l✖  QUITTER  — " + job.getColoredName(),
                "§7Cliquez pour quitter ce métier.",
                "",
                "§7Votre progression sera conservée.",
                "§cVous ne gagnerez plus d'XP pour ce métier."
            );
        } else if (activeCount >= JobManager.MAX_ACTIVE_JOBS) {
            // ── PLEIN ──────────────────────────────────────────────────────
            return GuiUtil.createItem(Material.BARRIER,
                "§c§l⚠  MÉTIERS COMPLETS",
                "§7Vous avez atteint la limite de",
                "§c" + JobManager.MAX_ACTIVE_JOBS + " §7métiers actifs simultanément.",
                "",
                "§7Quittez un métier pour pouvoir",
                "§7rejoindre " + color + job.getDisplayName() + "§7."
            );
        } else {
            // ── REJOINDRE ──────────────────────────────────────────────────
            Material btnMat = JOB_CONCRETE[jobIndex];
            return GuiUtil.createItem(btnMat,
                "§a§l►  REJOINDRE  — " + job.getColoredName(),
                "§7Rejoignez ce métier et commencez",
                "§7à gagner de l'XP dès maintenant!",
                "",
                job.getDescLine1(),
                job.getDescLine2()
            );
        }
    }

    private String getNextMilestoneLabel(int level) {
        if (level < 5)  return "§eNiveau 5 — §6+300 bonus";
        if (level < 10) return "§eNiveau 10 — §6+1 000 bonus";
        if (level < 15) return "§eNiveau 15 — §6+1 500 bonus";
        if (level < 20) return "§eNiveau 20 — §6+3 000 bonus";
        if (level < 25) return "§eNiveau 25 — §6+5 000 bonus";
        if (level < 30) return "§eNiveau 30 — §6+10 000 bonus";
        if (level < 40) return "§eNiveau 40 — §6+25 000 bonus";
        if (level < 50) return "§5Niveau 50 — §5+75 000 bonus (MAX)";
        return "§5✦ Maximum atteint!";
    }

    // ─── Gestion des clics ────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Fermer
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        for (int i = 0; i < JOBS.length; i++) {
            JobType job = JOBS[i];

            // Clic icône / barre / stats → ouvrir le détail
            if (slot == ICON_SLOTS[i] || slot == BAR_SLOTS[i] || slot == STAT_SLOTS[i]) {
                new JobDetailGui(plugin, player, job).open();
                return;
            }

            // Clic bouton action → rejoindre ou quitter
            if (slot == ACTION_SLOTS[i]) {
                handleActionClick(job);
                return;
            }
        }
    }

    private void handleActionClick(JobType job) {
        JobManager jm = plugin.getJobManager();
        PlayerJobData data = jm.getData(player.getUniqueId(), job);
        int activeCount = jm.getActiveCount(player.getUniqueId());

        if (data.isActive()) {
            jm.leaveJob(player, job);
        } else if (activeCount >= JobManager.MAX_ACTIVE_JOBS) {
            player.sendMessage("§c§l[Métiers] §cVous avez déjà "
                + JobManager.MAX_ACTIVE_JOBS + " métiers actifs. Quittez-en un d'abord!");
        } else {
            jm.joinJob(player, job);
        }
        // Rafraîchir le GUI après l'action
        open();
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
