package day_night_bed.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Loads every common mixin target so a renamed Minecraft class fails here instead of in a player session.
 */
public class ServerMixinApplySweepGameTest {
	@GameTest(maxTicks = 20)
	public void serverMixinApplySweep(GameTestHelper context) {
		load("net.minecraft.server.level.ServerPlayer");
		load("net.minecraft.world.entity.player.Player");
		load("net.minecraft.server.level.ServerLevel");
		load("net.minecraft.world.level.block.AbstractBedBlock");
		context.succeed();
	}

	private static void load(String name) {
		try {
			Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new AssertionError("Mixin target failed to load: " + name, e);
		}
	}
}
