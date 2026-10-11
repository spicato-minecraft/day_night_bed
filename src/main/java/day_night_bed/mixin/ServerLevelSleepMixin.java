package day_night_bed.mixin;

import day_night_bed.sleep.BedInteractionLogger;
import day_night_bed.sleep.DayNightBedSleepSupport;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelSleepMixin {

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/clock/ServerClockManager;moveToTimeMarker(Lnet/minecraft/core/Holder;Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/clock/ServerClockManager$MoveResult;"
			)
	)
	private ServerClockManager.MoveResult dayNightBed$redirectSleepSkipTime(
			ServerClockManager clockManager,
			Holder<WorldClock> clock,
			ResourceKey<ClockTimeMarker> marker
	) {
		ServerLevel level = (ServerLevel) (Object) this;
		if (marker == ClockTimeMarkers.WAKE_UP_FROM_SLEEP && DayNightBedSleepSupport.shouldDeferTimeAdvanceToWake(level)) {
			DayNightBedSleepSupport.markSleepCompletionPending(level);
			// ServerLevel.tick discards this result; MOVED matches the old boolean success.
			return ServerClockManager.MoveResult.MOVED;
		}
		return clockManager.moveToTimeMarker(clock, marker);
	}

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;resetWeatherCycle()V"
			)
	)
	private void dayNightBed$redirectWeatherReset(ServerLevel instance) {
		boolean preserve = DayNightBedSleepSupport.shouldPreserveWeather(instance);
		BedInteractionLogger.weatherReset(instance, preserve, DayNightBedSleepSupport.lastSleepTransition(instance));
		if (preserve) {
			DayNightBedSleepSupport.clearSleepTransition(instance, "weather-preserved");
			return;
		}
		instance.resetWeatherCycle();
		DayNightBedSleepSupport.clearSleepTransition(instance, "weather-reset");
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void dayNightBed$clearStaleTransition(CallbackInfo ci) {
		DayNightBedSleepSupport.endSleepTick((ServerLevel) (Object) this);
	}
}
