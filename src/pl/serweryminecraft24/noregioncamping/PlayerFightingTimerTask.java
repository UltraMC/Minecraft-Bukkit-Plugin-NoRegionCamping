package pl.serweryminecraft24.noregioncamping;

import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

@SuppressWarnings("unused")
public class PlayerFightingTimerTask extends BukkitRunnable {
	private PluginNoRegionCamping plugin;
	private long lastIntervalTime = 0L;
	private long elapsedTime = 0L;
	private PlayerRecord fightingPlayer;
	private EventsListener eventsListener;

	public PlayerFightingTimerTask(PluginNoRegionCamping plugin, EventsListener eventsListener, PlayerRecord playerRecord) {
		this.plugin = plugin;
		this.fightingPlayer = playerRecord;
		this.eventsListener = eventsListener;
	}

	@Override
	public void run() {
		if (this.lastIntervalTime == 0L) {
			this.lastIntervalTime = System.currentTimeMillis();

			performPlayerCheck(0);

			return;
		}

		int elpasedSeconds = getElapsedSeconds();

		if (elpasedSeconds > 0) {
			performPlayerCheck(elpasedSeconds);
		}

		this.lastIntervalTime = System.currentTimeMillis();
	}

	private int getElapsedSeconds() {
		long currentTime = System.currentTimeMillis();

		this.elapsedTime += currentTime - this.lastIntervalTime;

		int elpasedSeconds = (int) (this.elapsedTime / 1000L);

		return elpasedSeconds;
	}

	private void performPlayerCheck(int elpasedSeconds) {
		String playerName = this.fightingPlayer.name;
		Integer currentFightingTime = this.fightingPlayer.fightingTimeLeft;

		this.fightingPlayer.fightingTimeLeft = currentFightingTime - elpasedSeconds;

		if (this.fightingPlayer.fightingTimeLeft < 0) {
			this.fightingPlayer.fightingTimeLeft = 0;
		}

		this.elapsedTime = 0L;

		if (this.fightingPlayer.fightingTimeLeft < 1) {
			if (plugin.getConfig().getBoolean("PluginNoRegionCamping.messages.endOfFight")) {
				this.fightingPlayer.player.sendMessage(ChatColor.getByChar(plugin.getConfig().getString("PluginNoRegionCamping.messages.color")) + plugin.getConfig().getString("PluginNoRegionCamping.language.endOfFight"));
			}

			this.eventsListener.removePlayerRecord(this.fightingPlayer);
			this.fightingPlayer = null;

			cancel();
		}

	}
}
