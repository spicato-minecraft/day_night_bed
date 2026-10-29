package day_night_bed.gametest;

import day_night_bed.gametest.BedSleepHarness.Sleeper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Shift-click sets spawn and leaves time alone. A normal click still starts sleep.
 */
public class BedSpawnGameTest {

	@GameTest(environment = "day_night_bed_test:shift_click_day_sets_spawn", maxTicks = 40, skyAccess = true)
	public void shiftClickDuringDaySetsSpawnWithoutSleeping(GameTestHelper context) {
		assertShiftClickSetsSpawn(context, true, Blocks.BED.red());
	}

	@GameTest(environment = "day_night_bed_test:shift_click_night_sets_spawn", maxTicks = 40, skyAccess = true)
	public void shiftClickDuringNightSetsSpawnWithoutSleeping(GameTestHelper context) {
		assertShiftClickSetsSpawn(context, false, Blocks.BED.red());
	}

	@GameTest(environment = "day_night_bed_test:shift_click_straw_sets_spawn", maxTicks = 40, skyAccess = true)
	public void shiftClickStrawBedSetsSpawnAndKeepsBed(GameTestHelper context) {
		assertShiftClickSetsSpawn(context, true, Blocks.STRAW_BED);
	}

	@GameTest(environment = "day_night_bed_test:click_still_sleeps", maxTicks = 40, skyAccess = true)
	public void clickStillSleepsDuringDay(GameTestHelper context) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setClearWeather(level);
		BedSleepHarness.requireDay(context);
		Sleeper sleeper = BedSleepHarness.sleeperAt(context, BedSleepHarness.BED_FOOT);
		context.useBlock(BedSleepHarness.BED_FOOT, sleeper.player());
		context.assertTrue(sleeper.player().isSleeping(), "a normal click should still start sleep");
		context.succeed();
	}

	private static void assertShiftClickSetsSpawn(GameTestHelper context, boolean day, Block bedBlock) {
		ServerLevel level = BedSleepHarness.begin(context);
		BedSleepHarness.setClearWeather(level);
		if (day) {
			BedSleepHarness.requireDay(context);
		} else {
			BedSleepHarness.requireNight(context);
		}
		Sleeper sleeper = BedSleepHarness.sleeperAt(context, BedSleepHarness.BED_FOOT, bedBlock);
		ServerPlayer player = sleeper.player();
		long before = level.getDefaultClockTime();
		player.setShiftKeyDown(true);
		context.useBlock(BedSleepHarness.BED_FOOT, player);

		context.assertFalse(player.isSleeping(), "shift-click should not put the player in bed");
		context.assertValueEqual(level.getDefaultClockTime(), before, "clock");
		BedSleepHarness.assertBedRemains(context, sleeper.bed());

		BlockPos expectedSpawn = sleeper.bed().relative(Direction.EAST);
		ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
		context.assertTrue(respawn != null, "shift-click should set a respawn");
		context.assertValueEqual(respawn.respawnData().pos(), expectedSpawn, "spawn");
		context.assertFalse(respawn.forced(), "bed spawn should not be forced");
		context.succeed();
	}
}
