package day_night_bed.mixin;

import day_night_bed.sleep.BedInteractionLogger;
import day_night_bed.sleep.DayNightBedSleepSupport;
import net.minecraft.server.level.ServerLevel;
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
					target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V",
					ordinal = 0
			)
	)
	private void dayNightBed$redirectSleepSkipTime(ServerLevel instance, long vanillaDawnTime) {
		if (DayNightBedSleepSupport.shouldDeferTimeAdvanceToWake(instance)) {
			DayNightBedSleepSupport.markSleepCompletionPending(instance, vanillaDawnTime);
			return;
		}
		DayNightBedSleepSupport.applySleepTimeAdvance(instance, vanillaDawnTime);
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
