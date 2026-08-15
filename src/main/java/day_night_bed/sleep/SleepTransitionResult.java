package day_night_bed.sleep;

/**
 * Outcome of {@link SleepTransitionResolver} for a completed Overworld bed sleep.
 */
public enum SleepTransitionResult {
	/** Active thunder — vanilla skip to morning; weather may clear. */
	STORM_SKIP_MORNING,
	/** Non-storm daytime sleep — advance to configured night time. */
	TO_NIGHT,
	/** Non-storm nighttime sleep — advance to configured day time. */
	TO_DAY,
	/** No mod time change (disabled, wrong dimension, {@code doDaylightCycle} off, etc.). */
	NO_OP
}
