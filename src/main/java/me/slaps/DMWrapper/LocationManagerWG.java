package me.slaps.DMWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.config.Configuration;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class LocationManagerWG extends ILocationManager {

	private WorldGuardPlugin worldguard;
	private RegionManager regionmanager;
	
	private ArrayList<HashMap<String, String>> shops;
	
	public LocationManagerWG(DMWrapper instance, WorldGuardPlugin wg) {		
		worldguard = wg;
		shops = new ArrayList<HashMap<String, String>>();
		
		plugin = instance;
		
		configFile = new File(plugin.getDataFolder(), "shops-worldguard.yml");
		config = new Configuration(configFile);
		
		if (!configFile.exists()) {
			saveConfig();
		}
		
		loadConfig();
	}
	
	public void loadConfig() {
		config.load();
		
		locationsEnabled = config.getBoolean("enabled", false);
		List<String> shopList = config.getStringList("shops", null);
		
		Iterator<String> itr = shopList.iterator();
		while (itr.hasNext()) {
			String shop = itr.next();
			
			LinkedHashMap<String, String> shopKeys = new LinkedHashMap<String, String>();
			StringTokenizer st = new StringTokenizer(shop, "{}=, ");
			while(st.hasMoreTokens()) {
				shopKeys.put(st.nextToken(), st.nextToken());
			}
			
			String world = shopKeys.get("world");
			String region = shopKeys.get("region");
			add(world, region);
		}
		
		saveConfig();
	}
	
	public void saveConfig() {
		config = new Configuration(configFile);
		
		ArrayList<LinkedHashMap<String, String>> tempShops = new ArrayList<LinkedHashMap<String, String>>();
		
		config.setProperty("enabled", locationsEnabled);
		config.setProperty("type", "worldguard");
		
		Iterator<HashMap<String, String>> itr = shops.iterator();
		while (itr.hasNext()) {
			HashMap<String, String> shop = itr.next();
			LinkedHashMap<String, String> tempMap = new LinkedHashMap<String, String>();
			
			tempMap.put("world", shop.get("world"));
			tempMap.put("region", shop.get("region"));
			
			tempShops.add(tempMap);
		}
		
		config.setProperty("shops", tempShops);
		config.save();
	}

	public boolean add(String world, String region) {
		regionmanager = worldguard.getRegionManager(plugin.getServer().getWorld(world));
		if (regionmanager.getRegion(region) == null) {
			return false;
		}
		HashMap<String, String> tmp = new HashMap<String, String>();
		tmp.put("world", world);
		tmp.put("region", region);
		shops.add(tmp);
		
		saveConfig();
		return true;
	}

	public boolean removeShop(String world, String id) {
		int i = 0;
		
		Iterator<HashMap<String, String>> itr = shops.iterator();
		
		while (itr.hasNext()) {
			HashMap<String, String> shop = itr.next();
			if (shop.get("world").equalsIgnoreCase("world") && shop.get("region").equalsIgnoreCase(id)) {
				shops.remove(i);
				saveConfig();
				return true;
			}
			i++;
		}
		
		saveConfig();
		return false;
	}

	@Override
	public String listShops() {
		String list = "";
		Iterator<HashMap<String, String>> itr = shops.iterator();
		
		while (itr.hasNext()) {
			HashMap<String, String> shop = itr.next();
			list += shop.get("region") + " (" + shop.get("world") + ")";
			list += itr.hasNext() ? ", " : "";
		}
		
		return list;
	}

	public Location getShopLocation(String world, String id) {
		World w = plugin.getServer().getWorld("world");
		regionmanager = worldguard.getRegionManager(w);
		ProtectedRegion region = regionmanager.getRegion(id);
		
		if (region == null) {
			return null;
		}
		
		Vector point = region.getMinimumPoint();
		Location loc = new Location(w, point.getBlockX(), point.getBlockY() + 1, point.getBlockZ());
		
		return loc;
	}

	public String getShopId(Location loc) {
		World w = loc.getWorld();
		regionmanager = worldguard.getRegionManager(w);
		Vector point = new Vector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		List<String> regions = regionmanager.getApplicableRegionsIDs(point);
		
		Iterator<HashMap<String, String>> itr = shops.iterator();
		
		while (itr.hasNext()) {
			HashMap<String, String> shop = itr.next();
			if (regions.contains(shop.get("region"))) {
				return shop.get("region");
			}
		}
		
		return null;
	}

	@Override
	public boolean inShop(Location loc) {
		return (getShopId(loc) != null);
	}
}
