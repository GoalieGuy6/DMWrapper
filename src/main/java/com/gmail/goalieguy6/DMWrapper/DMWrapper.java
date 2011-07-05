package com.gmail.goalieguy6.DMWrapper;

import com.gmail.haloinverse.DynamicMarket.DynamicMarketAPI;
import com.gmail.haloinverse.DynamicMarket.Messaging;

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

public class DMWrapper extends JavaPlugin {	
	public String name;
	public String version;
	
	private Logger log = Logger.getLogger("Minecraft");
	private DynamicMarketAPI dmapi;
	private DMWrapperPluginListener pluginListener = new DMWrapperPluginListener(this);
	private LocationManager locationManager;
	
	private HashMap<String, Integer> pointsSet = new HashMap<String, Integer>();
	private HashMap<String, ShopLocation> shops = new HashMap<String, ShopLocation>();
	
	public void onDisable() {
		log.info("[" + this.name + "] Disabled.");
	}
	
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		this.name = pdfFile.getName();
		this.version = pdfFile.getVersion();
		
		log.info("[" + this.name + "] Initializing version " + this.version + ".");
				
		PluginManager pm = getServer().getPluginManager();
		locationManager = new LocationManager(this);

		pm.registerEvent(Event.Type.PLUGIN_ENABLE, pluginListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, pluginListener, Priority.Monitor, this);
		
		log.info("[" + this.name + "] Version " + this.version + " enabled.");
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
		
		// Not setting up a location
		if (!args[0].equalsIgnoreCase("location") && !args[0].equalsIgnoreCase("-l")) {
			// Check if they are in a shop or have the location bypass permission
			if (dmapi.hasPermission(sender, "admin.location.bypass") || locationManager.inShop(((Player) sender).getLocation())) {
				return dmapi.returnCommand(sender, cmd, args);
			} else {
				message.send(dmapi.getShopTag() + " You must be in a shop area to use that command.");
				return true;
			}
		} else {
			// Used a shop location command
			String username = ((Player) sender).getName();
			
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
					if (locationManager.add(shop)) {
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
				Integer id = locationManager.getShopId(((Player) sender).getLocation());
				if (id < 0) {
					message.send(dmapi.getShopTag() + " You are not in a shop area.");
				} else {
					message.send(dmapi.getShopTag() + " You are in shop ID: " + Integer.toString(id));
				}
				return true;
			} else if (args.length == 2 && args[1].equalsIgnoreCase("delete") && dmapi.hasPermission(sender, "admin.location.delete")) {
				Integer id = locationManager.getShopId(((Player) sender).getLocation());
				if (id < 0) {
					message.send(dmapi.getShopTag() + " No shop found. Specify an ID or stand in a shop.");
				} else if (locationManager.removeShop(id)) {
					message.send(dmapi.getShopTag() + " Shop removed.");
				} else {
					message.send(dmapi.getShopTag() + " Error removing shop ID: " + Integer.toString(id));
				}
				return true;
			} else if (args.length == 2 && args[1].equalsIgnoreCase("delete") && dmapi.hasPermission(sender, "admin.location.delete")) {
				Integer id;
				try {
					id = Integer.parseInt(args[2]);
				} catch (NumberFormatException ex) {
					message.send(dmapi.getShopTag() + " Invalid shop ID: " + args[2]);
					return true;
				}
				
				if (locationManager.removeShop(id)) {
					message.send(dmapi.getShopTag() + " Shop removed.");
				} else {
					message.send(dmapi.getShopTag() + " Error removing shop ID: " + Integer.toString(id));
				}
				return true;
			} else if (args.length == 2 && args[1].equalsIgnoreCase("list") && dmapi.hasPermission(sender, "location.list")) {
				message.send("Shop IDs: " + locationManager.listShops());
				return true;
			} else if (args.length == 2 && args[1].equalsIgnoreCase("enable") && dmapi.hasPermission(sender, "admin.location.toggle")) {
				locationManager.enableLocations();
				message.send(dmapi.getShopTag() + " Shop locations enabled.");
				return true;
			} else if (args.length == 2 && args[1].equalsIgnoreCase("disable") && dmapi.hasPermission(sender, "admin.location.toggle")) {
				locationManager.disableLocations();
				message.send(dmapi.getShopTag() + " Shop locations disabled.");
				return true;
			} else if (args.length == 3 && args[1].equalsIgnoreCase("tp") && dmapi.hasPermission(sender, "admin.location.tp")) {
				Integer id;
				try {
					id = Integer.parseInt(args[2]);
				} catch (NumberFormatException ex) {
					message.send(dmapi.getShopTag() + " Invalid shop ID: " + args[2]);
					return true;
				}
				
				Location dest = locationManager.getShopLocation(id);
				if (dest == null) {
					message.send(dmapi.getShopTag() + " Shop ID " + Integer.toString(id) + " not found.");
				} else {
					((Player) sender).teleport(dest);
				}
				return true;
			} else {
				if (dmapi.hasPermission(sender, "admin.location.create")) {
					message.send("{CMD} /shop location set {BKT}- {}Sets the two corners of the shop");
					message.send("{CMD} /shop location create {BKT}- {}Creates a shop location from two selected points");
					message.send("{CMD} /shop location cancel {BKT}- {}Cancels setting a shop location");
				}
				if (dmapi.hasPermission(sender, "location.check")) {
					message.send("{CMD} /shop location check {BKT}- {}Checks ID of current location");
				}
				if (dmapi.hasPermission(sender, "admin.location.delete")) {
					message.send("{CMD} /shop location delete {BKT}[{PRM}ID{BKT}] - {}Removes specified shop ID. Removes shop you are in if no shop specified");
				}
				if (dmapi.hasPermission(sender, "admin.location.toggle")) {
					message.send("{CMD} /shop location enable {BKT}- {}Enables location based shops");
					message.send("{CMD} /shop location disable {BKT}- {}Disable location based shops");
				}
				if (dmapi.hasPermission(sender, "admin.location.list")) {
					message.send("{CMD} /shop location list {BKT}- {}Lists shop IDs");
				}
				if (dmapi.hasPermission(sender, "admin.location.tp")) {
					message.send("{CMD} /shop location tp {PRM}<ID> {BKT}- {}Teleports to specified shop ID");
				}
				return true;
			}
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
}
