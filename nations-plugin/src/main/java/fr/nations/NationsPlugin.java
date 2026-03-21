package fr.nations;

import fr.nations.commands.*;
import fr.nations.config.ConfigManager;
import fr.nations.economy.EconomyManager;
import fr.nations.grade.GradeManager;
import fr.nations.listeners.*;
import fr.nations.nation.NationManager;
import fr.nations.season.SeasonManager;
import fr.nations.territory.TerritoryManager;
import fr.nations.util.DataManager;
import fr.nations.war.WarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NationsPlugin extends JavaPlugin {

    private static NationsPlugin instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private NationManager nationManager;
    private TerritoryManager territoryManager;
    private WarManager warManager;
    private EconomyManager economyManager;
    private SeasonManager seasonManager;
    private GradeManager gradeManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.gradeManager = new GradeManager(this);
        this.economyManager = new EconomyManager(this);
        this.nationManager = new NationManager(this);
        this.territoryManager = new TerritoryManager(this);
        this.warManager = new WarManager(this);
        this.seasonManager = new SeasonManager(this);

        dataManager.loadAll();

        registerCommands();
        registerListeners();

        getLogger().info("NationsEpoque v" + getDescription().getVersion() + " activé avec succès!");
        getLogger().info("Nations chargées: " + nationManager.getAllNations().size());
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        if (warManager != null) {
            warManager.shutdown();
        }
        if (seasonManager != null) {
            seasonManager.shutdown();
        }
        getLogger().info("NationsEpoque désactivé. Données sauvegardées.");
    }

    private void registerCommands() {
        NationCommand nationCommand = new NationCommand(this);
        getCommand("nation").setExecutor(nationCommand);
        getCommand("nation").setTabCompleter(nationCommand);

        WarCommand warCommand = new WarCommand(this);
        getCommand("war").setExecutor(warCommand);
        getCommand("war").setTabCompleter(warCommand);

        CoalitionCommand coalitionCommand = new CoalitionCommand(this);
        getCommand("coalition").setExecutor(coalitionCommand);
        getCommand("coalition").setTabCompleter(coalitionCommand);

        ClaimCommand claimCommand = new ClaimCommand(this);
        getCommand("claim").setExecutor(claimCommand);
        getCommand("claim").setTabCompleter(claimCommand);

        MoneyCommand moneyCommand = new MoneyCommand(this);
        getCommand("money").setExecutor(moneyCommand);
        getCommand("money").setTabCompleter(moneyCommand);

        SeasonCommand seasonCommand = new SeasonCommand(this);
        getCommand("season").setExecutor(seasonCommand);
        getCommand("season").setTabCompleter(seasonCommand);

        NationsAdminCommand adminCommand = new NationsAdminCommand(this);
        getCommand("nationsadmin").setExecutor(adminCommand);
        getCommand("nationsadmin").setTabCompleter(adminCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.nations.listeners.GuiClickListener(this), this);
    }

    public static NationsPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public NationManager getNationManager() { return nationManager; }
    public TerritoryManager getTerritoryManager() { return territoryManager; }
    public WarManager getWarManager() { return warManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public SeasonManager getSeasonManager() { return seasonManager; }
    public GradeManager getGradeManager() { return gradeManager; }
}
