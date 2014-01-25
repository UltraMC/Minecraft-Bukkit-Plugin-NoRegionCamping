package pl.serweryminecraft24.noregioncamping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class PluginNoRegionCamping extends JavaPlugin {
	private EventsListener eventsListener;
	private PluginNoRegionCamping plugin;
	private boolean isPluginEnabled;
	private Logger logger;
	public HashMap<String, PlayerRecord> playerRecords;
	public HashMap<String, PlayerLocationRecord> playerLocationRecords;

	public void onEnable() {
		this.logger = getLogger();

		if (!loadConfig()) {
			return;
		}

		this.isPluginEnabled = getConfig().getBoolean("PluginNoRegionCamping.enabled");

		if (!this.isPluginEnabled) {
			this.logger.info("Plugin is disabled in confing. Turning it off.");

			getServer().getPluginManager().disablePlugin(this);

			return;
		}

		this.plugin = this;
		this.plugin.logger.info("Number of loaded fight-free regions: " + this.plugin.getConfig().getStringList("PluginNoRegionCamping.fightFreeRegions").size());

		this.eventsListener = new EventsListener(this);
		getServer().getPluginManager().registerEvents(this.eventsListener, this);

		this.playerRecords = this.plugin.eventsListener.playerRecords;
		this.playerLocationRecords = this.plugin.eventsListener.playerLocationRecords;

		List<Player> playersList = Arrays.asList(Bukkit.getServer().getOnlinePlayers());

		for (int i = 0; i < playersList.size(); i++) {
			Player player = playersList.get(i);
			String playerName = player.getName().toLowerCase();

			if (!this.playerLocationRecords.containsKey(playerName)) {
				PlayerLocationRecord newRecord = new PlayerLocationRecord();
				newRecord.name = playerName;
				newRecord.player = player;
				newRecord.location = player.getLocation();

				this.playerLocationRecords.put(playerName, newRecord);
				this.eventsListener.setLocationTimer(newRecord);
			}
		}
	}

	public void onDisable() {
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (playerRecords == null) {
			return false;
		}

		if (cmd.getName().equalsIgnoreCase("amifighting") || cmd.getName().equalsIgnoreCase("af")) {
			if (!playerRecords.containsKey(sender.getName().toLowerCase())) {
				sender.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.amIFightingFalse"));

				return true;
			}

			if (this.plugin.getConfig().getBoolean("PluginNoRegionCamping.killLoggers")) {
				sender.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.amIFightingTrueKillLoggersOn"));
			} else {
				sender.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.amIFightingTrueKillLoggersOff"));
			}

			return true;
		}

		return false;
	}

	private boolean loadConfig() {
		FileConfiguration config = getConfig();

		List<String> fightFreeRegionsList = new ArrayList<String>();
		fightFreeRegionsList.add("spawn");
		fightFreeRegionsList.add("creative");

		List<String> blockedCommandsList = new ArrayList<String>();
		blockedCommandsList.add("/s");
		blockedCommandsList.add("/spawn");
		blockedCommandsList.add("/tp");
		blockedCommandsList.add("/tpa");
		blockedCommandsList.add("/w");
		blockedCommandsList.add("/warp");
		blockedCommandsList.add("/v");
		blockedCommandsList.add("/vanish");

		config.addDefault("PluginNoRegionCamping.enabled", Boolean.valueOf(true));
		config.addDefault("PluginNoRegionCamping.fightTime", Integer.valueOf(20));
		config.addDefault("PluginNoRegionCamping.killLoggers", Boolean.valueOf(false));
		config.addDefault("PluginNoRegionCamping.broadcastThatPlayerIsPvPLogger", Boolean.valueOf(true));
		config.addDefault("PluginNoRegionCamping.fightFreeRegions", fightFreeRegionsList);
		config.addDefault("PluginNoRegionCamping.blockedCommands", blockedCommandsList);
		config.addDefault("PluginNoRegionCamping.messages.color", String.valueOf("d"));
		config.addDefault("PluginNoRegionCamping.messages.stillFighting", Boolean.valueOf(true));
		config.addDefault("PluginNoRegionCamping.messages.endOfFight", Boolean.valueOf(true));
		config.addDefault("PluginNoRegionCamping.language.stillFighting", String.valueOf("You're fighting, can't let you in fight-free region!"));
		config.addDefault("PluginNoRegionCamping.language.endOfFight", String.valueOf("Fight is finished, you're now free to visit fight-free regions."));
		config.addDefault("PluginNoRegionCamping.language.commandCanceled", String.valueOf("You're fighting, can't let you use that command!"));
		config.addDefault("PluginNoRegionCamping.language.amIFightingTrueKillLoggersOff", String.valueOf("You're fighting right now!"));
		config.addDefault("PluginNoRegionCamping.language.amIFightingTrueKillLoggersOn", String.valueOf("You're fighting right now! Don't log off or you'll die!"));
		config.addDefault("PluginNoRegionCamping.language.amIFightingFalse", String.valueOf("You're not fighting and can log off safely."));
		config.addDefault("PluginNoRegionCamping.language.broadcastPvPLogger", String.valueOf("%s logged out while PvP fighting and he will wake up dead."));

		config.options().copyDefaults(true);

		saveConfig();

		if (config.getInt("PluginNoRegionCamping.fightTime") < 1) {
			this.logger.info("Plugin is disabled doe the fightTime < 1 in config. Turning it off.");

			getServer().getPluginManager().disablePlugin(this);

			return false;
		}

		return true;
	}

	public WorldGuardPlugin getWorldGuard() {
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

		if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
			return null;
		}

		return (WorldGuardPlugin) plugin;
	}

}
