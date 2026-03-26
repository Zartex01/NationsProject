package fr.nations.config;

import fr.nations.NationsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final NationsPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(NationsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getPrefix() {
        return colorize(config.getString("messages.prefix", "&8[&6Nations&8] "));
    }

    public String getMessage(String key) {
        return colorize(config.getString("messages." + key, "&cMessage manquant: " + key));
    }

    public int getMaxClaimsForGrade(String gradeName) {
        return config.getInt("grades." + gradeName.toLowerCase() + ".max-claims", 10);
    }

    public String getGradeDisplay(String gradeName, String defaultValue) {
        return colorize(config.getString("grades." + gradeName.toLowerCase() + ".display", defaultValue));
    }

    public String getGradeColorCode(String gradeName, String defaultColor) {
        return colorize(config.getString("grades." + gradeName.toLowerCase() + ".color", defaultColor));
    }

    public String getRoleDisplay(String roleName, String defaultValue) {
        return colorize(config.getString("nation-roles." + roleName + ".display", defaultValue));
    }

    public String getRoleColor(String roleName, String defaultColor) {
        return colorize(config.getString("nation-roles." + roleName + ".color", defaultColor));
    }

    public boolean getRolePerm(String roleName, String perm, boolean defaultValue) {
        return config.getBoolean("nation-roles." + roleName + "." + perm, defaultValue);
    }

    public double getNationCreationCost() {
        return config.getDouble("nations.creation-cost", 500);
    }

    public int getMaxNationNameLength() {
        return config.getInt("nations.max-name-length", 20);
    }

    public int getMinNationNameLength() {
        return config.getInt("nations.min-name-length", 3);
    }

    public double getClaimPrice() {
        return config.getDouble("claims.price-per-chunk", 100);
    }

    public double getStartingBalance() {
        return config.getDouble("economy.starting-balance", 250);
    }

    public double getWarDeclarationCost() {
        return config.getDouble("wars.declaration-cost", 1000);
    }

    public int getWarDeclarationCooldownHours() {
        return config.getInt("wars.declaration-cooldown-hours", 24);
    }

    public int getSeasonDurationDays() {
        return config.getInt("seasons.duration-days", 30);
    }

    public int getXpPerBlockPlaced() {
        return config.getInt("seasons.level.xp-per-block-placed", 1);
    }

    public int getXpPerKill() {
        return config.getInt("seasons.level.xp-per-kill", 50);
    }

    public int getXpPerWarWon() {
        return config.getInt("seasons.level.xp-per-war-won", 500);
    }

    public int getXpPerClaim() {
        return config.getInt("seasons.level.xp-per-claim", 10);
    }

    public int getMaxLevel() {
        return config.getInt("seasons.level.max-level", 100);
    }

    public int getXpFormulaMultiplier() {
        return config.getInt("seasons.level.xp-formula-multiplier", 100);
    }

    public int getSeasonFirstPlaceReward() {
        return config.getInt("seasons.rewards.first-place", 10000);
    }

    public int getSeasonSecondPlaceReward() {
        return config.getInt("seasons.rewards.second-place", 5000);
    }

    public int getSeasonThirdPlaceReward() {
        return config.getInt("seasons.rewards.third-place", 2500);
    }

    public int getSeasonFirstPlaceXpReward() {
        return config.getInt("seasons.rewards.first-place-xp", 5000);
    }

    public int getSeasonSecondPlaceXpReward() {
        return config.getInt("seasons.rewards.second-place-xp", 2500);
    }

    public int getSeasonThirdPlaceXpReward() {
        return config.getInt("seasons.rewards.third-place-xp", 1000);
    }

    public int getNationMaxLevel() {
        return config.getInt("seasons.nation-level.max-level", 50);
    }

    public int getNationXpFormulaMultiplier() {
        return config.getInt("seasons.nation-level.xp-formula-multiplier", 1000);
    }

    public boolean isDebug() {
        return config.getBoolean("plugin.debug", false);
    }

    private String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }
}
