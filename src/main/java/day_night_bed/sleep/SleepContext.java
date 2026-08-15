package day_night_bed.sleep;

import day_night_bed.config.DayNightBedConfig;

/**
 * Inputs for {@link SleepTransitionResolver}. Callers supply {@code isDay}/{@code isNight}
 * from {@link net.minecraft.world.level.Level#isDay()} / {@link net.minecraft.world.level.Level#isNight()}.
 */
public record SleepContext(
		boolean overworld,
		boolean isDay,
		boolean isNight,
		boolean isThundering,
		boolean isRaining,
		boolean doDaylightCycle,
		DayNightBedConfig config
) {
	public static SleepContext of(
			boolean overworld,
			boolean isDay,
			boolean isNight,
			boolean isThundering,
			boolean isRaining,
			boolean doDaylightCycle,
			DayNightBedConfig config
	) {
		return new SleepContext(overworld, isDay, isNight, isThundering, isRaining, doDaylightCycle, config);
	}
}
