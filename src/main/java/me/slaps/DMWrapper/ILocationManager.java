package me.slaps.DMWrapper;

import java.io.File;

import org.bukkit.Location;
import org.bukkit.util.config.Configuration;

public abstract class ILocationManager {
	
	protected DMWrapper plugin;
	
	protected File configFile;
	protected Configuration config;
	
	protected boolean locationsEnabled;
	
	public void enableLocations() {
		locationsEnabled = true;
		saveConfig();
	}
	
	public void disableLocations() {
		locationsEnabled = false;
		saveConfig();
	}
	
	public boolean enabled() {
		return locationsEnabled;
	}
	
	protected abstract void loadConfig();
	
	protected abstract void saveConfig();
		
	public abstract String listShops();
			
	public abstract boolean inShop(Location loc);
}
