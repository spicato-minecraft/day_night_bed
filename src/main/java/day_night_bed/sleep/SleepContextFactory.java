package day_night_bed.sleep;

import day_night_bed.config.DayNightBedConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Builds {@link SleepContext} from live server state.
 */
public final class SleepContextFactory {

	private SleepContextFactory() {
	}

	public static SleepContext fromLevel(ServerLevel level) {
		DayNightBedConfig config = DayNightBedConfig.get();
		return SleepContext.of(
				isOverworld(level),
				isDay(level),
				isNight(level),
				level.isThundering(),
				level.isRaining(),
				level.getGameRules().get(GameRules.ADVANCE_TIME),
				config
		);
	}

	public static boolean isOverworld(Level level) {
		return level.dimension().equals(Level.OVERWORLD);
	}

	/** Matches bright-sky phase (vanilla {@code isDay} equivalent in 1.21.11). */
	public static boolean isDay(Level level) {
		return level.isBrightOutside();
	}

	/** Matches dark-sky phase (vanilla {@code isNight} equivalent in 1.21.11). */
	public static boolean isNight(Level level) {
		return level.isDarkOutside();
	}
}
