package fr.nations.jobs;

import fr.nations.NationsPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class JobListener implements Listener {

    private final NationsPlugin plugin;

    /** XP Chasseur par type de mob */
    private static final Map<EntityType, Integer> MOB_XP = new EnumMap<>(EntityType.class);

    static {
        // ── Animaux paisibles ───────────────────────────────────────────────
        MOB_XP.put(EntityType.CHICKEN,     3);
        MOB_XP.put(EntityType.RABBIT,      3);
        MOB_XP.put(EntityType.PIG,         5);
        MOB_XP.put(EntityType.SHEEP,       5);
        MOB_XP.put(EntityType.COW,         5);
        MOB_XP.put(EntityType.MOOSHROOM,   6);
        MOB_XP.put(EntityType.HORSE,       5);
        MOB_XP.put(EntityType.DONKEY,      5);
        MOB_XP.put(EntityType.LLAMA,       5);
        MOB_XP.put(EntityType.FOX,         8);
        MOB_XP.put(EntityType.WOLF,        8);
        MOB_XP.put(EntityType.POLAR_BEAR, 10);
        MOB_XP.put(EntityType.GOAT,        6);
        MOB_XP.put(EntityType.AXOLOTL,     4);
        // ── Monstres communs ────────────────────────────────────────────────
        MOB_XP.put(EntityType.ZOMBIE,     10);
        MOB_XP.put(EntityType.SKELETON,   10);
        MOB_XP.put(EntityType.SPIDER,      8);
        MOB_XP.put(EntityType.CAVE_SPIDER,10);
        MOB_XP.put(EntityType.CREEPER,    15);
        MOB_XP.put(EntityType.ENDERMAN,   20);
        MOB_XP.put(EntityType.WITCH,      15);
        MOB_XP.put(EntityType.DROWNED,    10);
        MOB_XP.put(EntityType.HUSK,       10);
        MOB_XP.put(EntityType.STRAY,      10);
        MOB_XP.put(EntityType.PHANTOM,    12);
        MOB_XP.put(EntityType.SILVERFISH,  5);
        MOB_XP.put(EntityType.SLIME,       6);
        MOB_XP.put(EntityType.MAGMA_CUBE,  8);
        // ── Raid / Illageois ────────────────────────────────────────────────
        MOB_XP.put(EntityType.PILLAGER,   15);
        MOB_XP.put(EntityType.VINDICATOR, 18);
        MOB_XP.put(EntityType.RAVAGER,    35);
        MOB_XP.put(EntityType.EVOKER,     30);
        MOB_XP.put(EntityType.VEX,        12);
        // ── Nether ──────────────────────────────────────────────────────────
        MOB_XP.put(EntityType.BLAZE,       20);
        MOB_XP.put(EntityType.WITHER_SKELETON, 25);
        MOB_XP.put(EntityType.GHAST,       18);
        MOB_XP.put(EntityType.PIGLIN_BRUTE,22);
        MOB_XP.put(EntityType.HOGLIN,      15);
        MOB_XP.put(EntityType.ZOGLIN,      18);
        // ── Aquatique ───────────────────────────────────────────────────────
        MOB_XP.put(EntityType.GUARDIAN,    20);
        MOB_XP.put(EntityType.ELDER_GUARDIAN,50);
        // ── Boss ────────────────────────────────────────────────────────────
        MOB_XP.put(EntityType.WARDEN,     100);
        MOB_XP.put(EntityType.WITHER,     150);
        MOB_XP.put(EntityType.ENDER_DRAGON,200);
    }

    public JobListener(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Mineur + Bûcheron + Farmeur ─────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Block block = event.getBlock();
        Material mat = block.getType();

        JobManager jm = plugin.getJobManager();
        if (!jm.isLoaded(id)) jm.loadPlayer(id);

        // Mineur
        int minerXp = JobType.MINEUR.getXpForBlock(mat);
        if (minerXp > 0) {
            grantXp(player, id, JobType.MINEUR, minerXp);
            return;
        }

        // Bûcheron
        int woodXp = JobType.BUCHERON.getXpForBlock(mat);
        if (woodXp > 0) {
            grantXp(player, id, JobType.BUCHERON, woodXp);
            return;
        }

        // Farmeur (uniquement cultures matures)
        int farmXp = getFarmXp(block);
        if (farmXp > 0) {
            grantXp(player, id, JobType.FARMEUR, farmXp);
        }
    }

    private int getFarmXp(Block block) {
        Material mat = block.getType();
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) return 0;
        }
        return JobType.FARMEUR.getXpForBlock(mat);
    }

    // ─── Chasseur ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();
        UUID id = player.getUniqueId();

        JobManager jm = plugin.getJobManager();
        if (!jm.isLoaded(id)) jm.loadPlayer(id);

        EntityType type = event.getEntityType();
        int xp = MOB_XP.getOrDefault(type, 0);
        if (xp > 0) grantXp(player, id, JobType.CHASSEUR, xp);
    }

    // ─── Gestion XP + Level Up ───────────────────────────────────────────────

    private void grantXp(Player player, UUID playerId, JobType jobType, int rawXp) {
        JobManager jm = plugin.getJobManager();
        PlayerJobData data = jm.getData(playerId, jobType);

        // Ne rien accorder si le métier est inactif
        if (!data.isActive()) return;
        if (data.isMaxLevel()) return;

        int prevLevel  = data.getLevel();
        int finalXp    = jm.computeXp(playerId, jobType, rawXp);
        int levelsGained = jm.addXp(playerId, jobType, rawXp);

        // Action bar : affichage XP + multiplicateur si > 1
        showXpBar(player, jobType, data, finalXp);

        if (levelsGained > 0) {
            handleLevelUp(player, playerId, jobType, prevLevel, data.getLevel());
        }

        // Sauvegarde asynchrone
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> jm.savePlayer(playerId));
    }

    /** Affiche l'XP gagné dans la barre d'action du joueur */
    private void showXpBar(Player player, JobType jobType, PlayerJobData data, int xpGained) {
        String color = jobType.getColor();
        double mult  = data.getXpMultiplier();
        String multStr = mult > 1.0
            ? " §8(§6×" + String.format("%.2f", mult) + "§8)"
            : "";

        int pct = (int)(data.progressPercent() * 100);
        String bar = data.progressBar(10, color);

        String msg = color + "+" + xpGained + " XP §7" + jobType.getDisplayName()
            + multStr + "  " + bar + " §f" + pct + "%";

        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection().deserialize(msg));
    }

    private void handleLevelUp(Player player, UUID playerId,
                                JobType jobType, int oldLevel, int newLevel) {
        PlayerJobData data = plugin.getJobManager().getData(playerId, jobType);

        // Récompenses en coins
        long totalCoins = 0;
        for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
            totalCoins += PlayerJobData.coinsRewardForLevel(lvl);
        }
        data.addCoinsEarned(totalCoins);
        plugin.getEconomyManager().deposit(playerId, totalCoins);

        boolean milestone = PlayerJobData.isMilestone(newLevel);
        String color = jobType.getColor();
        String name  = jobType.getColoredName();
        String tier  = PlayerJobData.getTierName(newLevel);
        String stars = PlayerJobData.getTierStars(newLevel);

        // Effets sonores
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        if (milestone) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            spawnFireworks(player, jobType);
        }

        // Message de level-up
        player.sendMessage("");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("  §e§l✦ NIVEAU SUPÉRIEUR  ─  " + name);
        player.sendMessage("  " + color + "Métier " + jobType.getDisplayName()
            + " §f→ Niveau §e§l" + newLevel + " §8/ §7" + PlayerJobData.MAX_LEVEL);
        player.sendMessage("  §7Rang: " + tier + "  " + stars);
        player.sendMessage("  §a+" + String.format("%,d", totalCoins) + " coins §7de récompense!");
        if (milestone) {
            player.sendMessage("  §6§l★ PALIER ATTEINT§e – Bravo pour ce niveau de prestige!");
        }
        if (newLevel >= PlayerJobData.MAX_LEVEL) {
            player.sendMessage("  §5§l✦ NIVEAU MAXIMUM! Tu es " + name + " §5§lLégendaire!");
        }
        String nextMult = String.format("%.2f", PlayerJobData.getXpMultiplier(newLevel));
        player.sendMessage("  §7Multiplicateur XP: §b×" + nextMult);
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
    }

    private void spawnFireworks(Player player, JobType jobType) {
        Color primary = switch (jobType) {
            case MINEUR   -> Color.SILVER;
            case FARMEUR  -> Color.LIME;
            case CHASSEUR -> Color.RED;
            case BUCHERON -> Color.ORANGE;
        };
        Bukkit.getScheduler().runTask(plugin, () -> {
            Firework fw = player.getWorld().spawn(player.getLocation(), Firework.class);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(primary, Color.YELLOW)
                .withFade(Color.WHITE)
                .with(org.bukkit.FireworkEffect.Type.STAR)
                .trail(true).flicker(true).build());
            meta.setPower(0);
            fw.setFireworkMeta(meta);
        });
    }
}
