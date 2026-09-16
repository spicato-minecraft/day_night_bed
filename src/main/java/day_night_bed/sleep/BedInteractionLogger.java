package day_night_bed.sleep;

import day_night_bed.DayNightBed;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.Level;

/**
 * Debug logging for bed sleep flow — enable via {@code log4j2} logger {@code day_night_bed.bed}.
 */
public final class BedInteractionLogger {

	private BedInteractionLogger() {
	}

	public static void startSleepInBed(ServerPlayer player, BlockPos pos, String outcome, Object detail) {
		DayNightBed.LOGGER.info(
				"[bed] startSleepInBed player={} pos={} dim={} dayTime={} bright={} dark={} thunder={} -> {} {}",
				player.getGameProfile().name(),
				pos.toShortString(),
				player.level().dimension().identifier(),
				DayNightBedSleepSupport.clockTime(player.level()),
				SleepContextFactory.isDay(player.level()),
				SleepContextFactory.isNight(player.level()),
				player.level().isThundering(),
				outcome,
				detail
		);
	}

	public static void startSleeping(ServerPlayer player, BlockPos pos) {
		DayNightBed.LOGGER.info(
				"[bed] startSleeping player={} pos={} sleeping={} sleepCounter={}",
				player.getGameProfile().name(),
				pos.toShortString(),
				player.isSleeping(),
				player.getSleepTimer()
		);
	}

	public static void stopSleepInBed(ServerPlayer player, boolean setSpawn, boolean updateSleepList) {
		DayNightBed.LOGGER.info(
				"[bed] stopSleepInBed player={} setSpawn={} updateSleepList={} sleeping={} sleepCounter={} dayTime={}",
				player.getGameProfile().name(),
				setSpawn,
				updateSleepList,
				player.isSleeping(),
				player.getSleepTimer(),
				DayNightBedSleepSupport.clockTime(player.level())
		);
	}

	public static void canSleepCheck(String source, BedRule bedRule, Level level, boolean vanilla, boolean resolved) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		DayNightBed.LOGGER.info(
				"[bed] canSleepCheck source={} dim={} dayTime={} bright={} dark={} vanilla={} resolved={} bedRule={}",
				source,
				serverLevel.dimension().identifier(),
				DayNightBedSleepSupport.clockTime(serverLevel),
				SleepContextFactory.isDay(serverLevel),
				SleepContextFactory.isNight(serverLevel),
				vanilla,
				resolved,
				bedRule
		);
	}

	public static void sleepTimeAdvance(ServerLevel level, SleepTransitionResult result, long before, long target, long vanillaDawn) {
		DayNightBed.LOGGER.info(
				"[bed] sleepTimeAdvance dim={} result={} dayTime {} -> {} (vanillaDawn={}) bright={} dark={} thunder={}",
				level.dimension().identifier(),
				result,
				before,
				target,
				vanillaDawn,
				SleepContextFactory.isDay(level),
				SleepContextFactory.isNight(level),
				level.isThundering()
		);
	}

	public static void sleepTimeAdvanceSkipped(ServerLevel level, SleepTransitionResult result) {
		DayNightBed.LOGGER.info(
				"[bed] sleepTimeAdvanceSkipped dim={} result={} dayTime={}",
				level.dimension().identifier(),
				result,
				DayNightBedSleepSupport.clockTime(level)
		);
	}

	public static void weatherReset(ServerLevel level, boolean preserved, SleepTransitionResult result) {
		DayNightBed.LOGGER.info(
				"[bed] weatherReset dim={} preserved={} transition={}",
				level.dimension().identifier(),
				preserved,
				result
		);
	}

	public static void clearTransition(ServerLevel level, String reason) {
		DayNightBed.LOGGER.debug("[bed] clearTransition dim={} reason={}", level.dimension().identifier(), reason);
	}

	public static void sleepCompletionPending(ServerLevel level, long vanillaDawnTime) {
		DayNightBed.LOGGER.info(
				"[bed] sleepCompletionPending dim={} vanillaDawn={} dayTime={}",
				level.dimension().identifier(),
				vanillaDawnTime,
				DayNightBedSleepSupport.clockTime(level)
		);
	}

	public static void timeAdvanceDeferredOnWake(ServerPlayer player, String action) {
		DayNightBed.LOGGER.info(
				"[bed] timeOnWake player={} action={} sleeping={} sleepCounter={} dayTime={}",
				player.getGameProfile().name(),
				action,
				player.isSleeping(),
				player.getSleepTimer(),
				DayNightBedSleepSupport.clockTime(player.level())
		);
	}
}
