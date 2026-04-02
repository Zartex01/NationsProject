package fr.nations.jobs;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public class JobListener implements Listener {

    private final NationsPlugin plugin;

    // XP Chasseur par type de mob
    private static final java.util.Map<EntityType, Integer> MOB_XP = new java.util.EnumMap<>(EntityType.class);

    static {
        // Animaux paisibles
        MOB_XP.put(EntityType.CHICKEN, 3);
        MOB_XP.put(EntityType.RABBIT, 3);
        MOB_XP.put(EntityType.PIG, 5);
        MOB_XP.put(EntityType.SHEEP, 5);
        MOB_XP.put(EntityType.COW, 5);
        MOB_XP.put(EntityType.MUSHROOM_COW, 6);
        MOB_XP.put(EntityType.HORSE, 5);
        MOB_XP.put(EntityType.DONKEY, 5);
        MOB_XP.put(EntityType.LLAMA, 5);
        MOB_XP.put(EntityType.FOX, 8);
        MOB_XP.put(EntityType.WOLF, 8);
        MOB_XP.put(EntityType.POLAR_BEAR, 10);
        // Monstres
        MOB_XP.put(EntityType.ZOMBIE, 10);
        MOB_XP.put(EntityType.SKELETON, 10);
        MOB_XP.put(EntityType.SPIDER, 8);
        MOB_XP.put(EntityType.CAVE_SPIDER, 10);
        MOB_XP.put(EntityType.CREEPER, 15);
        MOB_XP.put(EntityType.ENDERMAN, 20);
        MOB_XP.put(EntityType.WITCH, 15);
        MOB_XP.put(EntityType.PILLAGER, 15);
        MOB_XP.put(EntityType.VINDICATOR, 18);
        MOB_XP.put(EntityType.RAVAGER, 30);
        MOB_XP.put(EntityType.EVOKER, 30);
        MOB_XP.put(EntityType.BLAZE, 20);
        MOB_XP.put(EntityType.WITHER_SKELETON, 22);
        MOB_XP.put(EntityType.GHAST, 18);
        MOB_XP.put(EntityType.PIGLIN_BRUTE, 20);
        MOB_XP.put(EntityType.DROWNED, 10);
        MOB_XP.put(EntityType.HUSK, 10);
        MOB_XP.put(EntityType.STRAY, 10);
        // Boss
        MOB_XP.put(EntityType.ELDER_GUARDIAN, 50);
        MOB_XP.put(EntityType.WARDEN, 100);
        MOB_XP.put(EntityType.WITHER, 150);
        MOB_XP.put(EntityType.ENDER_DRAGON, 200);
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

        // Farmeur (uniquement les cultures matures)
        int farmXp = getFarmXp(block);
        if (farmXp > 0) {
            grantXp(player, id, JobType.FARMEUR, farmXp);
        }
    }

    private int getFarmXp(Block block) {
        Material mat = block.getType();
        // Cultures avec âge → uniquement si matures
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
        EntityType type = event.getEntityType();
        int xp = MOB_XP.getOrDefault(type, 0);
        if (xp > 0) grantXp(player, id, JobType.CHASSEUR, xp);
    }

    // ─── Gestion XP + Level Up ────────────────────────────────────────────────

    private void grantXp(Player player, UUID playerId, JobType jobType, int xp) {
        JobManager jm = plugin.getJobManager();
        if (!jm.isLoaded(playerId)) jm.loadPlayer(playerId);

        PlayerJobData data = jm.getData(playerId, jobType);
        if (data.isMaxLevel()) return;

        int prevLevel = data.getLevel();
        int levelsGained = jm.addXp(playerId, jobType, xp);

        if (levelsGained > 0) {
            handleLevelUp(player, playerId, jobType, prevLevel, data.getLevel());
        }

        // Sauvegarde asynchrone toutes les N actions (optimisation)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> jm.savePlayer(playerId));
    }

    private void handleLevelUp(Player player, UUID playerId, JobType jobType, int oldLevel, int newLevel) {
        PlayerJobData data = plugin.getJobManager().getData(playerId, jobType);

        // Calculer et verser les coins de récompense
        long totalCoins = 0;
        for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
            int reward = PlayerJobData.coinsRewardForLevel(lvl);
            totalCoins += reward;
        }
        data.addCoinsEarned(totalCoins);
        plugin.getEconomyManager().deposit(playerId, totalCoins);

        // Effets visuels et sonores
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        if (isMilestone(newLevel)) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            spawnFireworks(player);
        }

        // Messages
        String color = jobType.getColor();
        String name = jobType.getColoredName();
        player.sendMessage("");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("  §e§l✦ NIVEAU SUPÉRIEUR — " + name);
        player.sendMessage("  " + color + "Métier " + jobType.getDisplayName() + " §f→ Niveau §e§l" + newLevel);
        player.sendMessage("  §a+" + totalCoins + " coins §7reçus en récompense!");
        if (isMilestone(newLevel)) {
            player.sendMessage("  §6§l★ PALIER ATTEINT! §eBravo pour ce niveau de prestige!");
        }
        if (newLevel >= PlayerJobData.MAX_LEVEL) {
            player.sendMessage("  §5§l✦ NIVEAU MAXIMUM ATTEINT! Tu es un maître " + jobType.getDisplayName() + "!");
        }
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
    }

    private boolean isMilestone(int level) {
        return level == 5 || level == 10 || level == 25 || level == 50;
    }

    private void spawnFireworks(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Firework fw = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Firework.class);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(Color.GOLD, Color.YELLOW)
                .withFade(Color.WHITE)
                .with(org.bukkit.FireworkEffect.Type.STAR)
                .trail(true).flicker(true).build());
            meta.setPower(0);
            fw.setFireworkMeta(meta);
        });
    }
}
