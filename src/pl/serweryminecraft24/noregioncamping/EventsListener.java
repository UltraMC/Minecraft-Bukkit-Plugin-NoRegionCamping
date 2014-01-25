package pl.serweryminecraft24.noregioncamping;

import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mewin.WGRegionEvents.events.RegionEnterEvent;
import com.mewin.WGRegionEvents.events.RegionEnteredEvent;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class EventsListener implements Listener {
	private PluginNoRegionCamping plugin;
	public HashMap<String, PlayerRecord> playerRecords = new HashMap<String, PlayerRecord>();
	public HashMap<String, PlayerLocationRecord> playerLocationRecords = new HashMap<String, PlayerLocationRecord>();
	public HashMap<String, PlayerFightingTimerTask> fightingTimers = new HashMap<String, PlayerFightingTimerTask>();
	public HashMap<String, PlayersLocationTimerTask> locationTimers = new HashMap<String, PlayersLocationTimerTask>();

	public EventsListener(PluginNoRegionCamping plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName().toLowerCase();

		if (!this.playerRecords.containsKey(playerName)) {
			return;
		}

		String command = event.getMessage().toLowerCase().split(" ")[0];

		List<String> blacklist = this.plugin.getConfig().getStringList("PluginNoRegionCamping.blockedCommands");

		for (int i = 0; i < blacklist.size(); i++) {
			if (command.equalsIgnoreCase(blacklist.get(i))) {
				event.setCancelled(true);

				player.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.commandCanceled"));
			}
		}

	}

	@EventHandler
	public void onRegionEnter(RegionEnterEvent event) {
		String regionName = event.getRegion().getId().toString().toLowerCase();
		Player player = event.getPlayer();

		if (!this.playerRecords.containsKey(player.getName().toLowerCase())) {
			return;
		}

		List<String> blacklist = this.plugin.getConfig().getStringList("PluginNoRegionCamping.fightFreeRegions");

		for (int i = 0; i < blacklist.size(); i++) {
			if (regionName.equals((String) blacklist.get(i)) && event.isCancellable()) {
				if (plugin.getConfig().getBoolean("PluginNoRegionCamping.messages.stillFighting")) {
					player.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.stillFighting"));
				}

				player.teleport(this.playerRecords.get(player.getName().toLowerCase()).enemyPlayer.getLocation());

				break;
			}
		}
	}

	@EventHandler
	public void onRegionEntered(RegionEnteredEvent event) {
		Player player = event.getPlayer();

		if (!this.playerRecords.containsKey(player.getName().toLowerCase())) {
			return;
		}

		player.teleport(this.playerLocationRecords.get(player.getName().toLowerCase()).location);
	}

	@EventHandler
	public void onEntityFight(EntityDamageByEntityEvent event) {
		Entity reciever = event.getEntity();
		Entity attacker = event.getDamager();

		if (!(reciever instanceof Player) || !(attacker instanceof Player)) {
			return;
		}

		Player attackerPlayer = (Player) attacker;
		Player recieverPlayer = (Player) reciever;

		List<String> blacklist = this.plugin.getConfig().getStringList("PluginNoRegionCamping.fightFreeRegions");

		for (int i = 0; i < blacklist.size(); i++) {
			if (isWithinRegion(attackerPlayer, blacklist.get(i)) || isWithinRegion(recieverPlayer, blacklist.get(i))) {
				event.setCancelled(true);

				return;
			}
		}

		if (!this.fightingTimers.containsKey(attackerPlayer.getName().toLowerCase())) {
			if (plugin.getConfig().getBoolean("PluginNoRegionCamping.killLoggers")) {
				attackerPlayer.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.amIFightingTrueKillLoggersOn"));
				recieverPlayer.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.amIFightingTrueKillLoggersOn"));
			} else {
				attackerPlayer.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.amIFightingTrueKillLoggersOff"));
				recieverPlayer.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.amIFightingTrueKillLoggersOff"));
			}
		}

		setFightingTimer(setPlayerRecord(attackerPlayer, recieverPlayer));
		setFightingTimer(setPlayerRecord(recieverPlayer, attackerPlayer));
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName().toLowerCase();

		if (this.playerLocationRecords.containsKey(playerName)) {
			return;
		}

		PlayerLocationRecord newPlayerLocationRecord = new PlayerLocationRecord();
		newPlayerLocationRecord.player = player;
		newPlayerLocationRecord.name = playerName;
		newPlayerLocationRecord.location = player.getLocation();

		this.playerLocationRecords.put(newPlayerLocationRecord.name, newPlayerLocationRecord);

		setLocationTimer(newPlayerLocationRecord);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		String playerName = event.getPlayer().getName().toLowerCase();

		if (this.playerRecords.containsKey(playerName)) {
			if (this.plugin.getConfig().getBoolean("PluginNoRegionCamping.killLoggers")) {

				if (this.plugin.getConfig().getBoolean("PluginNoRegionCamping.broadcastThatPlayerIsPvPLogger")) {
					this.plugin.getServer().broadcastMessage(String.format(ChatColor.getByChar(this.plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + this.plugin.getConfig().getString("PluginNoRegionCamping.language.broadcastPvPLogger"), event.getPlayer().getName()));
				}

				event.getPlayer().setHealth(0.0);
			}

			this.fightingTimers.get(playerName).cancel();
			this.fightingTimers.remove(playerName);
			this.playerRecords.remove(playerName);
		}

		if (this.locationTimers.containsKey(playerName)) {
			this.locationTimers.get(playerName).cancel();
			this.locationTimers.remove(playerName);
			this.playerLocationRecords.remove(playerName);
		}
	}

	public void setFightingTimer(PlayerRecord playerRecord) {
		if (!playerRecord.isFighting) {
			return;
		}

		if (this.fightingTimers.containsKey(playerRecord.name)) {
			this.fightingTimers.get(playerRecord.name).cancel();
		}

		PlayerFightingTimerTask fightTimeTask = new PlayerFightingTimerTask(this.plugin, this, playerRecord);
		fightTimeTask.runTaskTimer(this.plugin, 0L, Integer.valueOf(1));

		this.fightingTimers.put(playerRecord.name, fightTimeTask);
	}

	public void setLocationTimer(PlayerLocationRecord playerRecord) {
		if (this.locationTimers.containsKey(playerRecord.name)) {
			this.locationTimers.get(playerRecord.name).cancel();
		}

		PlayersLocationTimerTask playersLocationTimerTask = new PlayersLocationTimerTask(plugin, playerRecord);
		playersLocationTimerTask.runTaskTimer(this.plugin, 0L, Integer.valueOf(50));

		this.locationTimers.put(playerRecord.name, playersLocationTimerTask);
	}

	public PlayerRecord setPlayerRecord(Player player, Player enemyPlayer) {
		PlayerRecord playerRecord = new PlayerRecord();

		playerRecord.player = player;
		playerRecord.enemyPlayer = enemyPlayer;
		playerRecord.name = player.getName().toLowerCase();
		playerRecord.isFighting = true;
		playerRecord.fightingTimeLeft = Integer.valueOf(this.plugin.getConfig().getInt("PluginNoRegionCamping.fightTime"));

		this.playerRecords.put(playerRecord.name, playerRecord);

		return playerRecord;
	}

	public void removePlayerRecord(PlayerRecord fightingPlayer) {
		String playerName = fightingPlayer.name.toLowerCase();

		if (this.playerRecords.containsKey(playerName)) {
			this.playerRecords.remove(playerName);
			this.fightingTimers.remove(playerName);
		}

	}

	public boolean isWithinRegion(Player player, String regionName) {
		RegionManager regionManager = this.plugin.getWorldGuard().getRegionManager(player.getWorld());
		ApplicableRegionSet regionSet = regionManager.getApplicableRegions(player.getLocation());

		for (ProtectedRegion region : regionSet) {
			String thisName = region.getId();

			if (thisName.equals(regionName)) {
				return true;
			}
		}

		return false;
	}
}