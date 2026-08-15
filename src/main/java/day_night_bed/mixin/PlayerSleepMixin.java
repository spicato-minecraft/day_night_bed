package day_night_bed.mixin;

import day_night_bed.sleep.BedInteractionLogger;
import day_night_bed.sleep.DayNightBedSleepSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps players in bed during Overworld daytime — vanilla {@link Player#tick()} re-checks
 * {@link BedRule#canSleep(Level)} every tick and wakes the player when it is still bright outside.
 */
@Mixin(Player.class)
public abstract class PlayerSleepMixin {

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"
			)
	)
	private boolean dayNightBed$keepSleepingDuringDay(BedRule bedRule, Level level) {
		boolean vanilla = bedRule.canSleep(level);
		if (level instanceof ServerLevel serverLevel) {
			boolean resolved = DayNightBedSleepSupport.resolveCanSleep(bedRule, serverLevel);
			if (vanilla != resolved) {
				BedInteractionLogger.canSleepCheck("Player.tick", bedRule, level, vanilla, resolved);
			}
			return resolved;
		}
		return vanilla;
	}
}
