package pl.serweryminecraft24.noregioncamping;

import org.bukkit.scheduler.BukkitRunnable;

@SuppressWarnings("unused")
public class PlayersLocationTimerTask extends BukkitRunnable {
	private PluginNoRegionCamping plugin;
	private PlayerLocationRecord playerLocationRecord;
	private long lastIntervalTime = 0L;
	private long elapsedTime = 0L;

	public PlayersLocationTimerTask(PluginNoRegionCamping plugin, PlayerLocationRecord playerLocationRecord) {
		this.plugin = plugin;
		this.playerLocationRecord = playerLocationRecord;
	}

	@Override
	public void run() {
		this.playerLocationRecord.location = this.playerLocationRecord.player.getLocation();
	}
}
