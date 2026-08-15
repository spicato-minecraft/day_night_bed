package day_night_bed.sleep;

import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Derives overlay copy mode from live world state on the client (AC-25–28).
 */
public final class ClientSleepOverlayMode {

	private ClientSleepOverlayMode() {
	}

	public static Optional<TransitionMode> fromLevel(Level level) {
		if (level == null || !SleepContextFactory.isOverworld(level)) {
			return Optional.empty();
		}
		if (level.isThundering()) {
			return Optional.of(TransitionMode.SLEEP);
		}
		if (SleepContextFactory.isDay(level)) {
			return Optional.of(TransitionMode.REST);
		}
		return Optional.of(TransitionMode.SLEEP);
	}
}
