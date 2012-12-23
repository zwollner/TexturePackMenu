package com.wimbli.TexturePackMenu;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

public class TexturePackMenu extends JavaPlugin {

	@Override
	public void onEnable() {
		getConfig().options().copyDefaults(true);
		this.saveConfig();
		Config.load(this);
		getCommand("texture").setExecutor(new TPMCommand(this));
		getServer().getPluginManager().registerEvents(new TPMListener(), this);
	}

	@Override
	public void onDisable() {
		Config.savePlayerPacks();
	}

	protected final void log(Level lvl, String text) {
		this.getLogger().log(lvl, text);
	}

	protected final void log(String text) {
		log(Level.INFO, text);
	}

	protected final void logWarn(String text) {
		log(Level.WARNING, text);
	}

	protected final void logConfig(String text) {
		log(Level.INFO, "[CONFIG] " + text);
	}
}
