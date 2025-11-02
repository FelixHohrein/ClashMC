package de.payne.clashmc;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import de.payne.clashmc.attacks.AttackManager;
import de.payne.clashmc.attacks.equipment.EquipmentManager;
import de.payne.clashmc.cache.CacheManager;
import de.payne.clashmc.commands.ClashCommand;
import de.payne.clashmc.config.ConfigManager;
import de.payne.clashmc.commands.CommandHandler;
import de.payne.clashmc.commands.subcommands.AddCoinsCommand;
import de.payne.clashmc.commands.subcommands.InfoCommand;
import de.payne.clashmc.commands.subcommands.MineSessionCommand;
import de.payne.clashmc.commands.subcommands.ReloadCommand;
import de.payne.clashmc.commands.subcommands.ResetCommand;
import de.payne.clashmc.commands.subcommands.SaveMineCommand;
import de.payne.clashmc.commands.subcommands.SaveSchematicCommand;
import de.payne.clashmc.commands.subcommands.StatsCommand;
import de.payne.clashmc.commands.subcommands.TopCommand;
import de.payne.clashmc.commands.subcommands.UpgradeCommand;
import de.payne.clashmc.database.DatabaseManager;
import de.payne.clashmc.database.MySqlDatabase;
import de.payne.clashmc.database.migrations.MigrationManager;
import de.payne.clashmc.economy.ResourceManager;
import de.payne.clashmc.files.VillageDataHandler;
import de.payne.clashmc.listeners.player.AttackBlockBreakListener;
import de.payne.clashmc.listeners.player.AttackMenuClickListener;
import de.payne.clashmc.listeners.player.MineEnterListener;
import de.payne.clashmc.listeners.player.MineLeaveListener;
import de.payne.clashmc.listeners.player.MineRewardClickListener;
import de.payne.clashmc.listeners.player.PlayerInteractListener;
import de.payne.clashmc.listeners.player.PlayerJoinListener;
import de.payne.clashmc.listeners.player.PlayerLeaveListener;
import de.payne.clashmc.listeners.player.ShopClickListener;
import de.payne.clashmc.listeners.player.TownHallClickListener;
import de.payne.clashmc.listeners.player.UpgradeShopClickListener;
import de.payne.clashmc.maphandling.schematic.SchematicManager;
import de.payne.clashmc.maphandling.schematic.VillageAllocator;
import de.payne.clashmc.mine.MineManager;
import de.payne.clashmc.mine.MineSchematicManager;
import lombok.Getter;



public class ClashMC extends JavaPlugin {

	@Getter
	private ConfigManager configManager;
	@Getter
	private MySqlDatabase database;
	@Getter
	private DatabaseManager databaseManager;
	@Getter
	private CacheManager cacheManager;
	@Getter
    private VillageDataHandler villageDataHandler;
	@Getter
	private SchematicManager schematicManager;
	@Getter
	private VillageAllocator villageAllocator;
	@Getter
	private MineSchematicManager mineSchematicManager;
	@Getter
	private MineManager mineManager;
	@Getter
	private AttackManager attackManager;
	@Getter
	private de.payne.clashmc.replay.ReplayWorldManager replayWorldManager;
	@Getter
	private EquipmentManager equipmentManager;
	
	private MigrationManager migrationManager;
	
