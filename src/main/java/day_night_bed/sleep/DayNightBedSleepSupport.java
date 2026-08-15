package day_night_bed.sleep;

import day_night_bed.config.DayNightBedConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server sleep hook helpers — keeps mixins thin.
 */
public final class DayNightBedSleepSupport {

	private static final Map<ServerLevel, SleepTransitionResult> LAST_SLEEP_TRANSITION = new WeakHashMap<>();
	private static final Map<ServerLevel, Long> PENDING_VANILLA_DAWN = new WeakHashMap<>();
	private static final Set<ServerLevel> TIME_ADVANCE_APPLIED = Collections.newSetFromMap(new WeakHashMap<>());

	private DayNightBedSleepSupport() {
	}

	public static boolean resolveCanSleep(BedRule bedRule, ServerLevel level) {
		if (!DayNightBedConfig.get().enabled) {
			return bedRule.canSleep(level);
		}
		if (!SleepContextFactory.isOverworld(level)) {
			return bedRule.canSleep(level);
		}
		if (!level.getGameRules().get(GameRules.ADVANCE_TIME)) {
			return bedRule.canSleep(level);
		}
		return true;
	}

	/** Overworld toggles defer vanilla {@code setDayTime} until the player actually leaves the bed. */
	public static boolean shouldDeferTimeAdvanceToWake(ServerLevel level) {
		if (!DayNightBedConfig.get().enabled) {
			return false;
		}
		if (!SleepContextFactory.isOverworld(level)) {
			return false;
		}
		if (!level.getGameRules().get(GameRules.ADVANCE_TIME)) {
			return false;
		}
		SleepTransitionResult result = SleepTransitionResolver.resolve(SleepContextFactory.fromLevel(level));
		return result != SleepTransitionResult.NO_OP;
	}

	public static void markSleepCompletionPending(ServerLevel level, long vanillaDawnTime) {
		PENDING_VANILLA_DAWN.put(level, vanillaDawnTime);
		BedInteractionLogger.sleepCompletionPending(level, vanillaDawnTime);
	}

	public static void resetSleepCycle(ServerLevel level) {
		PENDING_VANILLA_DAWN.remove(level);
		TIME_ADVANCE_APPLIED.remove(level);
		LAST_SLEEP_TRANSITION.remove(level);
	}

	/**
	 * Apply the mod time target when a player exits bed after a completed rest (AC-19).
	 * Called at {@code stopSleepInBed} HEAD while {@code isSleepingLongEnough()} is still true.
	 */
	public static void tryApplyTimeOnWake(ServerPlayer player) {
		if (!player.isSleepingLongEnough()) {
			BedInteractionLogger.timeAdvanceDeferredOnWake(player, "early-exit");
			return;
		}
		ServerLevel level = player.level();
		if (!shouldDeferTimeAdvanceToWake(level)) {
			return;
		}
		if (!TIME_ADVANCE_APPLIED.add(level)) {
			BedInteractionLogger.timeAdvanceDeferredOnWake(player, "already-applied");
			return;
		}
		Long pendingDawn = PENDING_VANILLA_DAWN.remove(level);
		long vanillaDawn = pendingDawn != null ? pendingDawn : computeVanillaDawnTime(level.getDayTime());
		BedInteractionLogger.timeAdvanceDeferredOnWake(player, "applying");
		applySleepTimeAdvance(level, vanillaDawn);
	}

	public static void applySleepTimeAdvance(ServerLevel level, long vanillaDawnTime) {
		long before = level.getDayTime();
		SleepContext context = SleepContextFactory.fromLevel(level);
		SleepTransitionResult result = SleepTransitionResolver.resolve(context);
		LAST_SLEEP_TRANSITION.put(level, result);

		OptionalLong target = SleepTransitionResolver.targetTick(result, context.config());
		if (target.isEmpty()) {
			BedInteractionLogger.sleepTimeAdvanceSkipped(level, result);
			level.setDayTime(vanillaDawnTime);
			return;
		}

		long dayTime = normalizeDayTime(target.getAsLong());
		BedInteractionLogger.sleepTimeAdvance(level, result, before, dayTime, vanillaDawnTime);
		level.setDayTime(dayTime);
	}

	public static boolean shouldPreserveWeather(ServerLevel level) {
		SleepTransitionResult result = LAST_SLEEP_TRANSITION.get(level);
		if (result == null) {
			return false;
		}
		return result == SleepTransitionResult.TO_NIGHT || result == SleepTransitionResult.TO_DAY;
	}

	public static SleepTransitionResult lastSleepTransition(ServerLevel level) {
		return LAST_SLEEP_TRANSITION.get(level);
	}

	public static void clearSleepTransition(ServerLevel level) {
		LAST_SLEEP_TRANSITION.remove(level);
	}

	public static void clearSleepTransition(ServerLevel level, String reason) {
		if (LAST_SLEEP_TRANSITION.containsKey(level)) {
			BedInteractionLogger.clearTransition(level, reason);
		}
		LAST_SLEEP_TRANSITION.remove(level);
	}

	public static void endSleepTick(ServerLevel level) {
		PENDING_VANILLA_DAWN.remove(level);
		TIME_ADVANCE_APPLIED.remove(level);
		clearSleepTransition(level, "tick-tail");
	}

	private static long computeVanillaDawnTime(long dayTime) {
		long normalized = dayTime % 24_000L;
		if (normalized < 0) {
			normalized += 24_000L;
		}
		return dayTime + (24_000L - normalized);
	}

	private static long normalizeDayTime(long tick) {
		long normalized = tick % 24_000L;
		if (normalized < 0) {
			normalized += 24_000L;
		}
		return normalized;
	}
}
