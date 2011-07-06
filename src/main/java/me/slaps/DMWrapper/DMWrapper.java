package me.slaps.DMWrapper;

import com.gmail.haloinverse.DynamicMarket.DynamicMarketAPI;
import com.gmail.haloinverse.DynamicMarket.Messaging;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class DMWrapper extends JavaPlugin {	
	public String name;
	public String version;
	
	private Logger log = Logger.getLogger("Minecraft");
	private DynamicMarketAPI dmapi;
	private DMWrapperPluginListener pluginListener = new DMWrapperPluginListener(this);
	private ILocationManager locationManager;
	
	private HashMap<String, Integer> pointsSet = new HashMap<String, Integer>();
	private HashMap<String, ShopLocation> shops = new HashMap<String, ShopLocation>();
	private WorldGuardPlugin worldguard;
	
	private boolean attemptWG;
	private boolean useWG;
	
	public void onDisable() {
		log.info("[" + this.name + "] Version " + this.version + " disabled.");
	}
	
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		this.name = pdfFile.getName();
		this.version = pdfFile.getVersion();
		
		log.info("[" + name + "] Initializing version " + this.version + ".");
		
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			log.info("[" + this.name + "] Creating config file.");
			createConfig();
		}
		Configuration config = new Configuration(configFile);
		config.load();
		useWG = config.getBoolean("worldguard", false);
		attemptWG = useWG;
		config.save();
		
		PluginManager pm = getServer().getPluginManager();
		pluginListener.checkPlugins(pm);
		
		if (attemptWG && worldguard != null) {
			locationManager = new LocationManagerWG(this, worldguard);
			log.info("[" + this.name + "] Linked with WorldGuard successfully.");
		} else {
			useWG = false;
			locationManager = new LocationManager(this);
		}

		pm.registerEvent(Event.Type.PLUGIN_ENABLE, pluginListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, pluginListener, Priority.Monitor, this);
		
		log.info("[" + this.name + "] Version " + this.version + " enabled.");
	}
	
	private void createConfig() {
		Configuration config = new Configuration(new File(getDataFolder(), "config.yml"));
		config.load();
		config.setProperty("worldguard", false);
		config.save();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		// Command is from console
		if (!(sender instanceof Player))
			return dmapi.returnCommand(sender, cmd, args);
		
		// Not a shop command
		if (!cmd.getName().equalsIgnoreCase("shop") && !cmd.getName().equalsIgnoreCase("dshop")) {
			return dmapi.returnCommand(sender, cmd, args);
		}
		
		// No arguments, just /shop
		if (args.length == 0) {
			return dmapi.returnCommand(sender, cmd, args);
		}
		
		Messaging message = new Messaging(sender);
		String username = ((Player) sender).getName();
		
		// Not setting up a location
		if (!args[0].equalsIgnoreCase("location") && !args[0].equalsIgnoreCase("-l")) {
			// Check if they are in a shop or have the location bypass permission
			if (!locationManager.enabled() || dmapi.hasPermission(sender, "admin.location.bypass") || locationManager.inShop(((Player) sender).getLocation())) {
				return dmapi.returnCommand(sender, cmd, args);
			} else {
				message.send(dmapi.getShopTag() + " You must be in a shop area to use that command.");
				return true;
			}
		} else {
			if (args.length == 2 && args[1].equalsIgnoreCase("enable") && dmapi.hasPermission(sender, "admin.location.toggle")) {
				locationManager.enableLocations();
				message.send(dmapi.getShopTag() + " Shop locations enabled.");
				return true;
			} else if (args.length == 2 && args[1].equalsIgnoreCase("disable") && dmapi.hasPermission(sender, "admin.location.toggle")) {
				locationManager.disableLocations();
				message.send(dmapi.getShopTag() + " Shop locations disabled.");
				return true;
			} else if (useWG) {
				String world = ((Player) sender).getWorld().getName();
				if (args.length == 3 && args[1].equalsIgnoreCase("create") && dmapi.hasPermission(sender, "admin.location.create")) {
					String region = args[2];
					if (((LocationManagerWG) locationManager).add(world, region)) {
						message.send(dmapi.getShopTag() + " Shop location added on: " + region);
					} else {
						message.send(dmapi.getShopTag() + " Error creating shop on: " + region);
					}
					return true;
				} else if (args.length == 3 && args[1].equalsIgnoreCase("delete") && dmapi.hasPermission(sender, "admin.location.delete")) {
					String region = args[2];
					if (((LocationManagerWG) locationManager).removeShop(world, region)) {
						message.send(dmapi.getShopTag() + " Shop removed.");
					} else {
						message.send(dmapi.getShopTag() + " Error removing shop: " + region);
					}
					return true;
				} else if (args.length == 2 && args[1].equalsIgnoreCase("check") && dmapi.hasPermission(sender, "location.check")) {
					String id = ((LocationManagerWG) locationManager).getShopId(((Player) sender).getLocation());
					if (id == null) {
						message.send(dmapi.getShopTag() + " You are not in a shop area.");
					} else {
						message.send(dmapi.getShopTag() + " You are in shop: " + id);
					}
					return true;
				} else if (args.length == 2 && args[1].equalsIgnoreCase("list") && dmapi.hasPermission(sender, "location.list")) {
					message.send("Shop areas: " + locationManager.listShops());
					return true;
				} else if (args.length == 3 && args[1].equalsIgnoreCase("tp") && dmapi.hasPermission(sender, "admin.location.tp")) {
					String id = args[2];
					
					Location dest = ((LocationManagerWG) locationManager).getShopLocation(world, id);
					if (dest == null) {
						message.send(dmapi.getShopTag() + " Shop area " + id + " not found.");
					} else {
						((Player) sender).teleport(dest);
					}
					return true;
				}
			} else {			
				if (args.length == 2 && args[1].equalsIgnoreCase("set") && dmapi.hasPermission(sender, "admin.location.create")) {
					HashSet<Byte> transparent = new HashSet<Byte>();
					transparent.add((byte) Material.AIR.getId());
					transparent.add((byte) Material.LAVA.getId());
					transparent.add((byte) Material.WATER.getId());
					Location target = ((Player) sender).getTargetBlock(transparent, 300).getLocation();
					
					int points;
					ShopLocation shop = null;
					
					if (pointsSet.containsKey(username)) {
						points = pointsSet.get(username);
						if (points == 1) {
							shop = shops.get(username);
							shop.setLocation(2, target);
							shops.put(username, shop);
							pointsSet.put(username, 2);
							message.send(dmapi.getShopTag() + " Second point set (X:" + target.getBlockX() + " Y:" + target.getBlockY() + " Z:" + target.getBlockZ() + "). Finalize selection with /shop location create.");
						} else if (points == 2) {
							shops.put(username, new ShopLocation(target));
							pointsSet.put(username, 1);
							message.send(dmapi.getShopTag() + " First point set (X:" + target.getBlockX() + " Y:" + target.getBlockY() + " Z:" + target.getBlockZ() + "). Please select a second point.");
						}
					} else {
						shops.put(username, new ShopLocation(target));
						pointsSet.put(username, 1);
						message.send(dmapi.getShopTag() + " First point set (X:" + target.getBlockX() + " Y:" + target.getBlockY() + " Z:" + target.getBlockZ() + "). Please select a second point.");
					}
					
					return true;
				} else if (args.length == 2 && args[1].equalsIgnoreCase("create") && dmapi.hasPermission(sender, "admin.location.create")) {
					ShopLocation shop = null;
					int points = pointsSet.get(username);
					if (points == 1) {
						message.send(dmapi.getShopTag() + " You need to select a second point!");
						return true;
					} else if (points == 2) {
						shop = shops.get(username);
					} else {
						return true;
					}
					
					if (shop != null) {
						if (((LocationManager) locationManager).add(shop)) {
							message.send(dmapi.getShopTag() + " Shop location added.");
						} else {
							message.send(dmapi.getShopTag() + " Error creating shop location.");
						}
					} else {
						message.send(dmapi.getShopTag() + " Error creating shop location.");
					}
					
					return true;
				} else if (args.length == 2 && args[1].equalsIgnoreCase("cancel") && dmapi.hasPermission(sender, "admin.location.create")) {
					if (pointsSet.containsKey(username)) {
						pointsSet.remove("username");
						shops.remove("username");
						message.send(dmapi.getShopTag() + " Location setup cancelled.");
					} else {
						message.send(dmapi.getShopTag() + " You are not setting up a shop location!");
					}
					return true;
				} else if (args.length == 2 && args[1].equalsIgnoreCase("check") && dmapi.hasPermission(sender, "location.check")) {
					Integer id = ((LocationManager) locationManager).getShopId(((Player) sender).getLocation());
					if (id < 0) {
						message.send(dmapi.getShopTag() + " You are not in a shop area.");
					} else {
						message.send(dmapi.getShopTag() + " You are in shop ID: " + Integer.toString(id));
					}
					return true;
				} else if (args.length == 2 && args[1].equalsIgnoreCase("delete") && dmapi.hasPermission(sender, "admin.location.delete")) {
					Integer id = ((LocationManager) locationManager).getShopId(((Player) sender).getLocation());
					if (id < 0) {
						message.send(dmapi.getShopTag() + " No shop found. Specify an ID or stand in a shop.");
					} else if (((LocationManager) locationManager).removeShop(id)) {
						message.send(dmapi.getShopTag() + " Shop removed.");
					} else {
						message.send(dmapi.getShopTag() + " Error removing shop ID: " + Integer.toString(id));
					}
					return true;
				} else if (args.length == 3 && args[1].equalsIgnoreCase("delete") && dmapi.hasPermission(sender, "admin.location.delete")) {
					Integer id;
					try {
						id = Integer.parseInt(args[2]);
					} catch (NumberFormatException ex) {
						message.send(dmapi.getShopTag() + " Invalid shop ID: " + args[2]);
						return true;
					}
					
					if (((LocationManager) locationManager).removeShop(id)) {
						message.send(dmapi.getShopTag() + " Shop removed.");
					} else {
						message.send(dmapi.getShopTag() + " Error removing shop ID: " + Integer.toString(id));
					}
					return true;
				} else if (args.length == 2 && args[1].equalsIgnoreCase("list") && dmapi.hasPermission(sender, "location.list")) {
					message.send("Shop IDs: " + locationManager.listShops());
					return true;
				} else if (args.length == 3 && args[1].equalsIgnoreCase("tp") && dmapi.hasPermission(sender, "admin.location.tp")) {
					Integer id;
					try {
						id = Integer.parseInt(args[2]);
					} catch (NumberFormatException ex) {
						message.send(dmapi.getShopTag() + " Invalid shop ID: " + args[2]);
						return true;
					}
					
					Location dest = ((LocationManager) locationManager).getShopLocation(id);
					if (dest == null) {
						message.send(dmapi.getShopTag() + " Shop ID " + Integer.toString(id) + " not found.");
					} else {
						((Player) sender).teleport(dest);
					}
					return true;
				}
			}

			if (useWG) {
				if (dmapi.hasPermission(sender, "admin.location.create")) {
					message.send("{CMD} /shop location create {PRM}<region> {BKT}- {}Creates a shop area in specified WorldGuard region");
				}
				if (dmapi.hasPermission(sender, "admin.location.delete")) {
					message.send("{CMD} /shop location delete {BKT}[{PRM}region{BKT}] - {}Removes shop area on specified WorldGuard region");
				}
			} else {
				if (dmapi.hasPermission(sender, "admin.location.create")) {
					message.send("{CMD} /shop location set {BKT}- {}Sets the two corners of the shop");
					message.send("{CMD} /shop location create {BKT}- {}Creates a shop location from two selected points");
					message.send("{CMD} /shop location cancel {BKT}- {}Cancels setting a shop location");
				}
				if (dmapi.hasPermission(sender, "admin.location.delete")) {
					message.send("{CMD} /shop location delete {BKT}[{PRM}id{BKT}] - {}Removes specified shop ID. Removes shop you are in if no shop specified");
				}
			}
			if (dmapi.hasPermission(sender, "location.check")) {
				message.send("{CMD} /shop location check {BKT}- {}Checks if you are in a shop region");
			}
			if (dmapi.hasPermission(sender, "admin.location.toggle")) {
				message.send("{CMD} /shop location enable {BKT}- {}Enables location based shops");
				message.send("{CMD} /shop location disable {BKT}- {}Disable location based shops");
			}
			if (dmapi.hasPermission(sender, "admin.location.list")) {
				message.send("{CMD} /shop location list {BKT}- {}Lists shop areas");
			}
			if (dmapi.hasPermission(sender, "admin.location.tp")) {
				message.send("{CMD} /shop location tp {PRM}<id> {BKT}- {}Teleports to specified shop ");
			}
			
			return true;
		}
	}
	
	public void setAPI(DynamicMarketAPI api) {
		this.dmapi = api;
	}
	
	public DynamicMarketAPI getAPI() {
		return this.dmapi;
	}
	
	public void hookAPI() {
		dmapi.hookWrapper(this);
	}
	
	public void disable() {
		getServer().getPluginManager().disablePlugin(this);
	}

	public void setWG(WorldGuardPlugin wg) {
		if (!attemptWG) return;
		this.worldguard = wg;
		useWG = true;
		locationManager = new LocationManagerWG(this, worldguard);
		log.info("[" + this.name + "] Linked with WorldGuard successfully.");
	}

	public WorldGuardPlugin getWG() {
		return this.worldguard;
	}
}
