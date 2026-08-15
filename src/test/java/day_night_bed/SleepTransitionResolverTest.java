package day_night_bed;

import day_night_bed.config.DayNightBedConfig;
import day_night_bed.sleep.SleepContext;
import day_night_bed.sleep.SleepTransitionResult;
import day_night_bed.sleep.SleepTransitionResolver;
import day_night_bed.sleep.TransitionMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepTransitionResolverTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void clearDaytime_returnsToNight() {
		SleepContext context = context(true, true, false, false, false, true, enabledConfig());

		assertEquals(SleepTransitionResult.TO_NIGHT, SleepTransitionResolver.resolve(context));
		assertEquals(OptionalLong.of(13_000L), SleepTransitionResolver.targetTick(SleepTransitionResult.TO_NIGHT, enabledConfig()));
		assertEquals(Optional.of(TransitionMode.REST), TransitionMode.fromResult(SleepTransitionResult.TO_NIGHT));
	}

	@Test
	void clearNighttime_returnsToDay() {
		SleepContext context = context(true, false, true, false, false, true, enabledConfig());

		assertEquals(SleepTransitionResult.TO_DAY, SleepTransitionResolver.resolve(context));
		assertEquals(OptionalLong.of(1_000L), SleepTransitionResolver.targetTick(SleepTransitionResult.TO_DAY, enabledConfig()));
		assertEquals(Optional.of(TransitionMode.SLEEP), TransitionMode.fromResult(SleepTransitionResult.TO_DAY));
	}

	@Test
	void daytimeThunder_returnsStormSkipMorning() {
		SleepContext context = context(true, true, false, true, true, true, enabledConfig());

		assertEquals(SleepTransitionResult.STORM_SKIP_MORNING, SleepTransitionResolver.resolve(context));
		assertEquals(OptionalLong.of(1_000L), SleepTransitionResolver.targetTick(SleepTransitionResult.STORM_SKIP_MORNING, enabledConfig()));
		assertEquals(Optional.of(TransitionMode.SLEEP), TransitionMode.fromResult(SleepTransitionResult.STORM_SKIP_MORNING));
	}

	@Test
	void nighttimeThunder_returnsStormSkipMorning() {
		SleepContext context = context(true, false, true, true, true, true, enabledConfig());

		assertEquals(SleepTransitionResult.STORM_SKIP_MORNING, SleepTransitionResolver.resolve(context));
	}

	@Test
	void daytimeRainWithoutThunder_returnsToNight() {
		SleepContext context = context(true, true, false, false, true, true, enabledConfig());

		assertEquals(SleepTransitionResult.TO_NIGHT, SleepTransitionResolver.resolve(context));
		assertFalse(SleepTransitionResolver.isStorm(context));
	}

	@Test
	void nighttimeRainWithoutThunder_returnsToDay() {
		SleepContext context = context(true, false, true, false, true, true, enabledConfig());

		assertEquals(SleepTransitionResult.TO_DAY, SleepTransitionResolver.resolve(context));
		assertFalse(SleepTransitionResolver.isStorm(context));
	}

	@Test
	void masterToggleDisabled_returnsNoOp() {
		DayNightBedConfig config = enabledConfig();
		config.enabled = false;
		SleepContext context = context(true, true, false, false, false, true, config);

		assertEquals(SleepTransitionResult.NO_OP, SleepTransitionResolver.resolve(context));
		assertTrue(SleepTransitionResolver.targetTick(SleepTransitionResult.NO_OP, config).isEmpty());
		assertTrue(TransitionMode.fromResult(SleepTransitionResult.NO_OP).isEmpty());
	}

	@Test
	void doDaylightCycleFalse_returnsNoOp() {
		SleepContext context = context(true, true, false, false, false, false, enabledConfig());

		assertEquals(SleepTransitionResult.NO_OP, SleepTransitionResolver.resolve(context));
	}

	@Test
	void nonOverworld_returnsNoOp() {
		SleepContext context = context(false, true, false, false, false, true, enabledConfig());

		assertEquals(SleepTransitionResult.NO_OP, SleepTransitionResolver.resolve(context));
	}

	@Test
	void twilightNeitherDayNorNight_returnsNoOp() {
		SleepContext context = context(true, false, false, false, false, true, enabledConfig());

		assertEquals(SleepTransitionResult.NO_OP, SleepTransitionResolver.resolve(context));
	}

	@Test
	void customTargetTicks_appliedByTargetTickHelper() {
		DayNightBedConfig config = enabledConfig();
		config.dayTargetTick = 2_000L;
		config.nightTargetTick = 14_000L;

		assertEquals(OptionalLong.of(14_000L), SleepTransitionResolver.targetTick(SleepTransitionResult.TO_NIGHT, config));
		assertEquals(OptionalLong.of(2_000L), SleepTransitionResolver.targetTick(SleepTransitionResult.TO_DAY, config));
	}

	@Test
	void configDefaults_matchAcceptanceCriteria() {
		DayNightBedConfig config = DayNightBedConfig.defaults();

		assertTrue(config.enabled);
		assertEquals(1_000L, config.dayTargetTick);
		assertEquals(13_000L, config.nightTargetTick);
		assertFalse(config.resetRestStatOnDayRest);
	}

	private static DayNightBedConfig enabledConfig() {
		return DayNightBedConfig.defaults();
	}

	private static SleepContext context(
			boolean overworld,
			boolean isDay,
			boolean isNight,
			boolean isThundering,
			boolean isRaining,
			boolean doDaylightCycle,
			DayNightBedConfig config
	) {
		return SleepContext.of(overworld, isDay, isNight, isThundering, isRaining, doDaylightCycle, config);
	}
}
