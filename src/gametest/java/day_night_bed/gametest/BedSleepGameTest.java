package day_night_bed.gametest;

import com.mojang.datafixers.util.Either;
import day_night_bed.config.DayNightBedConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;

/**
 * In-world bed sleeps. Each method has its own test environment so clock and weather changes run one at a time.
 */
public class BedSleepGameTest {
	private static final int FULL_REST_TICKS = 250;
	private static final int REST_STAT = 40;

	@GameTest(environment = "day_night_bed_test:day_sleep_sets_night", maxTicks = FULL_REST_TICKS, skyAccess = true)
	public void daySleepSetsNight(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setClearWeather(level);
		BedSleepHarness.requireDay(context);
		ServerPlayer player = BedSleepHarness.enterBed(context, BedSleepHarness.BED_FOOT).player();
		BedSleepHarness.finishRest(context, player);
		context.assertValueEqual(level.getDefaultClockTime(), BedSleepHarness.NIGHT_TARGET, "clock");
		context.succeed();
	}

	@GameTest(environment = "day_night_bed_test:night_sleep_sets_day", maxTicks = FULL_REST_TICKS, skyAccess = true)
	public void nightSleepSetsDay(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setClearWeather(level);
		BedSleepHarness.requireNight(context);
		ServerPlayer player = BedSleepHarness.enterBed(context, BedSleepHarness.BED_FOOT).player();
		BedSleepHarness.finishRest(context, player);
		context.assertValueEqual(level.getDefaultClockTime(), BedSleepHarness.DAY_TIME, "clock");
		context.succeed();
	}

	@GameTest(environment = "day_night_bed_test:stay_in_bed_through_day", maxTicks = 40, skyAccess = true)
	public void stayInBedThroughDay(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setClearWeather(level);
		BedSleepHarness.requireDay(context);
		ServerPlayer player = BedSleepHarness.enterBed(context, BedSleepHarness.BED_FOOT).player();
		BedSleepHarness.tickSleep(player, 8);
		context.assertTrue(player.isSleeping(), "player woke during the day before the rest finished, timer=" + player.getSleepTimer());
		context.assertTrue(player.getSleepTimer() >= 8, "sleep timer did not advance, timer=" + player.getSleepTimer());
		context.assertTrue(player.getSleepTimer() < Player.SLEEP_DURATION, "rest should not have finished yet");
		context.succeed();
	}

	@GameTest(environment = "day_night_bed_test:early_exit_leaves_time", maxTicks = 80, skyAccess = true)
	public void earlyExitLeavesTime(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setClearWeather(level);
		BedSleepHarness.setClock(context, 6_000L);
		context.assertTrue(level.isBrightOutside(), "early exit should start during the day");
		ServerPlayer player = BedSleepHarness.enterBed(context, BedSleepHarness.BED_FOOT).player();
		BedSleepHarness.tickSleep(player, 5);
		context.assertTrue(player.isSleeping(), "player left the bed before the early exit");
		context.assertTrue(player.getSleepTimer() >= 5, "sleep timer did not advance, timer=" + player.getSleepTimer());
		context.assertTrue(player.getSleepTimer() < Player.SLEEP_DURATION, "early exit waited for a full rest");
		long before = level.getDefaultClockTime();
		player.stopSleepInBed(false, true);
		context.assertFalse(player.isSleeping(), "player should be awake after leaving early");
		context.assertValueEqual(level.getDefaultClockTime(), before, "clock");
		context.assertTrue(before != BedSleepHarness.NIGHT_TARGET, "early exit snapped the clock to night");
		context.succeed();
	}

