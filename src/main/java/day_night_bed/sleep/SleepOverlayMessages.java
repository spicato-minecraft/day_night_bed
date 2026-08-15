package day_night_bed.sleep;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;

/**
 * Rewrites vanilla sleep overlay strings for day-rest transitions (AC-25–28).
 * Does not replace global lang entries (AC-29–30).
 */
public final class SleepOverlayMessages {

	public static final String VANILLA_SKIPPING_NIGHT = "sleep.skipping_night";
	public static final String VANILLA_PLAYERS_SLEEPING = "sleep.players_sleeping";
	public static final String REST_THROUGH_DAY = "sleep.day_night_bed.resting_through_day";
	public static final String REST_PLAYERS = "sleep.day_night_bed.players_resting";

	private SleepOverlayMessages() {
	}

	public static Optional<Component> rewriteWithArgs(Component message, Optional<TransitionMode> mode) {
		if (mode.isEmpty() || mode.get() != TransitionMode.REST) {
			return Optional.empty();
		}
		Optional<String> key = translatableKey(message);
		if (key.isEmpty()) {
			return Optional.empty();
		}
		return switch (key.get()) {
			case VANILLA_SKIPPING_NIGHT -> Optional.of(Component.translatable(REST_THROUGH_DAY));
			case VANILLA_PLAYERS_SLEEPING -> Optional.of(
					Component.translatable(REST_PLAYERS, translatableArgs(message))
			);
			default -> Optional.empty();
		};
	}

	/** Local overlay when vanilla does not send sleep status (offline singleplayer). */
	public static Component createLocalOverlay(
			TransitionMode mode,
			boolean deepSleepComplete,
			int sleeping,
			int needed
	) {
		if (deepSleepComplete) {
			return mode == TransitionMode.REST
					? Component.translatable(REST_THROUGH_DAY)
					: Component.translatable(VANILLA_SKIPPING_NIGHT);
		}
		return mode == TransitionMode.REST
				? Component.translatable(REST_PLAYERS, sleeping, needed)
				: Component.translatable(VANILLA_PLAYERS_SLEEPING, sleeping, needed);
	}

	private static Optional<String> translatableKey(Component message) {
		ComponentContents contents = message.getContents();
		if (contents instanceof TranslatableContents translatable) {
			return Optional.of(translatable.getKey());
		}
		return Optional.empty();
	}

	private static Object[] translatableArgs(Component message) {
		ComponentContents contents = message.getContents();
		if (contents instanceof TranslatableContents translatable) {
			return translatable.getArgs();
		}
		return new Object[0];
	}
}
