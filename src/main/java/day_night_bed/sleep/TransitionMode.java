package day_night_bed.sleep;

import java.util.Optional;

/**
 * Client overlay copy mode for sleep UI (AC-25–28).
 */
public enum TransitionMode {
	REST,
	SLEEP;

	public static Optional<TransitionMode> fromResult(SleepTransitionResult result) {
		return switch (result) {
			case TO_NIGHT -> Optional.of(REST);
			case TO_DAY, STORM_SKIP_MORNING -> Optional.of(SLEEP);
			case NO_OP -> Optional.empty();
		};
	}
}