	public static ClashMC getInstance() {
		return JavaPlugin.getPlugin(ClashMC.class);
	}
	
	
	
	
	@Override
	public void onEnable() {
		// Config als erstes laden
		this.configManager = new ConfigManager(this);
		this.configManager.validateConfig();
		getLogger().info("===========================================");
		getLogger().info("ClashMC wird geladen...");
		getLogger().info("===========================================");
		
		this.database = new MySqlDatabase(this);
        this.database.openConnection();
        this.database.keepAlive();
        
        this.databaseManager = new DatabaseManager(this.database);
        
        try {
            this.migrationManager = new MigrationManager(this.databaseManager.getConnection(), this.getLogger());
            if(this.database.isConnected()) {
                this.migrationManager.migrate();
            } else {
                getLogger().severe("Datenbankverbindung ist fehlgeschlagen!");
            }
        } catch (Exception e) {
            getLogger().severe("Fehler beim Initialisieren der Migrations: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Initialize Cache Manager
        this.cacheManager = new CacheManager(this);
        getLogger().info("Cache-Manager initialisiert");
        
        this.villageDataHandler = new VillageDataHandler(this);
        this.schematicManager = new SchematicManager(this);
        this.villageAllocator = new VillageAllocator(villageDataHandler, this.getServer().getWorld("clash")); // oder entsprechende Welt
        this.mineSchematicManager = new MineSchematicManager(this);
        this.mineManager = new MineManager(this);
        this.attackManager = new AttackManager(this);
        this.equipmentManager = new EquipmentManager();
        this.replayWorldManager = new de.payne.clashmc.replay.ReplayWorldManager(this);
        getLogger().info("Replay-System initialisiert");
        
        this.registerCommands();
        this.registerListener();
        this.setWorldAttributes();
        this.onReload();
	}
	
	@Override
	public void onDisable() {
	    if (this.database != null && this.database.isConnected()) {
	        this.database.closeConnection();
	    }
	}
	
	
	
	private final void onReload() {
		//only true when reload 
		
		if(Bukkit.getOnlinePlayers().size() > 0) {
			getLogger().info("ClashMC Reload gestartet...");
			//DO SOMETHING WHEN RELOAD
		}
	}
	
	private final void registerListener() {
		 this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
		 this.getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this); // ResourceCollector
		 this.getServer().getPluginManager().registerEvents(new TownHallClickListener(), this); // Inventory GUI
		 this.getServer().getPluginManager().registerEvents(new ShopClickListener(), this); // Inventory GUI
		 this.getServer().getPluginManager().registerEvents(new UpgradeShopClickListener(this), this); // Inventory GUI
		 this.getServer().getPluginManager().registerEvents(new PlayerLeaveListener(), this); // Inventory GUI
		 this.getServer().getPluginManager().registerEvents(new MineLeaveListener(), this); // Triggered beim verlassen der Mine
		 this.getServer().getPluginManager().registerEvents(new MineRewardClickListener(), this); // MineGUI 
		 this.getServer().getPluginManager().registerEvents(new MineEnterListener(), this); // Block Interact Mine
		 this.getServer().getPluginManager().registerEvents(new AttackMenuClickListener(this, this.attackManager), this); // Attack GIU
		 this.getServer().getPluginManager().registerEvents(new AttackBlockBreakListener(), this);
	}
	
	private final void registerCommands() {
		
		CommandHandler handler = new CommandHandler();
		getCommand("clash").setExecutor(handler);
		getCommand("clash").setTabCompleter(handler);
		
		handler.register("clash", new ClashCommand());
		handler.register("info", new InfoCommand());
		handler.register("top", new TopCommand());

		if (Bukkit.getPluginManager().getPermission("clashmc.admin") != null) {
		    handler.register("reset", new ResetCommand());
		    handler.register("saveschematic", new SaveSchematicCommand(this.schematicManager));
			handler.register("upgrade", new UpgradeCommand());
			handler.register("addcoins", new AddCoinsCommand());
			handler.register("savemine", new SaveMineCommand(this.mineSchematicManager));
			handler.register("mine", new MineSessionCommand(this.mineManager));
			handler.register("stats", new StatsCommand());
			handler.register("reload", new ReloadCommand(this));
		}
		
	}
	
	private final void setWorldAttributes() {
		World world = Bukkit.getWorld("Clash"); // oder "mine"
		if (world != null) {
			world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
			world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
			world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
			world.setGameRule(GameRule.MOB_GRIEFING, false);
			world.setGameRule(GameRule.KEEP_INVENTORY, true);
			world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
			world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
			world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
			world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
			world.setGameRule(GameRule.DO_FIRE_TICK, false);
			world.setGameRule(GameRule.DO_TILE_DROPS, false);
			world.setGameRule(GameRule.DO_ENTITY_DROPS, false);
			world.setPVP(false);

			World attack = Bukkit.getWorld("Attacks"); // oder "mine"
			if (world != null) {
				attack.setGameRule(GameRule.DO_MOB_SPAWNING, false);
				attack.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
				attack.setTime(1000);
				attack.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
				attack.setGameRule(GameRule.MOB_GRIEFING, true);
				attack.setGameRule(GameRule.KEEP_INVENTORY, true);
				attack.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
				attack.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
				attack.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
				attack.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
				attack.setGameRule(GameRule.DO_FIRE_TICK, true);
				attack.setGameRule(GameRule.DO_TILE_DROPS, false);
				attack.setGameRule(GameRule.DO_ENTITY_DROPS, false);
				attack.setPVP(true);
			}
		}
	}
}
