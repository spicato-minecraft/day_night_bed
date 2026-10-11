package day_night_bed.gametest;

import com.mojang.datafixers.util.Either;
import day_night_bed.config.DayNightBedConfig;
import day_night_bed.sleep.SleepContextFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Set;

/**
 * Shared bed, clock, and weather setup for sleep GameTests.
 * Each scenario uses its own test environment so these world-wide changes do not overlap.
 */
final class BedSleepHarness {
	static final long DAY_TIME = 1_000L;
	static final long NIGHT_TIME = 18_000L;
	static final long NIGHT_TARGET = 13_000L;
	static final BlockPos BED_FOOT = new BlockPos(1, 2, 1);
	static final BlockPos SECOND_BED_FOOT = new BlockPos(1, 2, 4);

	private BedSleepHarness() {
	}

	record Sleeper(ServerPlayer player, BlockPos bed) {
	}

	static ServerLevel begin(GameTestHelper context) {
		DayNightBedConfig.setForTest(DayNightBedConfig.defaults());
		ServerLevel level = context.getLevel();
		context.assertTrue(SleepContextFactory.isOverworld(level), "GameTest level should be the overworld");
		setAdvanceTime(level, true);
		clearPlayers(level);
		return level;
	}

	static void setAdvanceTime(ServerLevel level, boolean enabled) {
		level.getGameRules().set(GameRules.ADVANCE_TIME, enabled, level.getServer());
		level.getServer().getGlobalGameRules().set(GameRules.ADVANCE_TIME, enabled, level.getServer());
	}

	static void setClock(GameTestHelper context, long ticks) {
		context.setTime(ticks);
		context.getLevel().updateSkyBrightness();
	}

	static void setClearWeather(ServerLevel level) {
		WeatherData weather = level.getWeatherData();
		weather.setClearWeatherTime(20_000);
		weather.setRaining(false);
		weather.setThundering(false);
		weather.setRainTime(0);
		weather.setThunderTime(0);
		level.setRainLevel(0.0F);
		level.setThunderLevel(0.0F);
	}

	static void setRain(ServerLevel level, boolean thunder) {
		WeatherData weather = level.getWeatherData();
		weather.setClearWeatherTime(0);
		weather.setRaining(true);
		weather.setThundering(thunder);
		weather.setRainTime(20_000);
		weather.setThunderTime(20_000);
		level.setRainLevel(1.0F);
		level.setThunderLevel(thunder ? 1.0F : 0.0F);
	}

	static void requireDay(GameTestHelper context) {
		setClock(context, DAY_TIME);
		ServerLevel level = context.getLevel();
		context.assertTrue(
				level.isBrightOutside(),
				"expected daylight, skyDarken=" + level.getSkyDarken() + " clock=" + level.getDefaultClockTime()
		);
		context.assertFalse(level.isThundering(), "expected a clear day");
	}

	static void requireNight(GameTestHelper context) {
		setClock(context, NIGHT_TIME);
		ServerLevel level = context.getLevel();
		context.assertTrue(
				level.isDarkOutside(),
				"expected night, skyDarken=" + level.getSkyDarken() + " clock=" + level.getDefaultClockTime()
		);
		context.assertFalse(level.isThundering(), "expected a clear night");
	}

	static Sleeper sleeperAt(GameTestHelper context, BlockPos foot) {
		BlockPos bed = placeBed(context, foot);
		ServerPlayer player = context.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.CREATIVE);
		ServerLevel level = context.getLevel();
		player.teleportTo(level, bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, Set.of(), 0.0F, 0.0F, true);
		return new Sleeper(player, bed);
	}

	static Sleeper enterBed(GameTestHelper context, BlockPos foot) {
		Sleeper sleeper = sleeperAt(context, foot);
		Either<Player.BedSleepingProblem, Unit> result = trySleep(sleeper);
		if (result.left().isPresent()) {
			throw context.assertionException("sleep rejected: " + result.left().get());
		}
		context.assertTrue(sleeper.player().isSleeping(), "player should be in the bed");
		return sleeper;
	}

	/**
	 * {@link ServerPlayer#tick()} does not call {@link Player#tick()}, and mock players are not entity-ticked.
	 * The sleep timer and the daytime bed-rule check both live on {@link Player#tick()}.
	 */
	private static final MethodHandle PLAYER_TICK = bindPlayerTick();

	private static MethodHandle bindPlayerTick() {
		try {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Player.class, MethodHandles.lookup());
			return lookup.findSpecial(Player.class, "tick", MethodType.methodType(void.class), Player.class);
		} catch (IllegalAccessException | NoSuchMethodException e) {
			throw new IllegalStateException("Could not bind Player.tick", e);
		}
	}

	static void tickSleep(ServerPlayer player, int ticks) {
		try {
			for (int i = 0; i < ticks; i++) {
				PLAYER_TICK.invoke(player);
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	static Either<Player.BedSleepingProblem, Unit> trySleep(Sleeper sleeper) {
		ServerLevel level = sleeper.player().level();
		BlockState state = level.getBlockState(sleeper.bed());
		if (!(state.getBlock() instanceof AbstractBedBlock bed)) {
			throw new IllegalStateException("expected a bed at " + sleeper.bed() + " but found " + state);
		}
		return sleeper.player().startSleepInBed(bed, state, bed.getBedRule(level, sleeper.bed()), sleeper.bed());
	}

	static void finishRest(GameTestHelper context, ServerPlayer player) {
		tickSleep(player, Player.SLEEP_DURATION);
		context.assertTrue(player.isSleeping(), "player should still be in bed after a full rest");
		context.assertTrue(player.isSleepingLongEnough(), "rest should be long enough to apply on wake");
		player.stopSleepInBed(false, true);
		context.assertFalse(player.isSleeping(), "player should be awake after leaving the bed");
	}

	private static BlockPos placeBed(GameTestHelper context, BlockPos foot) {
		BlockState footState = Blocks.BED.red().defaultBlockState()
				.setValue(BedBlock.FACING, Direction.EAST)
				.setValue(BedBlock.PART, BedPart.FOOT)
				.setValue(BedBlock.OCCUPIED, false);
		context.setBlock(foot, footState);
		context.setBlock(foot.relative(Direction.EAST), footState.setValue(BedBlock.PART, BedPart.HEAD));
		return context.absolutePos(foot);
	}

	private static void clearPlayers(ServerLevel level) {
		for (ServerPlayer player : List.copyOf(level.players())) {
			level.getServer().getPlayerList().remove(player);
		}
	}
}
