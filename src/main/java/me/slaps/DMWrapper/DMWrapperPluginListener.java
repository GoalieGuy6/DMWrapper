package me.slaps.DMWrapper;

import com.gmail.haloinverse.DynamicMarket.DynamicMarket;
import com.gmail.haloinverse.DynamicMarket.DynamicMarketAPI;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.PluginManager;

public class DMWrapperPluginListener extends ServerListener {
	
	private DMWrapper plugin;
	private String dmName = "DynamicMarket";
	
	public DMWrapperPluginListener(DMWrapper instance) {
		this.plugin = instance;
	}
	
	@Override
	public void onPluginDisable(PluginDisableEvent event) {
		if (event.getPlugin().getDescription().getName().equals(dmName)) {
			plugin.setAPI(null);
		}
		
		plugin.disable();
	}
	
	@Override
	public void onPluginEnable(PluginEnableEvent event) {
		if (event.getPlugin().getDescription().getName().equals(dmName)) {
			hookDM((DynamicMarket) event.getPlugin());
		}
	}
	
	public void checkPlugins(PluginManager pm) {
		if (pm.getPlugin("DynamicMarket").isEnabled() && plugin.getAPI() == null) {
			hookDM((DynamicMarket) pm.getPlugin("DynamicMarket"));
		}
	}
	
	public void hookDM(DynamicMarket dm) {
		DynamicMarketAPI api = dm.getAPI();
		plugin.setAPI((DynamicMarketAPI) api);
		plugin.hookAPI();
	}
}