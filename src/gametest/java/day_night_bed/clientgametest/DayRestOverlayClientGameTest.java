package day_night_bed.clientgametest;

import day_night_bed.sleep.SleepOverlayMessages;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.level.Level;

/**
 * Daytime overworld sleep copy, both from a vanilla skipping-night message and from a local bed rest.
 */
public class DayRestOverlayClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksDownload();
			ClientOverlaySupport.setTimeAndWeather(world.getServer(), ClientOverlaySupport.DAY_TIME, false);
			context.waitFor(client -> client.level != null
					&& client.level.dimension().equals(Level.OVERWORLD)
					&& client.level.isBrightOutside()
					&& !client.level.isThundering());
			ClientOverlaySupport.showSkippingNight(context, SleepOverlayMessages.REST_THROUGH_DAY);

			ClientOverlaySupport.startUnfinishedDaySleep(world.getServer());
			context.waitFor(client -> {
				var player = client.player;
				if (player == null || !player.isSleeping() || player.getSleepTimer() < 100) {
					return false;
				}
				return SleepOverlayMessages.REST_THROUGH_DAY.equals(ClientOverlaySupport.overlayKey(client));
			}, 400);
		}
	}
}
