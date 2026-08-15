package day_night_bed.mixin;

import com.mojang.datafixers.util.Either;
import day_night_bed.DayNightBed;
import day_night_bed.sleep.BedInteractionLogger;
import day_night_bed.sleep.DayNightBedSleepSupport;
import day_night_bed.sleep.RestStatPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSleepMixin {

	@Inject(method = "startSleepInBed", at = @At("HEAD"))
	private void dayNightBed$logStartSleepInBedHead(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
		BedInteractionLogger.startSleepInBed((ServerPlayer) (Object) this, pos, "enter", "");
	}

	@Inject(method = "startSleepInBed", at = @At("RETURN"))
	private void dayNightBed$logStartSleepInBedReturn(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
		Either<Player.BedSleepingProblem, Unit> result = cir.getReturnValue();
		String outcome = result.left().isPresent() ? "rejected" : "accepted";
		Object detail = result.left().map(Object::toString).orElse("ok");
		BedInteractionLogger.startSleepInBed((ServerPlayer) (Object) this, pos, outcome, detail);
	}

	@Inject(method = "startSleeping", at = @At("HEAD"))
	private void dayNightBed$onStartSleeping(BlockPos pos, CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		DayNightBedSleepSupport.resetSleepCycle(player.level());
		BedInteractionLogger.startSleeping(player, pos);
	}

	@Inject(method = "stopSleepInBed", at = @At("HEAD"))
	private void dayNightBed$onStopSleepInBed(boolean setSpawn, boolean updateSleepList, CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		DayNightBedSleepSupport.tryApplyTimeOnWake(player);
		BedInteractionLogger.stopSleepInBed(player, setSpawn, updateSleepList);
	}

	@Redirect(
			method = "startSleepInBed",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"
			)
	)
	private boolean dayNightBed$overrideCanSleep(BedRule bedRule, Level level) {
		boolean vanilla = bedRule.canSleep(level);
		if (level instanceof ServerLevel serverLevel) {
			boolean resolved = DayNightBedSleepSupport.resolveCanSleep(bedRule, serverLevel);
			BedInteractionLogger.canSleepCheck("startSleepInBed", bedRule, level, vanilla, resolved);
			return resolved;
		}
		return vanilla;
	}

	@Redirect(
			method = "startSleeping",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerPlayer;resetStat(Lnet/minecraft/stats/Stat;)V"
			)
	)
	private void dayNightBed$conditionalRestStatReset(ServerPlayer instance, Stat<?> stat) {
		if (RestStatPolicy.shouldSkipRestStatReset((ServerPlayer) (Object) this)) {
			DayNightBed.LOGGER.info("[bed] restStatReset skipped for player={}", ((ServerPlayer) (Object) this).getGameProfile().name());
			return;
		}
		instance.resetStat(stat);
	}
}
