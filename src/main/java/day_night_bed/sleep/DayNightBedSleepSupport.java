package day_night_bed.sleep;

import day_night_bed.config.DayNightBedConfig;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;
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
	private static final Set<ServerLevel> PENDING_VANILLA_WAKE = Collections.newSetFromMap(new WeakHashMap<>());
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

	/** Overworld toggles defer vanilla {@code moveToTimeMarker(WAKE_UP_FROM_SLEEP)} until the player leaves the bed. */
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

	public static void markSleepCompletionPending(ServerLevel level) {
		PENDING_VANILLA_WAKE.add(level);
		BedInteractionLogger.sleepCompletionPending(level, clockTime(level));
	}

	public static void resetSleepCycle(ServerLevel level) {
		PENDING_VANILLA_WAKE.remove(level);
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
		PENDING_VANILLA_WAKE.remove(level);
		BedInteractionLogger.timeAdvanceDeferredOnWake(player, "applying");
		applySleepTimeAdvance(level);
	}

	public static void applySleepTimeAdvance(ServerLevel level) {
		long before = clockTime(level);
		SleepContext context = SleepContextFactory.fromLevel(level);
		SleepTransitionResult result = SleepTransitionResolver.resolve(context);
		LAST_SLEEP_TRANSITION.put(level, result);

		OptionalLong target = SleepTransitionResolver.targetTick(result, context.config());
		if (target.isEmpty()) {
			BedInteractionLogger.sleepTimeAdvanceSkipped(level, result);
			applyVanillaWakeUp(level);
			return;
		}

		long dayTime = normalizeDayTime(target.getAsLong());
		BedInteractionLogger.sleepTimeAdvance(level, result, before, dayTime, before);
		setClockTime(level, dayTime);
	}

	public static long clockTime(Level level) {
		return level.getDefaultClockTime();
	}

	public static void setClockTime(ServerLevel level, long ticks) {
		defaultClock(level).ifPresent(clock -> level.getServer().clockManager().setTotalTicks(clock, ticks));
	}

	public static void applyVanillaWakeUp(ServerLevel level) {
		defaultClock(level).ifPresent(clock ->
				level.getServer().clockManager().moveToTimeMarker(clock, ClockTimeMarkers.WAKE_UP_FROM_SLEEP));
	}

	private static java.util.Optional<Holder<WorldClock>> defaultClock(ServerLevel level) {
		return level.dimensionType().defaultClock();
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
		PENDING_VANILLA_WAKE.remove(level);
		TIME_ADVANCE_APPLIED.remove(level);
		clearSleepTransition(level, "tick-tail");
	}

	private static long normalizeDayTime(long tick) {
		long normalized = tick % 24_000L;
		if (normalized < 0) {
			normalized += 24_000L;
		}
		return normalized;
	}
}
