package day_night_bed.sleep;

import day_night_bed.config.DayNightBedConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Phantom {@code TimeSinceRest} policy (AC-7, AC-8).
 */
public final class RestStatPolicy {

	private RestStatPolicy() {
	}

	public static boolean shouldSkipRestStatReset(ServerPlayer player) {
		DayNightBedConfig config = DayNightBedConfig.get();
		if (!config.enabled) {
			return false;
		}
		ServerLevel level = player.level();
		if (!SleepContextFactory.isOverworld(level)) {
			return false;
		}
		if (level.isThundering()) {
			return false;
		}
		if (!SleepContextFactory.isDay(level)) {
			return false;
		}
		return !config.resetRestStatOnDayRest;
	}
}
