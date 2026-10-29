package day_night_bed.mixin;

import day_night_bed.sleep.BedSpawnGesture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBedBlock.class)
public abstract class AbstractBedBlockUseMixin {

	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void dayNightBed$shiftClickSetsSpawn(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (BedSpawnGesture.tryHandle((AbstractBedBlock) (Object) this, state, level, pos, player)) {
			cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
		}
	}
}