	@GameTest(environment = "day_night_bed_test:storm_sleep_clears_to_morning", maxTicks = 400, skyAccess = true)
	public void stormSleepClearsToMorning(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setClock(context, BedSleepHarness.DAY_TIME);
		BedSleepHarness.setRain(level, true);
		context.assertValueEqual(level.getDefaultClockTime(), BedSleepHarness.DAY_TIME, "clock before sleep");
		context.assertTrue(level.isThundering(), "storm sleep should start in thunder");
		ServerPlayer player = BedSleepHarness.enterBed(context, BedSleepHarness.BED_FOOT).player();
		BedSleepHarness.tickSleep(player, Player.SLEEP_DURATION);
		context.assertTrue(player.isSleepingLongEnough(), "storm rest should be long enough");
		boolean[] clockPaused = {false};
		context.succeedWhen(() -> {
			if (player.isSleeping()) {
				throw context.assertionException("still sleeping, timer=" + player.getSleepTimer());
			}
			context.assertValueEqual(level.getDefaultClockTime(), BedSleepHarness.DAY_TIME, "clock");
			if (!clockPaused[0]) {
				BedSleepHarness.setAdvanceTime(level, false);
				clockPaused[0] = true;
			}
			WeatherData weather = level.getWeatherData();
			if (weather.isThundering() || level.isThundering()) {
				throw context.assertionException(
						"thunder still active, weather=" + weather.isThundering() + " level=" + level.getThunderLevel(1.0F)
				);
			}
			context.assertValueEqual(level.getDefaultClockTime(), BedSleepHarness.DAY_TIME, "clock");
			BedSleepHarness.setAdvanceTime(level, true);
		});
	}

	@GameTest(environment = "day_night_bed_test:day_sleep_keeps_rain", maxTicks = FULL_REST_TICKS, skyAccess = true)
	public void daySleepKeepsRain(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setRain(level, false);
		BedSleepHarness.setClock(context, BedSleepHarness.DAY_TIME);
		context.assertTrue(level.isBrightOutside(), "rainy day sleep should start during the day");
		context.assertTrue(level.isRaining(), "rainy day sleep should start in rain");
		context.assertFalse(level.isThundering(), "rainy day sleep should not start in thunder");
		ServerPlayer player = BedSleepHarness.enterBed(context, BedSleepHarness.BED_FOOT).player();
		BedSleepHarness.tickSleep(player, Player.SLEEP_DURATION);
		context.assertTrue(player.isSleepingLongEnough(), "rainy day rest should be long enough");
		context.succeedWhen(() -> {
			if (player.isSleeping()) {
				throw context.assertionException("still sleeping, timer=" + player.getSleepTimer());
			}
			context.assertValueEqual(level.getDefaultClockTime(), BedSleepHarness.NIGHT_TARGET, "clock");
			WeatherData weather = level.getWeatherData();
			context.assertTrue(weather.isRaining(), "rain should still be active after a day rest");
			context.assertTrue(level.isRaining(), "rain level should still count as raining");
			context.assertFalse(weather.isThundering(), "a rainy day rest should not start thunder");
			context.assertFalse(level.isThundering(), "a rainy day rest should not become a thunderstorm");
		});
	}

	@GameTest(environment = "day_night_bed_test:disabled_mod_rejects_day_sleep", maxTicks = 20, skyAccess = true)
	public void disabledModRejectsDaySleep(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		DayNightBedConfig disabled = DayNightBedConfig.defaults();
		disabled.enabled = false;
		DayNightBedConfig.setForTest(disabled);
		try {
			BedSleepHarness.setClearWeather(level);
			BedSleepHarness.requireDay(context);
			long clock = level.getDefaultClockTime();
			BedSleepHarness.Sleeper sleeper = BedSleepHarness.sleeperAt(context, BedSleepHarness.BED_FOOT);
			Either<Player.BedSleepingProblem, Unit> result = sleeper.player().startSleepInBed(sleeper.bed());
			context.assertTrue(result.left().isPresent(), "daytime sleep should be rejected while the mod is disabled");
			context.assertFalse(sleeper.player().isSleeping(), "rejected daytime sleep should leave the player awake");
			context.assertValueEqual(level.getDefaultClockTime(), clock, "clock");
			context.succeed();
		} finally {
			DayNightBedConfig.setForTest(DayNightBedConfig.defaults());
		}
	}

	@GameTest(environment = "day_night_bed_test:daylight_cycle_off_rejects_day_sleep", maxTicks = 20, skyAccess = true)
	public void daylightCycleOffRejectsDaySleep(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		boolean previousLevel = level.getGameRules().get(GameRules.ADVANCE_TIME);
		boolean previousGlobal = level.getServer().getGlobalGameRules().get(GameRules.ADVANCE_TIME);
		try {
			BedSleepHarness.setClearWeather(level);
			BedSleepHarness.setAdvanceTime(level, false);
			BedSleepHarness.requireDay(context);
			long clock = level.getDefaultClockTime();
			BedSleepHarness.Sleeper sleeper = BedSleepHarness.sleeperAt(context, BedSleepHarness.BED_FOOT);
			Either<Player.BedSleepingProblem, Unit> result = sleeper.player().startSleepInBed(sleeper.bed());
			context.assertTrue(result.left().isPresent(), "daytime sleep should be rejected when the daylight cycle is off");
			context.assertFalse(sleeper.player().isSleeping(), "rejected daytime sleep should leave the player awake");
			context.assertValueEqual(level.getDefaultClockTime(), clock, "clock");
			context.succeed();
		} finally {
			level.getGameRules().set(GameRules.ADVANCE_TIME, previousLevel, level.getServer());
			level.getServer().getGlobalGameRules().set(GameRules.ADVANCE_TIME, previousGlobal, level.getServer());
		}
	}

