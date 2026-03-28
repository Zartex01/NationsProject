package fr.nations;

import fr.nations.atm.AtmManager;
import fr.nations.commands.*;
import fr.nations.config.ConfigManager;
import fr.nations.database.DatabaseManager;
import fr.nations.economy.EconomyManager;
import fr.nations.grade.GradeManager;
import fr.nations.hdv.HdvManager;
import fr.nations.kit.KitManager;
import fr.nations.kit.PlaytimeTracker;
import fr.nations.shop.ShopManager;
import fr.nations.listeners.*;
import fr.nations.nation.NationManager;
import fr.nations.role.CustomRoleManager;
import fr.nations.season.SeasonManager;
import fr.nations.territory.TerritoryManager;
import fr.nations.util.DataManager;
import fr.nations.war.WarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NationsPlugin extends JavaPlugin {

    private static NationsPlugin instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private DataManager dataManager;
    private NationManager nationManager;
    private TerritoryManager territoryManager;
    private WarManager warManager;
    private EconomyManager economyManager;
    private SeasonManager seasonManager;
    private GradeManager gradeManager;
    private CustomRoleManager customRoleManager;
    private AtmManager atmManager;
    private KitManager kitManager;
    private PlaytimeTracker playtimeTracker;
    private ShopManager shopManager;
    private HdvManager hdvManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);

        this.databaseManager = new DatabaseManager(this);
        boolean dbConnected = databaseManager.connect();

        if (dbConnected) {
            databaseManager.createTables();
            getLogger().info("Base de données SQLite prête.");
        } else {
            getLogger().warning("Base de données indisponible — mode dégradé (YAML fallback).");
        }

        this.gradeManager = new GradeManager(this);
        this.economyManager = new EconomyManager(this);
        this.atmManager = new AtmManager(this);
        this.playtimeTracker = new PlaytimeTracker(this);
        this.kitManager = new KitManager(this);
        this.shopManager = new ShopManager(this);
        this.hdvManager  = new HdvManager(this);
        this.nationManager = new NationManager(this);
        this.territoryManager = new TerritoryManager(this);
        this.warManager = new WarManager(this);
        this.seasonManager = new SeasonManager(this);
        this.customRoleManager = new CustomRoleManager(this);
        this.dataManager = new DataManager(this);

        if (dbConnected) {
            nationManager.loadFromDatabase();
            economyManager.loadFromDatabase();
            gradeManager.loadFromDatabase();
            warManager.loadAll();
            seasonManager.loadFromDatabase();
            customRoleManager.loadAll();
            territoryManager.loadFromDatabase();
        } else {
            dataManager.loadAll();
        }

        warManager.startTasks();

        registerCommands();
        registerListeners();

        getLogger().info("NationsEpoque v" + getDescription().getVersion() + " activé!");
        getLogger().info("Nations chargées: " + nationManager.getAllNations().size());
    }

    @Override
    public void onDisable() {
        if (playtimeTracker != null) playtimeTracker.saveAll();
        if (atmManager != null) atmManager.flushAllSessions();
        if (warManager != null) warManager.shutdown();
        if (seasonManager != null) seasonManager.shutdown();
        if (databaseManager != null) databaseManager.close();
        getLogger().info("NationsEpoque désactivé.");
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

        NationRoleCommand roleCommand = new NationRoleCommand(this);
        getCommand("nationrole").setExecutor(roleCommand);
        getCommand("nationrole").setTabCompleter(roleCommand);

        AtmCommand atmCommand = new AtmCommand(this);
        getCommand("atm").setExecutor(atmCommand);
        getCommand("atm").setTabCompleter(atmCommand);

        PayCommand payCommand = new PayCommand(this);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);

        BalanceCommand balanceCommand = new BalanceCommand(this);
        getCommand("bal").setExecutor(balanceCommand);
        getCommand("bal").setTabCompleter(balanceCommand);

        GiveMoneyCommand giveMoneyCommand = new GiveMoneyCommand(this);
        getCommand("givemoney").setExecutor(giveMoneyCommand);
        getCommand("givemoney").setTabCompleter(giveMoneyCommand);

        SetGradeCommand setGradeCommand = new SetGradeCommand(this);
        getCommand("setgrade").setExecutor(setGradeCommand);
        getCommand("setgrade").setTabCompleter(setGradeCommand);

        KitCommand kitCommand = new KitCommand(this);
        getCommand("kit").setExecutor(kitCommand);
        getCommand("kit").setTabCompleter(kitCommand);

        CraftCommand craftCommand = new CraftCommand(this);
        getCommand("craft").setExecutor(craftCommand);
        getCommand("craft").setTabCompleter(craftCommand);

        PWeatherCommand pWeatherCommand = new PWeatherCommand(this);
        getCommand("pweather").setExecutor(pWeatherCommand);
        getCommand("pweather").setTabCompleter(pWeatherCommand);

        PTimeCommand pTimeCommand = new PTimeCommand(this);
        getCommand("ptime").setExecutor(pTimeCommand);
        getCommand("ptime").setTabCompleter(pTimeCommand);

        FurnaceCommand furnaceCommand = new FurnaceCommand(this);
        getCommand("furnace").setExecutor(furnaceCommand);
        getCommand("furnace").setTabCompleter(furnaceCommand);

        StonecutterCommand stonecutterCommand = new StonecutterCommand(this);
        getCommand("stonecutter").setExecutor(stonecutterCommand);
        getCommand("stonecutter").setTabCompleter(stonecutterCommand);

        AnvilCommand anvilCommand = new AnvilCommand(this);
        getCommand("anvil").setExecutor(anvilCommand);
        getCommand("anvil").setTabCompleter(anvilCommand);

        BackCommand backCommand = new BackCommand(this);
        getCommand("back").setExecutor(backCommand);
        getCommand("back").setTabCompleter(backCommand);

        EnderChestCommand ecCommand = new EnderChestCommand(this);
        getCommand("ec").setExecutor(ecCommand);
        getCommand("ec").setTabCompleter(ecCommand);

        XpBottleCommand xpbCommand = new XpBottleCommand(this);
        getCommand("xpb").setExecutor(xpbCommand);
        getCommand("xpb").setTabCompleter(xpbCommand);

        RepairCommand repairCommand = new RepairCommand(this);
        getCommand("repair").setExecutor(repairCommand);
        getCommand("repair").setTabCompleter(repairCommand);

        NickCommand nickCommand = new NickCommand(this);
        getCommand("nick").setExecutor(nickCommand);
        getCommand("nick").setTabCompleter(nickCommand);

        HdvCommand hdvCommand = new HdvCommand(this);
        getCommand("hdv").setExecutor(hdvCommand);
        getCommand("hdv").setTabCompleter(hdvCommand);

        ShopCommand shopCommand = new ShopCommand(this);
        getCommand("shop").setExecutor(shopCommand);
        getCommand("shop").setTabCompleter(shopCommand);

        SellCommand sellCommand = new SellCommand(this);
        getCommand("sell").setExecutor(sellCommand);
        getCommand("sell").setTabCompleter(sellCommand);

        getServer().getPluginManager().registerEvents(new BackLocationListener(this, backCommand), this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(playtimeTracker, this);
        getServer().getPluginManager().registerEvents(new HdvChatListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiClickListener(this), this);
        getServer().getPluginManager().registerEvents(new AtmPlaytimeListener(this), this);
        getServer().getPluginManager().registerEvents(new GradeCommandListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
    }

    public static NationsPlugin getInstance() { return instance; }

    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public DataManager getDataManager() { return dataManager; }
    public NationManager getNationManager() { return nationManager; }
    public TerritoryManager getTerritoryManager() { return territoryManager; }
    public WarManager getWarManager() { return warManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public SeasonManager getSeasonManager() { return seasonManager; }
    public GradeManager getGradeManager() { return gradeManager; }
    public CustomRoleManager getCustomRoleManager() { return customRoleManager; }
    public AtmManager getAtmManager() { return atmManager; }
    public KitManager getKitManager() { return kitManager; }
    public PlaytimeTracker getPlaytimeTracker() { return playtimeTracker; }
    public ShopManager getShopManager()         { return shopManager; }
    public HdvManager getHdvManager()           { return hdvManager; }
}
