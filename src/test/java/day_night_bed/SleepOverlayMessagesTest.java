package day_night_bed;

import day_night_bed.sleep.ClientSleepOverlayMode;
import day_night_bed.sleep.SleepOverlayMessages;
import day_night_bed.sleep.TransitionMode;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepOverlayMessagesTest {

	@Test
	void restModeRewritesSkippingNight() {
		Component vanilla = Component.translatable(SleepOverlayMessages.VANILLA_SKIPPING_NIGHT);
		Component rewritten = SleepOverlayMessages.rewriteWithArgs(vanilla, Optional.of(TransitionMode.REST))
				.orElseThrow();
		assertEquals(SleepOverlayMessages.REST_THROUGH_DAY, getKey(rewritten));
	}

	@Test
	void restModeRewritesPlayerCountPreservingArgs() {
		Component vanilla = Component.translatable(SleepOverlayMessages.VANILLA_PLAYERS_SLEEPING, 1, 2);
		Component rewritten = SleepOverlayMessages.rewriteWithArgs(vanilla, Optional.of(TransitionMode.REST))
				.orElseThrow();
		assertEquals(SleepOverlayMessages.REST_PLAYERS, getKey(rewritten));
		assertEquals(1, ((Number) getArgs(rewritten)[0]).intValue());
		assertEquals(2, ((Number) getArgs(rewritten)[1]).intValue());
	}

	@Test
	void sleepModeLeavesVanillaStrings() {
		Component vanilla = Component.translatable(SleepOverlayMessages.VANILLA_SKIPPING_NIGHT);
		assertTrue(SleepOverlayMessages.rewriteWithArgs(vanilla, Optional.of(TransitionMode.SLEEP)).isEmpty());
	}

	@Test
	void emptyModeLeavesVanillaStrings() {
		Component vanilla = Component.translatable(SleepOverlayMessages.VANILLA_PLAYERS_SLEEPING, 1, 2);
		assertTrue(SleepOverlayMessages.rewriteWithArgs(vanilla, Optional.empty()).isEmpty());
	}

	@Test
	void unrelatedTranslatableIsUntouched() {
		Component other = Component.translatable("chat.type.text", "Steve", "hello");
		assertTrue(SleepOverlayMessages.rewriteWithArgs(other, Optional.of(TransitionMode.REST)).isEmpty());
	}

	@Test
	void localOverlayRestDeepSleep() {
		Component overlay = SleepOverlayMessages.createLocalOverlay(TransitionMode.REST, true, 1, 1);
		assertEquals(SleepOverlayMessages.REST_THROUGH_DAY, getKey(overlay));
	}

	@Test
	void localOverlayRestCounting() {
		Component overlay = SleepOverlayMessages.createLocalOverlay(TransitionMode.REST, false, 1, 2);
		assertEquals(SleepOverlayMessages.REST_PLAYERS, getKey(overlay));
	}

	@Test
	void localOverlaySleepDeepSleepUsesVanilla() {
		Component overlay = SleepOverlayMessages.createLocalOverlay(TransitionMode.SLEEP, true, 1, 1);
		assertEquals(SleepOverlayMessages.VANILLA_SKIPPING_NIGHT, getKey(overlay));
	}

	private static String getKey(Component component) {
		return ((net.minecraft.network.chat.contents.TranslatableContents) component.getContents()).getKey();
	}

	private static Object[] getArgs(Component component) {
		return ((net.minecraft.network.chat.contents.TranslatableContents) component.getContents()).getArgs();
	}
}
