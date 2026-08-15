package day_night_bed.sleep;

import day_night_bed.config.DayNightBedConfig;

import java.util.OptionalLong;

/**
 * Pure sleep transition logic — unit tested, called from server mixins in later phases.
 */
public final class SleepTransitionResolver {

	private SleepTransitionResolver() {
	}

	public static SleepTransitionResult resolve(SleepContext context) {
		DayNightBedConfig config = context.config();
		if (config == null || !config.enabled) {
			return SleepTransitionResult.NO_OP;
		}
		if (!context.overworld()) {
			return SleepTransitionResult.NO_OP;
		}
		if (!context.doDaylightCycle()) {
			return SleepTransitionResult.NO_OP;
		}
		if (context.isThundering()) {
			return SleepTransitionResult.STORM_SKIP_MORNING;
		}
		if (context.isDay()) {
			return SleepTransitionResult.TO_NIGHT;
		}
		if (context.isNight()) {
			return SleepTransitionResult.TO_DAY;
		}
		return SleepTransitionResult.NO_OP;
	}

	public static OptionalLong targetTick(SleepTransitionResult result, DayNightBedConfig config) {
		return switch (result) {
			case TO_NIGHT -> OptionalLong.of(config.nightTargetTick);
			case TO_DAY, STORM_SKIP_MORNING -> OptionalLong.of(config.dayTargetTick);
			case NO_OP -> OptionalLong.empty();
		};
	}

	/** Storm path only — rain without thunder does not qualify (AC-10). */
	public static boolean isStorm(SleepContext context) {
		return context.isThundering();
	}
}
