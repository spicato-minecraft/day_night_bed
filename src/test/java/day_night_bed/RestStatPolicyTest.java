package day_night_bed;

import day_night_bed.config.DayNightBedConfig;
import day_night_bed.sleep.RestStatPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class RestStatPolicyTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void dayRest_defaultConfig_skipsReset() {
		DayNightBedConfig.setForTest(DayNightBedConfig.defaults());
		var player = mockPlayer(true, false);
		assertTrue(RestStatPolicy.shouldSkipRestStatReset(player));
	}

	@Test
	void nightSleep_neverSkipsReset() {
		DayNightBedConfig.setForTest(DayNightBedConfig.defaults());
		var player = mockPlayer(false, false);
		assertFalse(RestStatPolicy.shouldSkipRestStatReset(player));
	}

	@Test
	void storm_neverSkipsResetEvenDuringDay() {
		DayNightBedConfig.setForTest(DayNightBedConfig.defaults());
		var player = mockPlayer(true, true);
		assertFalse(RestStatPolicy.shouldSkipRestStatReset(player));
	}

	@Test
	void configOptIn_resetsOnDayRest() {
		DayNightBedConfig config = DayNightBedConfig.defaults();
		config.resetRestStatOnDayRest = true;
		DayNightBedConfig.setForTest(config);

		var player = mockPlayer(true, false);
		assertFalse(RestStatPolicy.shouldSkipRestStatReset(player));
	}

	@Test
	void disabledMod_neverSkipsReset() {
		DayNightBedConfig config = DayNightBedConfig.defaults();
		config.enabled = false;
		DayNightBedConfig.setForTest(config);

		var player = mockPlayer(true, false);
		assertFalse(RestStatPolicy.shouldSkipRestStatReset(player));
	}

	private static net.minecraft.server.level.ServerPlayer mockPlayer(boolean isDay, boolean isThundering) {
		var level = Mockito.mock(net.minecraft.server.level.ServerLevel.class);
		when(level.dimension()).thenReturn(net.minecraft.world.level.Level.OVERWORLD);
		when(level.isBrightOutside()).thenReturn(isDay);
		when(level.isDarkOutside()).thenReturn(!isDay);
		when(level.isThundering()).thenReturn(isThundering);

		var player = Mockito.mock(net.minecraft.server.level.ServerPlayer.class);
		when(player.level()).thenReturn(level);
		return player;
	}
}
