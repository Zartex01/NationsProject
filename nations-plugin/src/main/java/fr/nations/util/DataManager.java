package fr.nations.util;

import fr.nations.NationsPlugin;
import fr.nations.economy.PlayerAccount;
import fr.nations.grade.PlayerGrade;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.nation.NationRole;
import fr.nations.season.PlayerStats;
import fr.nations.territory.ClaimedChunk;
import fr.nations.war.War;
import fr.nations.war.WarStatus;
import fr.nations.war.WarType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class DataManager {

    private final NationsPlugin plugin;

    private File nationsFile;
    private File playersFile;
    private File claimsFile;
    private File warsFile;
    private File economyFile;
    private File seasonsFile;

    private FileConfiguration nationsConfig;
    private FileConfiguration playersConfig;
    private FileConfiguration claimsConfig;
    private FileConfiguration warsConfig;
    private FileConfiguration economyConfig;
    private FileConfiguration seasonsConfig;

    public DataManager(NationsPlugin plugin) {
        this.plugin = plugin;
        initFiles();
    }

    private void initFiles() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        nationsFile = new File(dataFolder, "nations.yml");
        playersFile = new File(dataFolder, "players.yml");
        claimsFile = new File(dataFolder, "claims.yml");
        warsFile = new File(dataFolder, "wars.yml");
        economyFile = new File(dataFolder, "economy.yml");
        seasonsFile = new File(dataFolder, "seasons.yml");

        nationsConfig = YamlConfiguration.loadConfiguration(nationsFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
        warsConfig = YamlConfiguration.loadConfiguration(warsFile);
        economyConfig = YamlConfiguration.loadConfiguration(economyFile);
        seasonsConfig = YamlConfiguration.loadConfiguration(seasonsFile);
    }

    public void loadAll() {
        loadNations();
        loadClaims();
        loadWars();
        loadEconomy();
        loadPlayers();
        loadSeasons();
    }

    public void saveAll() {
        saveNations();
        saveClaims();
        saveWars();
        saveEconomy();
        savePlayers();
        saveSeasons();
    }

    private void loadNations() {
        if (!nationsConfig.contains("nations")) return;
        for (String nationId : nationsConfig.getConfigurationSection("nations").getKeys(false)) {
            String path = "nations." + nationId;
            String name = nationsConfig.getString(path + ".name");
            UUID leaderId = UUID.fromString(nationsConfig.getString(path + ".leader"));
            String description = nationsConfig.getString(path + ".description", "");
            long createdAt = nationsConfig.getLong(path + ".created-at");
            double bankBalance = nationsConfig.getDouble(path + ".bank-balance", 0);
            int seasonPoints = nationsConfig.getInt(path + ".season-points", 0);
            boolean isOpen = nationsConfig.getBoolean(path + ".open", false);

            Nation nation = new Nation(UUID.fromString(nationId), name, leaderId, createdAt);
            nation.setDescription(description);
            nation.setBankBalance(bankBalance);
            nation.setSeasonPoints(seasonPoints);
            nation.setOpen(isOpen);

            if (nationsConfig.contains(path + ".members")) {
                for (String memberUUID : nationsConfig.getConfigurationSection(path + ".members").getKeys(false)) {
                    String roleName = nationsConfig.getString(path + ".members." + memberUUID + ".role", "MEMBER");
                    String memberName = nationsConfig.getString(path + ".members." + memberUUID + ".name", "Unknown");
                    NationRole role = NationRole.valueOf(roleName);
                    NationMember member = new NationMember(UUID.fromString(memberUUID), memberName, role);
                    nation.addMember(member);
                }
            }

            if (nationsConfig.contains(path + ".allies")) {
                for (String allyId : nationsConfig.getStringList(path + ".allies")) {
                    nation.addAlly(UUID.fromString(allyId));
                }
            }

            if (nationsConfig.contains(path + ".coalition")) {
                String coalitionId = nationsConfig.getString(path + ".coalition");
                if (coalitionId != null && !coalitionId.isEmpty()) {
                    nation.setCoalitionId(UUID.fromString(coalitionId));
                }
            }

            plugin.getNationManager().addNation(nation);
        }
    }

    public void saveNations() {
        nationsConfig = new YamlConfiguration();
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            String path = "nations." + nation.getId();
            nationsConfig.set(path + ".name", nation.getName());
            nationsConfig.set(path + ".leader", nation.getLeaderId().toString());
            nationsConfig.set(path + ".description", nation.getDescription());
            nationsConfig.set(path + ".created-at", nation.getCreatedAt());
            nationsConfig.set(path + ".bank-balance", nation.getBankBalance());
            nationsConfig.set(path + ".season-points", nation.getSeasonPoints());
            nationsConfig.set(path + ".open", nation.isOpen());

            for (NationMember member : nation.getMembers().values()) {
                String mPath = path + ".members." + member.getPlayerId();
                nationsConfig.set(mPath + ".role", member.getRole().name());
                nationsConfig.set(mPath + ".name", member.getPlayerName());
            }

            List<String> allyIds = new ArrayList<>();
            for (UUID allyId : nation.getAllies()) {
                allyIds.add(allyId.toString());
            }
            nationsConfig.set(path + ".allies", allyIds);

            if (nation.getCoalitionId() != null) {
                nationsConfig.set(path + ".coalition", nation.getCoalitionId().toString());
            }
        }
        saveFile(nationsConfig, nationsFile);
    }

    private void loadClaims() {
        if (!claimsConfig.contains("claims")) return;
        for (String key : claimsConfig.getConfigurationSection("claims").getKeys(false)) {
            String path = "claims." + key;
            String worldName = claimsConfig.getString(path + ".world");
            int chunkX = claimsConfig.getInt(path + ".x");
            int chunkZ = claimsConfig.getInt(path + ".z");
            UUID nationId = UUID.fromString(claimsConfig.getString(path + ".nation"));
            UUID claimedBy = UUID.fromString(claimsConfig.getString(path + ".claimed-by"));
            long claimedAt = claimsConfig.getLong(path + ".claimed-at");

            ClaimedChunk chunk = new ClaimedChunk(worldName, chunkX, chunkZ, nationId, claimedBy, claimedAt);
            plugin.getTerritoryManager().addClaim(chunk);
        }
    }

    public void saveClaims() {
        claimsConfig = new YamlConfiguration();
        for (ClaimedChunk chunk : plugin.getTerritoryManager().getAllClaims()) {
            String key = chunk.getWorldName() + "_" + chunk.getChunkX() + "_" + chunk.getChunkZ();
            String path = "claims." + key;
            claimsConfig.set(path + ".world", chunk.getWorldName());
            claimsConfig.set(path + ".x", chunk.getChunkX());
            claimsConfig.set(path + ".z", chunk.getChunkZ());
            claimsConfig.set(path + ".nation", chunk.getNationId().toString());
            claimsConfig.set(path + ".claimed-by", chunk.getClaimedBy().toString());
            claimsConfig.set(path + ".claimed-at", chunk.getClaimedAt());
        }
        saveFile(claimsConfig, claimsFile);
    }

    private void loadWars() {
        if (!warsConfig.contains("wars")) return;
        for (String warId : warsConfig.getConfigurationSection("wars").getKeys(false)) {
            String path = "wars." + warId;
            UUID attackerNation = UUID.fromString(warsConfig.getString(path + ".attacker"));
            UUID defenderNation = UUID.fromString(warsConfig.getString(path + ".defender"));
            String typeName = warsConfig.getString(path + ".type");
            String statusName = warsConfig.getString(path + ".status");
            long declaredAt = warsConfig.getLong(path + ".declared-at");
            long endsAt = warsConfig.getLong(path + ".ends-at");
            String reason = warsConfig.getString(path + ".reason", "");

            War war = new War(
                UUID.fromString(warId),
                attackerNation,
                defenderNation,
                WarType.valueOf(typeName),
                declaredAt,
                endsAt,
                reason
            );
            war.setStatus(WarStatus.valueOf(statusName));
            war.setAttackerKills(warsConfig.getInt(path + ".attacker-kills", 0));
            war.setDefenderKills(warsConfig.getInt(path + ".defender-kills", 0));

            plugin.getWarManager().addWarFallback(war);
        }
    }

    public void saveWars() {
        warsConfig = new YamlConfiguration();
        for (War war : plugin.getWarManager().getAllWars()) {
            String path = "wars." + war.getId();
            warsConfig.set(path + ".attacker", war.getAttackerNationId().toString());
            warsConfig.set(path + ".defender", war.getDefenderNationId().toString());
            warsConfig.set(path + ".type", war.getType().name());
            warsConfig.set(path + ".status", war.getStatus().name());
            warsConfig.set(path + ".declared-at", war.getDeclaredAt());
            warsConfig.set(path + ".ends-at", war.getEndsAt());
            warsConfig.set(path + ".reason", war.getReason());
            warsConfig.set(path + ".attacker-kills", war.getAttackerKills());
            warsConfig.set(path + ".defender-kills", war.getDefenderKills());
        }
        saveFile(warsConfig, warsFile);
    }

    private void loadEconomy() {
        if (!economyConfig.contains("accounts")) return;
        for (String uuidStr : economyConfig.getConfigurationSection("accounts").getKeys(false)) {
            double balance = economyConfig.getDouble("accounts." + uuidStr + ".balance", 0);
            PlayerAccount account = new PlayerAccount(UUID.fromString(uuidStr), balance);
            plugin.getEconomyManager().addAccount(account);
        }
    }

    public void saveEconomy() {
        economyConfig = new YamlConfiguration();
        for (PlayerAccount account : plugin.getEconomyManager().getAllAccounts()) {
            economyConfig.set("accounts." + account.getPlayerId() + ".balance", account.getBalance());
        }
        saveFile(economyConfig, economyFile);
    }

    private void loadPlayers() {
        if (!playersConfig.contains("players")) return;
        for (String uuidStr : playersConfig.getConfigurationSection("players").getKeys(false)) {
            String path = "players." + uuidStr;
            String gradeName = playersConfig.getString(path + ".grade", "JOUEUR");
            int level = playersConfig.getInt(path + ".level", 1);
            long xp = playersConfig.getLong(path + ".xp", 0);
            int claimCount = playersConfig.getInt(path + ".claim-count", 0);

            PlayerGrade grade = new PlayerGrade(UUID.fromString(uuidStr), gradeName, level, xp, claimCount);
            plugin.getGradeManager().addPlayerGrade(grade);
        }
    }

    public void savePlayers() {
        playersConfig = new YamlConfiguration();
        for (PlayerGrade grade : plugin.getGradeManager().getAllPlayerGrades()) {
            String path = "players." + grade.getPlayerId();
            playersConfig.set(path + ".grade", grade.getGradeName());
            playersConfig.set(path + ".level", grade.getLevel());
            playersConfig.set(path + ".xp", grade.getXp());
            playersConfig.set(path + ".claim-count", grade.getClaimCount());
        }
        saveFile(playersConfig, playersFile);
    }

    private void loadSeasons() {
        if (!seasonsConfig.contains("stats")) return;
        for (String uuidStr : seasonsConfig.getConfigurationSection("stats").getKeys(false)) {
            String path = "stats." + uuidStr;
            int kills = seasonsConfig.getInt(path + ".kills", 0);
            int deaths = seasonsConfig.getInt(path + ".deaths", 0);
            int warsWon = seasonsConfig.getInt(path + ".wars-won", 0);
            int claimsCount = seasonsConfig.getInt(path + ".claims", 0);

            PlayerStats stats = new PlayerStats(UUID.fromString(uuidStr), kills, deaths, warsWon, claimsCount);
            plugin.getSeasonManager().addPlayerStats(stats);
        }
        seasonsConfig.set("current-season", seasonsConfig.getInt("current-season", 1));
        plugin.getSeasonManager().setCurrentSeason(seasonsConfig.getInt("current-season", 1));
        plugin.getSeasonManager().setSeasonStartTime(seasonsConfig.getLong("season-start-time", System.currentTimeMillis()));
    }

    public void saveSeasons() {
        seasonsConfig = new YamlConfiguration();
        seasonsConfig.set("current-season", plugin.getSeasonManager().getCurrentSeason());
        seasonsConfig.set("season-start-time", plugin.getSeasonManager().getSeasonStartTime());

        for (PlayerStats stats : plugin.getSeasonManager().getAllPlayerStats()) {
            String path = "stats." + stats.getPlayerId();
            seasonsConfig.set(path + ".kills", stats.getKills());
            seasonsConfig.set(path + ".deaths", stats.getDeaths());
            seasonsConfig.set(path + ".wars-won", stats.getWarsWon());
            seasonsConfig.set(path + ".claims", stats.getClaimsCount());
        }
        saveFile(seasonsConfig, seasonsFile);
    }

    private void saveFile(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de sauvegarder " + file.getName(), e);
        }
    }
}
