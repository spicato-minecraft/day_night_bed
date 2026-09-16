package day_night_bed;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Initializes Minecraft registries for Fabric Loader JUnit unit tests.
 */
public final class MinecraftTestBootstrap {

	private static boolean initialized;

	private MinecraftTestBootstrap() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		net.minecraft.core.registries.BuiltInRegistries.ITEM.listElements()
				.forEach(ref -> ref.bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY));
		initialized = true;
	}
}
