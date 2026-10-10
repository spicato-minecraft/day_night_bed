package day_night_bed.clientgametest;

import day_night_bed.sleep.SleepOverlayMessages;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.level.Level;

/**
 * Night and daytime thunder keep vanilla sleep copy.
 */
public class NightAndStormOverlayClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksDownload();
			ClientOverlaySupport.setTimeAndWeather(world.getServer(), ClientOverlaySupport.NIGHT_TIME, false);
			context.waitFor(client -> client.level != null
					&& client.level.dimension().equals(Level.OVERWORLD)
					&& client.level.isDarkOutside()
					&& !client.level.isThundering());
			ClientOverlaySupport.showSkippingNight(context, SleepOverlayMessages.VANILLA_SKIPPING_NIGHT);

			ClientOverlaySupport.setTimeAndWeather(world.getServer(), ClientOverlaySupport.DAY_TIME, true);
			try {
				context.waitFor(client -> {
					if (client.level == null) {
						return false;
					}
					// Thunder visual level is rain * thunder, and the client often never crosses 0.9 from the weather packet alone.
					client.level.setRainLevel(1.0F);
					client.level.setThunderLevel(1.0F);
					if (!client.level.isThundering()) {
						return false;
					}
					long dayTime = Math.floorMod(client.level.getDefaultClockTime(), 24_000L);
					return dayTime < 12_000L;
				});
			} catch (AssertionError e) {
				context.runOnClient(client -> {
					var level = client.level;
					throw new AssertionError(
							"daytime thunder did not reach the client, clock="
									+ (level == null ? "none" : level.getDefaultClockTime())
									+ " rain=" + (level == null ? "none" : level.getRainLevel(1.0F))
									+ " thunder=" + (level == null ? "none" : level.getThunderLevel(1.0F)),
							e
					);
				});
			}
			ClientOverlaySupport.showSkippingNight(context, SleepOverlayMessages.VANILLA_SKIPPING_NIGHT);
		}
	}
}