	@GameTest(environment = "day_night_bed_test:day_sleep_skips_rest_stat", maxTicks = FULL_REST_TICKS, skyAccess = true)
	public void daySleepSkipsRestStat(GameTestHelper context) {
		finishRestStat(context, true, REST_STAT);
	}

	@GameTest(environment = "day_night_bed_test:night_sleep_resets_rest_stat", maxTicks = FULL_REST_TICKS, skyAccess = true)
	public void nightSleepResetsRestStat(GameTestHelper context) {
		finishRestStat(context, false, 0);
	}

	@GameTest(environment = "day_night_bed_test:wrapped_night_target", maxTicks = FULL_REST_TICKS, skyAccess = true)
	public void wrappedNightTarget(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		DayNightBedConfig config = DayNightBedConfig.defaults();
		config.nightTargetTick = 25_000L;
		DayNightBedConfig.setForTest(config);
		BedSleepHarness.setClearWeather(level);
		BedSleepHarness.requireDay(context);
		ServerPlayer player = BedSleepHarness.enterBed(context, BedSleepHarness.BED_FOOT).player();
		BedSleepHarness.finishRest(context, player);
		context.assertValueEqual(level.getDefaultClockTime(), BedSleepHarness.DAY_TIME, "clock");
		DayNightBedConfig.setForTest(DayNightBedConfig.defaults());
		context.succeed();
	}

	@GameTest(environment = "day_night_bed_test:two_sleepers_apply_once", maxTicks = FULL_REST_TICKS, skyAccess = true)
	public void twoSleepersApplyOnce(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setClearWeather(level);
		BedSleepHarness.requireDay(context);
		ServerPlayer first = BedSleepHarness.enterBed(context, BedSleepHarness.BED_FOOT).player();
		ServerPlayer second = BedSleepHarness.enterBed(context, BedSleepHarness.SECOND_BED_FOOT).player();
		BedSleepHarness.tickSleep(first, Player.SLEEP_DURATION);
		BedSleepHarness.tickSleep(second, Player.SLEEP_DURATION);
		context.assertTrue(first.isSleepingLongEnough() && second.isSleepingLongEnough(), "both players should finish the rest");
		first.stopSleepInBed(false, true);
		second.stopSleepInBed(false, true);
		context.assertFalse(first.isSleeping(), "first player should be awake");
		context.assertFalse(second.isSleeping(), "second player should be awake");
		context.assertValueEqual(level.getDefaultClockTime(), BedSleepHarness.NIGHT_TARGET, "clock");
		context.succeed();
	}

	private static void finishRestStat(GameTestHelper context, boolean day, int expectedStat) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setClearWeather(level);
		if (day) {
			BedSleepHarness.requireDay(context);
		} else {
			BedSleepHarness.requireNight(context);
		}
		BedSleepHarness.Sleeper sleeper = BedSleepHarness.sleeperAt(context, BedSleepHarness.BED_FOOT);
		Stat<Identifier> rest = restStat();
		sleeper.player().getStats().setValue(sleeper.player(), rest, REST_STAT);
		Either<Player.BedSleepingProblem, Unit> result = sleeper.player().startSleepInBed(sleeper.bed());
		if (result.left().isPresent()) {
			throw context.assertionException("sleep rejected: " + result.left().get());
		}
		long expectedClock = day ? BedSleepHarness.NIGHT_TARGET : BedSleepHarness.DAY_TIME;
		BedSleepHarness.finishRest(context, sleeper.player());
		context.assertValueEqual(level.getDefaultClockTime(), expectedClock, "clock");
		context.assertValueEqual(sleeper.player().getStats().getValue(rest), expectedStat, "time since rest");
		context.succeed();
	}

	private static Stat<Identifier> restStat() {
		return Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
	}
}
