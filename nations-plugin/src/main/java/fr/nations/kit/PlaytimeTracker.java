package fr.nations.kit;

import fr.nations.NationsPlugin;
import fr.nations.grade.GradeType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlaytimeTracker implements Listener {

    private final NationsPlugin plugin;
    private final File dataFile;
    private YamlConfiguration cfg;

    private final Map<UUID, Long> joinTimes  = new HashMap<>();
    private final Map<UUID, Long> accumulated = new HashMap<>();
    private final Map<UUID, Map<GradeType, Long>> kitClaims = new HashMap<>();

    public PlaytimeTracker(NationsPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "kit_playtime.yml");
        loadFile();
        for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
            handleJoin(p.getUniqueId());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Events
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handleJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        flushSession(uuid);
        savePlayer(uuid);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    public long getPlaytimeMillis(UUID uuid) {
        long acc    = accumulated.getOrDefault(uuid, 0L);
        Long joined = joinTimes.get(uuid);
        return acc + (joined != null ? System.currentTimeMillis() - joined : 0L);
    }

    public long getKitClaimPlaytime(UUID uuid, GradeType kit) {
        Map<GradeType, Long> map = kitClaims.get(uuid);
        return (map != null) ? map.getOrDefault(kit, -1L) : -1L;
    }

    public void setKitClaimPlaytime(UUID uuid, GradeType kit, long playtimeMs) {
        kitClaims.computeIfAbsent(uuid, k -> new EnumMap<>(GradeType.class))
                 .put(kit, playtimeMs);
        savePlayer(uuid);
    }

    public void saveAll() {
        for (UUID uuid : new HashSet<>(joinTimes.keySet())) {
            flushSession(uuid);
            savePlayer(uuid);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal
    // ─────────────────────────────────────────────────────────────────────────

    private void handleJoin(UUID uuid) {
        joinTimes.put(uuid, System.currentTimeMillis());
        loadPlayer(uuid);
    }

    private void flushSession(UUID uuid) {
        Long joined = joinTimes.remove(uuid);
        if (joined == null) return;
        accumulated.merge(uuid, System.currentTimeMillis() - joined, Long::sum);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Persistence
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFile() {
        cfg = dataFile.exists()
            ? YamlConfiguration.loadConfiguration(dataFile)
            : new YamlConfiguration();
    }

    private void loadPlayer(UUID uuid) {
        String key = uuid.toString();
        accumulated.put(uuid, cfg.getLong(key + ".playtime", 0L));
        Map<GradeType, Long> claims = new EnumMap<>(GradeType.class);
        for (GradeType grade : List.of(GradeType.HEROS, GradeType.CHEVALIER, GradeType.PREMIUM)) {
            long val = cfg.getLong(key + ".kits." + grade.name(), -1L);
            if (val >= 0) claims.put(grade, val);
        }
        kitClaims.put(uuid, claims);
    }

    private void savePlayer(UUID uuid) {
        String key = uuid.toString();
        cfg.set(key + ".playtime", accumulated.getOrDefault(uuid, 0L));
        Map<GradeType, Long> claims = kitClaims.get(uuid);
        if (claims != null) {
            for (Map.Entry<GradeType, Long> e : claims.entrySet()) {
                cfg.set(key + ".kits." + e.getKey().name(), e.getValue());
            }
        }
        try {
            if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Impossible de sauvegarder kit_playtime.yml : " + ex.getMessage());
        }
    }
}
