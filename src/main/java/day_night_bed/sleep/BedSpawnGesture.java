package day_night_bed.sleep;

import day_night_bed.config.DayNightBedConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Shift-click an Overworld bed to set spawn without entering it or advancing time.
 * The action bar names the skip a normal click would take.
 */
public final class BedSpawnGesture {

	public static final String CLICK_TO_SLEEP_THROUGH_DAY = "sleep.day_night_bed.click_to_sleep_through_day";
	public static final String CLICK_TO_SLEEP_THROUGH_NIGHT = "sleep.day_night_bed.click_to_sleep_through_night";

	private BedSpawnGesture() {
	}

	/**
	 * @return true when this click was handled and must not start sleep
	 */
	public static boolean tryHandle(AbstractBedBlock bed, BlockState state, Level level, BlockPos pos, Player player) {
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (!serverPlayer.isShiftKeyDown() || !serverPlayer.isAlive() || serverPlayer.isSpectator()) {
			return false;
		}
		if (!DayNightBedConfig.get().enabled || !SleepContextFactory.isOverworld(serverLevel)) {
			return false;
		}
		if (!state.hasProperty(AbstractBedBlock.PART) || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
			return false;
		}

		BedPart part = state.getValue(AbstractBedBlock.PART);
		Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
		Direction neighbourDirection = part == BedPart.FOOT ? facing : facing.getOpposite();
		BlockPos neighbourPos = pos.relative(neighbourDirection);
		BlockState neighbourState = serverLevel.getBlockState(neighbourPos);
		if (!neighbourState.is(bed) || neighbourState.getValue(AbstractBedBlock.PART) == part) {
			return false;
		}

		BlockPos headPos = part == BedPart.HEAD ? pos : pos.relative(facing);
		BlockState headState = part == BedPart.HEAD ? state : serverLevel.getBlockState(headPos);
		if (headState.getValue(AbstractBedBlock.OCCUPIED)) {
			return false;
		}
		BedRule rule = bed.getBedRule(serverLevel, headPos);
		if (rule.destroyOnUse()) {
			return false;
		}
		if (!inRange(serverPlayer, headPos, facing)) {
			return false;
		}

		LevelData.RespawnData data = LevelData.RespawnData.of(
				serverLevel.dimension(),
				headPos,
				serverPlayer.getYRot(),
				serverPlayer.getXRot()
		);
		serverPlayer.setRespawnPosition(new ServerPlayer.RespawnConfig(data, false), true);
		cycleHint(SleepContextFactory.fromLevel(serverLevel)).ifPresent(serverPlayer::sendOverlayMessage);
		return true;
	}

	/**
	 * Names the skip a normal click would take. Storms skip to morning, so they use the night line.
	 * No hint when a click would not change time.
	 */
	public static Optional<Component> cycleHint(SleepContext context) {
		return switch (SleepTransitionResolver.resolve(context)) {
			case TO_NIGHT -> Optional.of(Component.translatable(CLICK_TO_SLEEP_THROUGH_DAY));
			case TO_DAY, STORM_SKIP_MORNING -> Optional.of(Component.translatable(CLICK_TO_SLEEP_THROUGH_NIGHT));
			case NO_OP -> Optional.empty();
		};
	}

	private static boolean inRange(Player player, BlockPos headPos, Direction facing) {
		return reachable(player, headPos) || reachable(player, headPos.relative(facing.getOpposite()));
	}

	/** Same 3×2×3 box vanilla uses before it will set a bed spawn. */
	private static boolean reachable(Player player, BlockPos pos) {
		Vec3 center = Vec3.atBottomCenterOf(pos);
		return Math.abs(player.getX() - center.x) <= 3.0
				&& Math.abs(player.getY() - center.y) <= 2.0
				&& Math.abs(player.getZ() - center.z) <= 3.0;
	}
}
