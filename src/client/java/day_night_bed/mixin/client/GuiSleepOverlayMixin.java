package day_night_bed.mixin.client;

import day_night_bed.sleep.ClientSleepOverlayMode;
import day_night_bed.sleep.SleepOverlayMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Hud.class)
public abstract class GuiSleepOverlayMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@ModifyVariable(method = "setOverlayMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Component dayNightBed$rewriteSleepOverlay(Component message) {
		return SleepOverlayMessages.rewriteWithArgs(
				message,
				ClientSleepOverlayMode.fromLevel(minecraft.level)
		).orElse(message);
	}
}
