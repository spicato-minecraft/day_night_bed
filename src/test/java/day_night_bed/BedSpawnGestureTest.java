package day_night_bed;

import day_night_bed.config.DayNightBedConfig;
import day_night_bed.sleep.BedSpawnGesture;
import day_night_bed.sleep.SleepContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedSpawnGestureTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void clearDay_hintsSleepThroughDay() {
		assertHint(context(true, false, false), BedSpawnGesture.CLICK_TO_SLEEP_THROUGH_DAY);
	}

	@Test
	void clearNight_hintsSleepThroughNight() {
		assertHint(context(false, true, false), BedSpawnGesture.CLICK_TO_SLEEP_THROUGH_NIGHT);
	}

	@Test
	void storm_hintsSleepThroughNight() {
		assertHint(context(true, false, true), BedSpawnGesture.CLICK_TO_SLEEP_THROUGH_NIGHT);
	}

	@Test
	void frozenDaylightCycle_hasNoHint() {
		SleepContext frozen = SleepContext.of(true, true, false, false, false, false, DayNightBedConfig.defaults());
		assertTrue(BedSpawnGesture.cycleHint(frozen).isEmpty());
	}

	private static void assertHint(SleepContext context, String key) {
		Component hint = BedSpawnGesture.cycleHint(context).orElseThrow();
		assertEquals(key, ((TranslatableContents) hint.getContents()).getKey());
	}

	private static SleepContext context(boolean isDay, boolean isNight, boolean thundering) {
		return SleepContext.of(true, isDay, isNight, thundering, thundering, true, DayNightBedConfig.defaults());
	}
}
