package com.wimbli.TexturePackMenu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.io.CRCStore;
import org.getspout.spoutapi.player.SpoutPlayer;

public class Config {

	private static TexturePackMenu plugin;
	private static File playerFile;
	private static YamlConfiguration playerConfig;
	private static final String PLAYER_FILE_NAME = "players.yml";
	private static final String PLAYER_CONFIG_SECTION = "PlayerTextures";

	private static Map<String, String> texturePacks = new LinkedHashMap<String, String>(); // (Pack Name, URL)
	private static Map<String, String> playerPacks = new HashMap<String, String>(); // (Player Name, Pack Name)

	private static final byte[] CRC_BUFFER = new byte[16384];

	// load config
	public static void load(final TexturePackMenu master) {
		plugin = master;
		playerConfig = getPlayerCofig();
		loadTexturePackList();
		loadPlayerPacks();
	}

	private static YamlConfiguration getPlayerCofig() {
		playerFile = new File(plugin.getDataFolder(), PLAYER_FILE_NAME);
		if (!playerFile.getParentFile().exists() || !playerFile.exists()) {
			try {
				plugin.log("Creating: " + playerFile.getPath());
				playerFile.createNewFile();
			} catch (IOException e) {
				plugin.log(Level.WARNING, "Error creating: " + playerFile.getPath());
			}
		}
		return YamlConfiguration.loadConfiguration(playerFile);
	}

	public static List<String> texPackNames() {
		return new ArrayList<String>(texturePacks.keySet());
	}

	public static List<String> texPackURLs() {
		return new ArrayList<String>(texturePacks.values());
	}

	public static int texturePackCount() {
		return texturePacks.size();
	}

	public static String getPack(final String playerName) {
		if (!playerPacks.containsKey(playerName.toLowerCase())) {
			return "";
		}

		return playerPacks.get(playerName.toLowerCase());
	}

	public static void setPack(SpoutPlayer sPlayer, final String packName) {
		if (!texturePacks.containsKey(packName)) {
			setPack(sPlayer, 0);
			if (sPlayer.hasPermission("texturepackmenu.texture")) {
				sPlayer.sendNotification("Texture packs available", "Use command: " + ChatColor.AQUA + "/texture",
						new ItemStack(Material.PAPER), 10000);
			}
		} else {
			setPlayerTexturePack(sPlayer, packName);
		}
	}

	public static void setPack(SpoutPlayer sPlayer, int index) {
		if (texturePacks.size() < index - 1) {
			index = 0;
		}
		setPlayerTexturePack(sPlayer, texPackNames().get(index));
	}

	public static void setPackDelayed(final SpoutPlayer sPlayer, final String packName) {
		plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
			public void run() {
				setPlayerTexturePack(sPlayer, packName);
			}
		}, 2);
	}

	public static void setPackDelayed(SpoutPlayer sPlayer, int index) {
		if (texturePacks.size() < index - 1) {
			index = 0;
		}
		setPackDelayed(sPlayer, texPackNames().get(index));
	}

	private static void setPlayerTexturePack(SpoutPlayer sPlayer, String packName) {
		if (sPlayer == null || !sPlayer.isOnline()) {
			return;
		}

		if (!isValidPackName(packName)) {
			// Get default name
			packName = texPackNames().get(0);
		}
		String packURL = texturePacks.get(packName);

		// IF URL is empty after getting default, then it's the players choice
		// Let them know others are available if they have the permissions.
		if (packURL == null || packURL.trim().isEmpty()) {
			sPlayer.resetTexturePack();
			if (sPlayer.hasPermission("texturepackmenu.texture")) {
				sPlayer.sendNotification("Texture packs available", "Use command: " + ChatColor.AQUA + "/texture",
						new ItemStack(Material.PAPER), 10000);
			}
		} else {
			sPlayer.sendNotification(
					plugin.getConfig().getString("settings.notification.title", "Loading texture pack..."), packName,
					new ItemStack(Material.PAINTING), plugin.getConfig().getInt("settings.notification.delay", 10000));
			storePlayerTexturePack(sPlayer.getName(), packName);

			sPlayer.setTexturePack(packURL);

			// make sure it checks out as valid, by getting CRC value for it
			Long crc = null;
			crc = CRCStore.getCRC(packURL, CRC_BUFFER);
			if (crc == null || crc == 0) {
				plugin.logWarn("Bad CRC value for texture pack. It is probably an invalid URL: " + packURL);
			}
		}
	}

	private static boolean isValidPackName(String packName) {
		if (packName == null || packName.isEmpty()) {
			return false;
		}
		if (texPackNames().contains(packName)) {
			return true;
		}
		return false;
	}

	public static void storePlayerTexturePack(final String playerName, final String packName) {
		playerPacks.put(playerName.toLowerCase(), packName);
	}

	public static void resetPlayerTexturePack(final String playerName) {
		playerPacks.remove(playerName.toLowerCase());

		Player player = plugin.getServer().getPlayer(playerName);
		if (player != null) {
			SpoutPlayer sPlayer = SpoutManager.getPlayer(player);
			if (sPlayer != null && sPlayer.isSpoutCraftEnabled()) {
				setPack(sPlayer, 0);
			}
		}
	}

	public static void loadTexturePackList() {
		int packCount = 0;
		for (Entry<String, Object> entry : plugin.getConfig().getConfigurationSection("TexturePacks").getValues(false)
				.entrySet()) {
			texturePacks.put(entry.getKey(), entry.getValue().toString());
			packCount++;
		}
		plugin.log("Found " + packCount + " configured texture packs.");
	}

	// load player data
	private static void loadPlayerPacks() {
		if (!playerFile.getParentFile().exists() || !playerFile.exists()) {
			plugin.log("Error locating " + PLAYER_FILE_NAME);
			return;
		}
		ConfigurationSection section = playerConfig.getConfigurationSection(PLAYER_CONFIG_SECTION);
		if (section != null && !section.getValues(false).isEmpty()) {
			for (Entry<String, Object> entry : section.getValues(false).entrySet()) {
				playerPacks.put(entry.getKey(), (String) entry.getValue());
			}
		}
	}

	// save player data
	public static void savePlayerPacks() {
		if (!playerFile.getParentFile().exists()) {
			return;
		}
		playerConfig.createSection(PLAYER_CONFIG_SECTION, playerPacks);
		try {
			playerConfig.save(playerFile);
		} catch (IOException e) {
			plugin.logWarn("Error saving: " + playerFile.getPath());
		}
	}
}
