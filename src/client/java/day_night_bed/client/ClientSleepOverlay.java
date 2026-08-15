package day_night_bed.client;

import day_night_bed.sleep.ClientSleepOverlayMode;
import day_night_bed.sleep.SleepOverlayMessages;
import day_night_bed.sleep.TransitionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

/**
 * Drives sleep action-bar copy in offline singleplayer, where vanilla skips {@code announceSleepStatus}.
 * Completion copy uses vanilla {@link net.minecraft.client.gui.Gui#setOverlayMessage} display time (60 ticks).
 */
public final class ClientSleepOverlay {

	private static final int DEEP_SLEEP_TICKS = 100;

	private static boolean wasSleeping;
	private static boolean completedDeepSleep;
	private static TransitionMode lastMode = TransitionMode.SLEEP;

	private ClientSleepOverlay() {
	}

	public static void tick(Minecraft client) {
		if (!shouldDriveLocally(client)) {
			resetTracking();
			return;
		}

		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			resetTracking();
			return;
		}

		var mode = ClientSleepOverlayMode.fromLevel(client.level);
		if (mode.isEmpty()) {
			resetTracking();
			return;
		}

		if (player.isSleeping()) {
			lastMode = mode.get();
			boolean deepSleep = player.getSleepTimer() >= DEEP_SLEEP_TICKS;
			completedDeepSleep = completedDeepSleep || deepSleep;
			Component overlay = SleepOverlayMessages.createLocalOverlay(lastMode, deepSleep, 1, 1);
			client.gui.setOverlayMessage(overlay, false);
			wasSleeping = true;
			return;
		}

		if (wasSleeping && completedDeepSleep) {
			Component completion = SleepOverlayMessages.createLocalOverlay(lastMode, true, 1, 1);
			client.gui.setOverlayMessage(completion, false);
		}
		resetTracking();
	}

	private static boolean shouldDriveLocally(Minecraft client) {
		if (!client.isLocalServer()) {
			return false;
		}
		IntegratedServer server = client.getSingleplayerServer();
		return server != null && !server.isPublished();
	}

	private static void resetTracking() {
		wasSleeping = false;
		completedDeepSleep = false;
	}
}
