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

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  GUI de détail d'un métier  — 6 rangées × 9 colonnes    ║
 * ║                                                          ║
 * ║  Rangée 0 : header (couleur du métier)                   ║
 * ║  Rangée 1 : icône centrale (slot 13) + déco              ║
 * ║  Rangée 2 : barre de progression visuelle (slots 19-25)  ║
 * ║  Rangée 3 : 4 panneaux info (28, 30, 32, 34)             ║
 * ║  Rangée 4 : rembourrage décoratif                        ║
 * ║  Rangée 5 : bouton Retour (45), bouton Action (49)       ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class JobDetailGui {

    private final NationsPlugin plugin;
    private final Player player;
    private final JobType job;

    public JobDetailGui(NationsPlugin plugin, Player player, JobType job) {
        this.plugin = plugin;
        this.player = player;
        this.job    = job;
    }

    // ─── Construction ─────────────────────────────────────────────────────────

    public Inventory build() {
        PlayerJobData data  = plugin.getJobManager().getData(player.getUniqueId(), job);
        String color        = job.getColor();
        Material border     = getBorderMaterial();
        Material barFilled  = getFilledBarMaterial();

        String title = color + "§l◆  " + job.getDisplayName().toUpperCase() + "  ◆";
        Inventory inv = GuiUtil.createGui(title, 6);

        // ── Header & Footer ──────────────────────────────────────────────────
        for (int i = 0; i < 9; i++) {
            inv.setItem(i,      GuiUtil.createFillerItem(border));
            inv.setItem(45 + i, GuiUtil.createFillerItem(border));
        }

        // ── Colonnes gauche et droite (rangées 1-4) ──────────────────────────
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9,     GuiUtil.createFillerItem(border));
            inv.setItem(row * 9 + 8, GuiUtil.createFillerItem(border));
        }

        // ── Intérieur noir (rembourrage par défaut) ───────────────────────────
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                inv.setItem(row * 9 + col, GuiUtil.createFillerItem(Material.BLACK_STAINED_GLASS_PANE));
            }
        }

        // ── Rangée 1 : Icône centrale + coins de bordure ─────────────────────
        inv.setItem(10, GuiUtil.createFillerItem(border));
        inv.setItem(16, GuiUtil.createFillerItem(border));
        inv.setItem(13, buildMainIcon(data));

        // ── Rangée 2 : Barre de progression (7 slots) ────────────────────────
        buildProgressBar(inv, data, 19, barFilled);

        // ── Rangée 3 : Panneaux d'information ────────────────────────────────
        inv.setItem(27, GuiUtil.createFillerItem(border));
        inv.setItem(35, GuiUtil.createFillerItem(border));
        inv.setItem(28, buildActivitiesItem());
        inv.setItem(30, buildMilestonesItem(data));
        inv.setItem(32, buildStatsItem(data));
        inv.setItem(34, buildNextLevelItem(data));

        // ── Rangée 4 : décorations ────────────────────────────────────────────
        inv.setItem(37, GuiUtil.createFillerItem(border));
        inv.setItem(43, GuiUtil.createFillerItem(border));

        // ── Rangée 5 : boutons ───────────────────────────────────────────────
        inv.setItem(46, GuiUtil.createItem(Material.ARROW,
            "§7« Retour aux métiers",
            "§8Retournez au menu principal."));

        // Bouton central Rejoindre / Quitter
        inv.setItem(49, buildActionButton(data));

        return inv;
    }

    // ── Icône principale ──────────────────────────────────────────────────────
    private ItemStack buildMainIcon(PlayerJobData data) {
        String color  = job.getColor();
        int    level  = data.getLevel();
        String tier   = data.getTierName();
        String stars  = data.getTierStars();
        String mult   = String.format("%.2f", data.getXpMultiplier());
        String status = data.isActive() ? "§a§l◉ ACTIF" : "§8◎ Inactif";

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(job.getDescLine1());
        lore.add(job.getDescLine2());
        lore.add("");
        lore.add("  §7Niveau:      " + color + "§l" + level + " §8/ §750");
        lore.add("  §7Rang:         " + tier);
        lore.add("  §7Prestige:     " + stars);
        lore.add("  §7Statut:       " + status);
        lore.add("  §7Multiplicateur: §b×" + mult);
        lore.add("");
        if (data.isMaxLevel()) {
            lore.add("  §5✦ Niveau maximum atteint!");
        } else {
            lore.add("  §7XP: §a" + String.format("%,d", data.getXp())
                + " §8/ §e" + String.format("%,d", PlayerJobData.xpToNextLevel(level)));
            lore.add("  §7Manque: §c" + String.format("%,d", data.xpNeeded()) + " XP");
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        ItemStack item = new ItemStack(job.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(color + "§l✦ " + job.getDisplayName() + " §r" + color + "§l✦"));
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(MessageUtil.colorize(l));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Barre de progression visuelle sur 7 slots ─────────────────────────────
    private void buildProgressBar(Inventory inv, PlayerJobData data,
                                   int startSlot, Material filled) {
        double pct       = data.progressPercent();
        int    filledCnt = (int) Math.round(pct * 7);
        String color     = job.getColor();
        String pctStr    = data.isMaxLevel() ? "MAX" : (int)(pct * 100) + "%";

        for (int i = 0; i < 7; i++) {
            boolean isFilled = i < filledCnt;
            Material mat     = isFilled ? filled : Material.GRAY_STAINED_GLASS_PANE;
            String label;
            if (i == 3) {
                // Centre : affiche le pourcentage
                label = data.isMaxLevel()
                    ? "§5§l MAX "
                    : color + "§l" + pctStr;
            } else {
                label = isFilled ? color + "▐ " + pctStr : "§8░";
            }
            String loreLine = isFilled
                ? color + "Progression: §f" + pctStr
                : "§8░ Non complété";
            inv.setItem(startSlot + i, GuiUtil.createItem(mat, label, loreLine));
        }
    }

    // ── Activités et gains XP ─────────────────────────────────────────────────
    private ItemStack buildActivitiesItem() {
        String color = job.getColor();
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Gains d'XP par action:");
        lore.add("");

        switch (job) {
            case MINEUR -> {
                lore.add("  §fCharbon:       §a+5 XP");
                lore.add("  §fCuivre:        §a+8 XP");
                lore.add("  §fFer:           §a+10 XP");
                lore.add("  §fOr / Lapis:    §a+15 XP");
                lore.add("  §fRedstone:      §a+12 XP");
                lore.add("  §bDiamant:       §a+40 XP");
                lore.add("  §aÉmeraude:      §a+50 XP");
                lore.add("  §5Ancient Debris: §a+80 XP");
                lore.add("");
                lore.add("  §8(Version deepslate = même XP)");
            }
            case FARMEUR -> {
                lore.add("  §fBlé / Carotte / Patate: §a+5 XP");
                lore.add("  §fBetterave:      §a+5 XP");
                lore.add("  §fMelon / Citrouille: §a+10 XP");
                lore.add("  §fCanne à sucre:  §a+3 XP");
                lore.add("  §fCactus:         §a+3 XP");
                lore.add("  §fVerrue du nether: §a+8 XP");
                lore.add("  §fCacao:          §a+5 XP");
                lore.add("");
                lore.add("  §8(Uniquement cultures matures)");
            }
            case CHASSEUR -> {
                lore.add("  §fPoulet / Lapin: §a+3 XP");
                lore.add("  §fCochon / Mouton / Vache: §a+5 XP");
                lore.add("  §fLoup / Renard:  §a+8 XP");
                lore.add("  §fZombie / Squelette: §a+10 XP");
                lore.add("  §fCreeper / Sorcière: §a+15 XP");
                lore.add("  §fEnderman:       §a+20 XP");
                lore.add("  §fBlaze / Wither Skel.: §a+20-25 XP");
                lore.add("  §fRavageur / Évocateur: §a+30-35 XP");
                lore.add("  §cWarden:         §a+100 XP");
                lore.add("  §5Ender Dragon:   §a+200 XP");
            }
            case BUCHERON -> {
                lore.add("  §fChêne / Épicéa: §a+5 XP");
                lore.add("  §fBouleau / Jungle: §a+5 XP");
                lore.add("  §fAcacia / Chêne Noir: §a+5 XP");
                lore.add("  §fPalétuvier / Cerisier: §a+5 XP");
                lore.add("  §fBambou:         §a+2 XP");
                lore.add("  §fChampignon géant: §a+3 XP");
            }
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        return GuiUtil.createItem(Material.KNOWLEDGE_BOOK,
            color + "§lActivités & XP", lore.toArray(new String[0]));
    }

    // ── Paliers de récompenses ────────────────────────────────────────────────
    private ItemStack buildMilestonesItem(PlayerJobData data) {
        String color = job.getColor();
        int    level = data.getLevel();

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Paliers de prestige:");
        lore.add("");
        lore.add(ms(level,  5,  "§e+300 bonus",        "§eJournalier"));
        lore.add(ms(level, 10,  "§e+1 000 bonus",      "§eJournalier II"));
        lore.add(ms(level, 15,  "§e+1 500 bonus",      "§eJournalier III"));
        lore.add(ms(level, 20,  "§a+3 000 bonus",      "§aCompagnon"));
        lore.add(ms(level, 25,  "§a+5 000 bonus",      "§aCompagnon II"));
        lore.add(ms(level, 30,  "§b+10 000 bonus",     "§b§lExpert"));
        lore.add(ms(level, 40,  "§6+25 000 bonus",     "§6§lMaître"));
        lore.add(ms(level, 50,  "§5+75 000 bonus",     "§5§lGrand Maître ✦"));
        lore.add("");
        lore.add("§7Base niveau: §6+" + (data.getLevel() * 150) + " coins/niv.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        return GuiUtil.createItem(Material.GOLD_BLOCK,
            color + "§lPaliers & Récompenses", lore.toArray(new String[0]));
    }

    private String ms(int current, int target, String reward, String rankName) {
        if (current >= target) {
            return "  §a✔ §7Niv." + String.format("%3d", target)
                + " §8─ " + reward + " §8(" + rankName + "§8)";
        } else if (current == target - 1 || (current < target && target - current <= 3)) {
            return "  §e➤ §7Niv." + String.format("%3d", target)
                + " §8─ " + reward + " §8(" + rankName + "§8)";
        } else {
            return "  §8○ §8Niv." + String.format("%3d", target)
                + " §8─ " + reward;
        }
    }

    // ── Statistiques ──────────────────────────────────────────────────────────
    private ItemStack buildStatsItem(PlayerJobData data) {
        String color = job.getColor();
        String actionLabel = switch (job) {
            case MINEUR   -> "Blocs minés";
            case FARMEUR  -> "Cultures récoltées";
            case CHASSEUR -> "Créatures tuées";
            case BUCHERON -> "Bûches coupées";
        };
        double mult = data.getXpMultiplier();
        String multStr = String.format("%.2f", mult);
        boolean bonus = mult > 1.0;

        return GuiUtil.createItem(Material.PAPER,
            color + "§lMes Statistiques",
            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
            "§7Niveau: " + color + "§l" + data.getLevel() + " §8/ §750",
            "§7Rang:   " + data.getTierName() + "  " + data.getTierStars(),
            "",
            "§7" + actionLabel + ": §f" + String.format("%,d", data.getActionsCount()),
            "§7XP actuelle: §a" + String.format("%,d", data.getXp()),
            "§7XP total gagné: §a" + String.format("%,d", data.getTotalXpEarned()),
            "§7Coins gagnés: §6" + String.format("%,d", data.getCoinsEarned()),
            "",
            bonus
                ? "§7Bonus XP: §b×" + multStr + " §8(+§b" + (int)((mult - 1) * 100) + "%§8)"
                : "§7Bonus XP: §8aucun (niv. ≥10 requis)",
            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        );
    }

    // ── Prochain niveau ───────────────────────────────────────────────────────
    private ItemStack buildNextLevelItem(PlayerJobData data) {
        String color = job.getColor();

        if (data.isMaxLevel()) {
            return GuiUtil.createItem(Material.BEACON,
                "§5§l✦ NIVEAU MAXIMUM ✦",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Vous avez atteint le niveau",
                "§7maximum de ce métier!",
                "",
                "§5§l✦ Maître " + job.getDisplayName() + " Légendaire!",
                "§7Multiplicateur: §b×2.00",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
            );
        }

        int nextLvl  = data.getLevel() + 1;
        int reward   = PlayerJobData.coinsRewardForLevel(nextLvl);
        boolean ms   = PlayerJobData.isMilestone(nextLvl);
        String tierN = PlayerJobData.getTierName(nextLvl);
        String bar   = data.progressBar(10, color);
        double nMult = PlayerJobData.getXpMultiplier(nextLvl);

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("  " + bar + " §f" + (int)(data.progressPercent() * 100) + "%");
        lore.add("");
        lore.add("§7XP manquante: §e" + String.format("%,d", data.xpNeeded()));
        lore.add("§7Récompense:  §6+" + String.format("%,d", reward) + " coins");
        if (ms) {
            lore.add("§6§l★ Palier de prestige!");
            lore.add("§7Nouveau rang: " + tierN);
        }
        if (nMult > data.getXpMultiplier()) {
            lore.add("§7Nouveau multiplicateur: §b×" + String.format("%.2f", nMult));
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        ItemStack item = new ItemStack(ms ? Material.EMERALD_BLOCK : Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(color + "§lProchain Niveau — §e§l" + nextLvl));
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(MessageUtil.colorize(l));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Bouton Rejoindre / Quitter (slot 49) ──────────────────────────────────
    private ItemStack buildActionButton(PlayerJobData data) {
        JobManager jm = plugin.getJobManager();
        int activeCount = jm.getActiveCount(player.getUniqueId());

        if (data.isActive()) {
            return GuiUtil.createItem(Material.RED_CONCRETE,
                "§c§l✖  QUITTER " + job.getDisplayName().toUpperCase(),
                "§7Cliquez pour quitter ce métier.",
                "",
                "§7Votre progression est conservée.",
                "§cVous ne gagnerez plus d'XP."
            );
        } else if (activeCount >= JobManager.MAX_ACTIVE_JOBS) {
            return GuiUtil.createItem(Material.BARRIER,
                "§c§l⚠  MÉTIERS COMPLETS",
                "§7Limite de §c" + JobManager.MAX_ACTIVE_JOBS + " §7métiers atteinte.",
                "§7Quittez un métier pour rejoindre",
                "§7" + job.getColor() + job.getDisplayName() + "§7."
            );
        } else {
            return GuiUtil.createItem(getFilledBarMaterial(),
                "§a§l►  REJOINDRE " + job.getDisplayName().toUpperCase(),
                "§7Rejoignez ce métier maintenant!",
                "",
                job.getDescLine1(),
                job.getDescLine2()
            );
        }
    }

    // ── Helpers matériaux ─────────────────────────────────────────────────────
    private Material getBorderMaterial() {
        return switch (job) {
            case MINEUR   -> Material.GRAY_STAINED_GLASS_PANE;
            case FARMEUR  -> Material.LIME_STAINED_GLASS_PANE;
            case CHASSEUR -> Material.RED_STAINED_GLASS_PANE;
            case BUCHERON -> Material.ORANGE_STAINED_GLASS_PANE;
        };
    }

    private Material getFilledBarMaterial() {
        return switch (job) {
            case MINEUR   -> Material.GRAY_CONCRETE;
            case FARMEUR  -> Material.LIME_CONCRETE;
            case CHASSEUR -> Material.RED_CONCRETE;
            case BUCHERON -> Material.ORANGE_CONCRETE;
        };
    }

    // ─── Gestion des clics ────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Retour au menu principal
        if (slot == 46) {
            new JobsGui(plugin, player).open();
            return;
        }

        // Rejoindre / Quitter
        if (slot == 49) {
            handleActionClick();
        }
    }

    private void handleActionClick() {
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
        // Rafraîchir
        open();
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
