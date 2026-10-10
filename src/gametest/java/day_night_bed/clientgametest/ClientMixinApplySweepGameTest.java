package day_night_bed.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Loads the client mixin target on the client thread. A renamed Hud class fails here.
 */
public class ClientMixinApplySweepGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.computeOnClient(client -> {
			try {
				Class.forName("net.minecraft.client.gui.Hud");
			} catch (ClassNotFoundException e) {
				throw new AssertionError("Mixin target failed to load: Hud", e);
			}
			return null;
		});
	}
}
