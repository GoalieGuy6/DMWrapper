package com.gmail.goalieguy6.DMWrapper;

import org.bukkit.Location;
import org.bukkit.World;

public class ShopLocation {
	public boolean set;
	public Integer id;
	
	public World world;
	public Location loc1;
	public Location loc2;
	private Location LocMin;
	private Location LocMax;
	
	public ShopLocation() {
		
	}
	
	public ShopLocation(Location loc) {
		loc1 = loc;
	}
	
	public ShopLocation(Integer id, Location loc1, Location loc2) {
		this.id = id;
		this.loc1 = loc1;
		this.loc2 = loc2;
		computeNewBlock();
	}
	
	public boolean setLocation(Integer id, Location loc) {
		if (id == 1) {
			loc1 = loc;
		} else {
			loc2 = loc;
		}
		
		return computeNewBlock();
	}
	
	private boolean computeNewBlock() {
		if (loc1 == null || loc2 == null) {
			return false;
		}
		
		if (loc1.getWorld() != loc2.getWorld()) {
			return false;
		}
		
		int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
		int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
		int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
		int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
		int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
		int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
		
		while (maxY - minY < 2) maxY += 1;
		
		world = loc1.getWorld();
		
		LocMin = new Location(world, minX, minY, minZ);
		LocMax = new Location(world, maxX, maxY, maxZ);
				
		set = true;
		return true;
	}
	
	public boolean inShop(Location loc) {
		if (LocMin == null || LocMax == null) {
			return false;
		}
		
		if (!loc.getWorld().equals(world)) {
			return false;
		}
		
		return (loc.getBlockX() >= LocMin.getBlockX() && 
				loc.getBlockY() >= LocMin.getBlockY() &&
				loc.getBlockZ() >= LocMin.getBlockZ() &&
				loc.getBlockX() <= LocMax.getBlockX() &&
				loc.getBlockY() <= LocMax.getBlockY() &&
				loc.getBlockZ() <= LocMax.getBlockZ());
	}
	
	public boolean intersects(ShopLocation shop) {
		if (!shop.world.equals(world)) {
			return false;
		}
		
		int maxX = shop.LocMax.getBlockX();
		int maxY = shop.LocMax.getBlockY();
		int maxZ = shop.LocMax.getBlockZ();
		int minX = shop.LocMin.getBlockX();
		int minY = shop.LocMin.getBlockY();
		int minZ = shop.LocMin.getBlockZ();
		
		if (inShop(new Location(world, maxX, maxY, maxZ))) return true;
		if (inShop(new Location(world, maxX, maxY, minZ))) return true;
		if (inShop(new Location(world, maxX, minY, maxZ))) return true;
		if (inShop(new Location(world, maxX, minY, minZ))) return true;
		if (inShop(new Location(world, minX, maxY, maxZ))) return true;
		if (inShop(new Location(world, minX, maxY, minZ))) return true;
		if (inShop(new Location(world, minX, minY, maxZ))) return true;
		if (inShop(new Location(world, minX, minY, minZ))) return true;
		
		maxX = LocMax.getBlockX();
		maxY = LocMax.getBlockY();
		maxZ = LocMax.getBlockZ();
		minX = LocMin.getBlockX();
		minY = LocMin.getBlockY();
		minZ = LocMin.getBlockZ();
		
		if (shop.inShop(new Location(world, maxX, maxY, maxZ))) return true;
		if (shop.inShop(new Location(world, maxX, maxY, minZ))) return true;
		if (shop.inShop(new Location(world, maxX, minY, maxZ))) return true;
		if (shop.inShop(new Location(world, maxX, minY, minZ))) return true;
		if (shop.inShop(new Location(world, minX, maxY, maxZ))) return true;
		if (shop.inShop(new Location(world, minX, maxY, minZ))) return true;
		if (shop.inShop(new Location(world, minX, minY, maxZ))) return true;
		if (shop.inShop(new Location(world, minX, minY, minZ))) return true;
		
		return false;
	}
}
