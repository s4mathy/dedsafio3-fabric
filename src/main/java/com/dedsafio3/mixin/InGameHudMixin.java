package com.dedsafio3.mixin;

import com.dedsafio3.client.ReviilClientState;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Durante el Momento Reviil los jugadores "no pueden observar su vida": se omite el dibujo de los corazones.
 * {@code require = 0}: si una versión futura renombra el método, el mod sigue cargando (solo dejaría de ocultarla).
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true, require = 0)
	private void dedsafio3$hideHealth(CallbackInfo ci) {
		if (ReviilClientState.hideHealth) {
			ci.cancel();
		}
	}
}
