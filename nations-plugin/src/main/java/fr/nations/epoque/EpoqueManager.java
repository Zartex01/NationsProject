package fr.nations.epoque;

import fr.nations.NationsPlugin;
import fr.nations.gui.EpoqueGui;
import fr.nations.gui.GuiManager;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EpoqueManager {

    private final NationsPlugin plugin;
    private final File configFile;
    private final File dataFile;

    private final List<EpoqueLevel>                        levels   = new ArrayList<>();
    private final Map<UUID, EpoqueNationProgress>          progress = new HashMap<>();

    public EpoqueManager(NationsPlugin plugin) {
        this.plugin     = plugin;
        this.configFile = new File(plugin.getDataFolder(), "epoque.yml");
        this.dataFile   = new File(plugin.getDataFolder(), "epoque_data.yml");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Init
    // ─────────────────────────────────────────────────────────────────────────

    public void load() {
        loadConfig();
        loadData();
        startTicker();
    }

    private void loadConfig() {
        levels.clear();
        if (!configFile.exists()) {
            plugin.saveResource("epoque.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        InputStream defStream = plugin.getResource("epoque.yml");
        if (defStream != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(
                new InputStreamReader(defStream, StandardCharsets.UTF_8)));
        }

        ConfigurationSection sec = cfg.getConfigurationSection("levels");
        if (sec == null) return;
        List<String> keys = new ArrayList<>(sec.getKeys(false));
        keys.sort(Comparator.comparingInt(Integer::parseInt));

        for (String key : keys) {
            int num  = Integer.parseInt(key);
            String name = sec.getString(key + ".name", "Niveau " + num);
            int dur  = sec.getInt(key + ".duration-minutes", 15 * num);
            EpoqueLevel level = new EpoqueLevel(num, name, dur);

            List<Map<?, ?>> condList = sec.getMapList(key + ".conditions");
            for (Map<?, ?> map : condList) {
                try {
                    EpoqueCondition.Type type = EpoqueCondition.Type.valueOf(
                        String.valueOf(map.get("type")).toUpperCase());
                    double value = Double.parseDouble(String.valueOf(map.get("value")));
                    String desc  = map.containsKey("description")
                        ? String.valueOf(map.get("description")) : type.getLabel();
                    level.addCondition(new EpoqueCondition(type, value, desc));
                } catch (Exception e) {
                    plugin.getLogger().warning("[Époque] Condition invalide dans niveau " + num);
                }
            }

            List<Map<?, ?>> rewList = sec.getMapList(key + ".rewards");
            for (Map<?, ?> map : rewList) {
                try {
                    EpoqueReward.Type type = EpoqueReward.Type.valueOf(
                        String.valueOf(map.get("type")).toUpperCase());
                    double value = Double.parseDouble(String.valueOf(map.get("value")));
                    String desc  = map.containsKey("description")
                        ? String.valueOf(map.get("description")) : type.getLabel();
                    level.addReward(new EpoqueReward(type, value, desc));
                } catch (Exception e) {
                    plugin.getLogger().warning("[Époque] Récompense invalide dans niveau " + num);
                }
            }
            levels.add(level);
        }
        plugin.getLogger().info("[Époque] " + levels.size() + " niveaux chargés.");
    }

    private void loadData() {
        progress.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("nations");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID nationId = UUID.fromString(key);
                int level     = sec.getInt(key + ".level", 1);
                long resEnd   = sec.getLong(key + ".research-end", 0);
                progress.put(nationId, new EpoqueNationProgress(nationId, level, resEnd));
            } catch (Exception e) {
                plugin.getLogger().warning("[Époque] Entrée corrompue: " + key);
            }
        }
    }

    public void saveData() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (EpoqueNationProgress p : progress.values()) {
            String key = "nations." + p.getNationId();
            cfg.set(key + ".level", p.getCurrentLevel());
            cfg.set(key + ".research-end", p.getResearchEndTime());
        }
        try {
            if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Époque] Impossible de sauvegarder: " + e.getMessage());
        }
    }

    public void saveConfig() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (EpoqueLevel level : levels) {
            String base = "levels." + level.getNumber();
            cfg.set(base + ".name", level.getName());
            cfg.set(base + ".duration-minutes", level.getDurationMinutes());

            List<Map<String, Object>> condList = new ArrayList<>();
            for (EpoqueCondition c : level.getConditions()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("type", c.getType().name());
                map.put("value", c.getValue());
                map.put("description", c.getDescription());
                condList.add(map);
            }
            cfg.set(base + ".conditions", condList);

            List<Map<String, Object>> rewList = new ArrayList<>();
            for (EpoqueReward r : level.getRewards()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("type", r.getType().name());
                map.put("value", r.getValue());
                map.put("description", r.getDescription());
                rewList.add(map);
            }
            cfg.set(base + ".rewards", rewList);
        }
        try {
            cfg.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Époque] Impossible de sauvegarder la config: " + e.getMessage());
        }
    }

    public void reload() {
        loadConfig();
        loadData();
        plugin.getLogger().info("[Époque] Config rechargée.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Ticker
    // ─────────────────────────────────────────────────────────────────────────

    private void startTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tickResearch();
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    private void tickResearch() {
        for (EpoqueNationProgress p : new ArrayList<>(progress.values())) {
            if (!p.isResearchComplete()) continue;

            Nation nation = plugin.getNationManager().getNationById(p.getNationId());
            if (nation == null) continue;

            EpoqueLevel level = getLevel(p.getCurrentLevel());
            if (level != null) {
                for (EpoqueReward reward : level.getRewards()) {
                    reward.apply(nation, plugin);
                }
                broadcastToNation(nation,
                    "&6&l[Époque] &aNiveau &e" + level.getName()
                    + " &acomplété ! Vos récompenses ont été distribuées !");
            }

            p.setResearchEndTime(0);
            p.setCurrentLevel(p.getCurrentLevel() + 1);
            saveData();

            refreshOpenGuis(nation);
        }
    }

    private void broadcastToNation(Nation nation, String msg) {
        for (UUID uid : nation.getMembers().keySet()) {
            Player p = plugin.getServer().getPlayer(uid);
            if (p != null) MessageUtil.send(p, msg);
        }
    }

    private void refreshOpenGuis(Nation nation) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (nation.isMember(p.getUniqueId())) {
                Object gui = GuiManager.getOpenGui(p.getUniqueId());
                if (gui instanceof EpoqueGui) {
                    new EpoqueGui(plugin, p, nation).open();
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  State management
    // ─────────────────────────────────────────────────────────────────────────

    public EpoqueNationProgress getProgress(UUID nationId) {
        return progress.computeIfAbsent(nationId,
            id -> new EpoqueNationProgress(id, 1, 0));
    }

    public boolean startResearch(Nation nation) {
        EpoqueNationProgress p = getProgress(nation.getId());
        if (p.isResearching()) return false;

        EpoqueLevel level = getLevel(p.getCurrentLevel());
        if (level == null) return false;

        for (EpoqueCondition cond : level.getConditions()) {
            if (!cond.check(nation, plugin)) return false;
        }

        p.setResearchEndTime(System.currentTimeMillis() + level.getDurationMillis());
        saveData();
        return true;
    }

    public boolean allLevelsCompleted(UUID nationId) {
        EpoqueNationProgress p = getProgress(nationId);
        return p.getCurrentLevel() > levels.size();
    }

    public EpoqueLevel getLevel(int number) {
        return levels.stream()
            .filter(l -> l.getNumber() == number)
            .findFirst()
            .orElse(null);
    }

    public List<EpoqueLevel> getLevels() { return Collections.unmodifiableList(levels); }

    public int getTotalLevels() { return levels.size(); }

    public void addLevel(EpoqueLevel level) {
        levels.add(level);
        saveConfig();
    }

    public void setNationLevel(UUID nationId, int level) {
        EpoqueNationProgress p = getProgress(nationId);
        p.setCurrentLevel(level);
        p.setResearchEndTime(0);
        saveData();
    }
}
