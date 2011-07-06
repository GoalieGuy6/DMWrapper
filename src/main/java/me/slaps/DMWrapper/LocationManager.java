package me.slaps.DMWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

import me.slaps.DMWrapper.ShopLocation;

import org.bukkit.Location;
import org.bukkit.util.config.Configuration;

public class LocationManager extends ILocationManager {
	
	private ArrayList<ShopLocation> shops;
	
	public LocationManager(DMWrapper instance) {
		shops = new ArrayList<ShopLocation>();
		
		plugin = instance;
		
		configFile = new File(plugin.getDataFolder(), "shops-dmwrapper.yml");
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
			
			Location loc1 = new Location(plugin.getServer().getWorld(shopKeys.get("world")),
					Integer.parseInt(shopKeys.get("corner1x")),
					Integer.parseInt(shopKeys.get("corner1y")),
					Integer.parseInt(shopKeys.get("corner1z")));
			
			Location loc2 = new Location(plugin.getServer().getWorld(shopKeys.get("world")),
					Integer.parseInt(shopKeys.get("corner2x")),
					Integer.parseInt(shopKeys.get("corner2y")),
					Integer.parseInt(shopKeys.get("corner2z")));
			
			add(new ShopLocation(Integer.parseInt(shopKeys.get("id")), loc1, loc2));
		}
		
		saveConfig();
	}
	
	public void saveConfig() {
		config = new Configuration(configFile);
		
		ArrayList<LinkedHashMap<String, Object>> tempShops = new ArrayList<LinkedHashMap<String, Object>>();
		
		config.setProperty("enabled", locationsEnabled);
		config.setProperty("type", "dmwrapper");
		
		Iterator<ShopLocation> itr = shops.iterator();
		while (itr.hasNext()) {
			ShopLocation shop = itr.next();
			LinkedHashMap<String, Object> tempMap = new LinkedHashMap<String, Object>();
			
			tempMap.put("id", shop.id);
			tempMap.put("world", shop.world.getName());
			tempMap.put("corner1x", shop.loc1.getBlockX());
			tempMap.put("corner1y", shop.loc1.getBlockY());
			tempMap.put("corner1z", shop.loc1.getBlockZ());
			tempMap.put("corner2x", shop.loc2.getBlockX());
			tempMap.put("corner2y", shop.loc2.getBlockY());
			tempMap.put("corner2z", shop.loc2.getBlockZ());
			
			tempShops.add(tempMap);
		}
		
		config.setProperty("shops", tempShops);
		config.save();
	}

	public boolean add(ShopLocation shop) {
		if (shop.id == null) {
			shop.id = getNextId();
		}
		shops.add(shop);
		saveConfig();
		return true;
	}
	
	public boolean removeShop(Integer id) {
		int i = 0;
		Iterator<ShopLocation> itr = shops.iterator();
		
		while (itr.hasNext()) {
			ShopLocation tmp = itr.next();
			if (id == tmp.id) {
				shops.remove(i);
				saveConfig();
				return true;
			}
			i++;
		}
		
		return false;
	}
	
	@Override
	public String listShops() {
		String list = "";
		Iterator<ShopLocation> itr = shops.iterator();
		
		while (itr.hasNext()) {
			ShopLocation tmp = itr.next();
			list += Integer.toString(tmp.id);
			list += itr.hasNext() ? ", " : "";
		}
		
		return list;
	}
	
	public Location getShopLocation(Integer id) {
		Iterator<ShopLocation> itr = shops.iterator();
		
		while (itr.hasNext()) {
			ShopLocation tmp = itr.next();
			if (id == tmp.id) {
				return new Location(tmp.world, tmp.loc1.getBlockX(), tmp.loc1.getBlockY() + 1, tmp.loc1.getBlockZ());
			}
		}
		
		return null;
	}
	
	public Integer getNextId() {
		Integer i = 0;
		Iterator<ShopLocation> itr = shops.iterator();
		
		while (itr.hasNext()) {
			
			ShopLocation tmp = itr.next();
			if (tmp.id > 0) i = tmp.id;
		}
		
		return i + 1;
	}
	
	public Integer getShopId(Location loc) {
		Iterator<ShopLocation> itr = shops.iterator();
		
		while (itr.hasNext()) {
			ShopLocation tmp = itr.next();
			if (tmp.inShop(loc)) return tmp.id;
		}
		
		return -1;
	}
	
	@Override
	public boolean inShop(Location loc) {
		Iterator<ShopLocation> itr = shops.iterator();
		
		while (itr.hasNext()) {
			ShopLocation tmp = itr.next();
			if (tmp.inShop(loc)) return true;
		}
		
		return false;
	}
}
