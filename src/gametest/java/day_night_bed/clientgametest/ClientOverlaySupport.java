package day_night_bed.clientgametest;

import day_night_bed.config.DayNightBedConfig;
import day_night_bed.sleep.DayNightBedSleepSupport;
import day_night_bed.sleep.SleepOverlayMessages;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;

import java.lang.reflect.Field;

final class ClientOverlaySupport {
	static final long DAY_TIME = 1_000L;
	static final long NIGHT_TIME = 18_000L;

	private ClientOverlaySupport() {
	}

	static String overlayKey(Minecraft client) {
		try {
			Field field = Hud.class.getDeclaredField("overlayMessageString");
			field.setAccessible(true);
			Object value = field.get(client.gui.hud);
			if (!(value instanceof Component component)) {
				throw new AssertionError("sleep overlay is missing");
			}
			if (!(component.getContents() instanceof TranslatableContents contents)) {
				throw new AssertionError("sleep overlay is not translatable: " + component);
			}
			return contents.getKey();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not read the sleep overlay", e);
		}
	}

	static void showSkippingNight(ClientGameTestContext context, String expectedKey) {
		context.runOnClient(client -> {
			client.gui.hud.setOverlayMessage(Component.translatable(SleepOverlayMessages.VANILLA_SKIPPING_NIGHT), false);
			String key = overlayKey(client);
			if (!expectedKey.equals(key)) {
				throw new AssertionError("overlay key was " + key + ", expected " + expectedKey);
			}
		});
	}

	static void setTimeAndWeather(TestServerContext server, long time, boolean thunder) {
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			DayNightBedSleepSupport.setClockTime(level, time);
			WeatherData weather = level.getWeatherData();
			weather.setClearWeatherTime(thunder ? 0 : 20_000);
			weather.setRaining(thunder);
			weather.setThundering(thunder);
			weather.setRainTime(thunder ? 20_000 : 0);
			weather.setThunderTime(thunder ? 20_000 : 0);
			level.setRainLevel(thunder ? 1.0F : 0.0F);
			level.setThunderLevel(thunder ? 1.0F : 0.0F);
			level.updateSkyBrightness();
			minecraft.getPlayerList().broadcastAll(minecraft.clockManager().createFullSyncPacket());
		});
		server.runCommand(thunder ? "weather thunder" : "weather clear");
	}

	/**
	 * Puts the local player in a bed and keeps the rest from completing, so the client can observe 100 sleep ticks.
	 */
	static void startUnfinishedDaySleep(TestServerContext server) {
		server.runOnServer(minecraft -> {
			DayNightBedConfig.setForTest(DayNightBedConfig.defaults());
			ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
			ServerLevel level = player.level();
			level.getGameRules().set(GameRules.ADVANCE_TIME, true, minecraft);
			minecraft.getGlobalGameRules().set(GameRules.ADVANCE_TIME, true, minecraft);
			level.getGameRules().set(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 101, minecraft);
			player.setGameMode(GameType.CREATIVE);
			BlockPos foot = player.blockPosition();
			BlockState footState = Blocks.BED.red().defaultBlockState()
					.setValue(BedBlock.FACING, Direction.EAST)
					.setValue(BedBlock.PART, BedPart.FOOT)
					.setValue(BedBlock.OCCUPIED, false);
			level.setBlockAndUpdate(foot, footState);
			level.setBlockAndUpdate(foot.relative(Direction.EAST), footState.setValue(BedBlock.PART, BedPart.HEAD));
			level.setBlockAndUpdate(foot.above(), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(foot.relative(Direction.WEST).above(), Blocks.AIR.defaultBlockState());
			player.teleportTo(level, foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, java.util.Set.of(), 0.0F, 0.0F, true);
			BlockState sleptIn = level.getBlockState(foot);
			if (!(sleptIn.getBlock() instanceof AbstractBedBlock bed)) {
				throw new AssertionError("expected a bed at " + foot + " but found " + sleptIn);
			}
			var result = player.startSleepInBed(bed, sleptIn, bed.getBedRule(level, foot), foot);
			if (result.left().isPresent()) {
				throw new AssertionError("sleep rejected: " + result.left().get());
			}
		});
	}
}
