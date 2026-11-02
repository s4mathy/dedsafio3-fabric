package com.dedsafio3.mixin;

import com.dedsafio3.ModEffects;
import com.dedsafio3.SoulIcons;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Añade el icono de alma al nombre de los jugadores que aún tienen su alma (nametag, chat, lista). */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
	/** Con el efecto Keep Inventory no se suelta el inventario al morir (se copia al reaparecer, ver ChangeHooks). */
	@Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
	private void dedsafio3$keepInventory(CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (ModEffects.KEEP_INVENTORY != null && self.hasStatusEffect(ModEffects.KEEP_INVENTORY)) {
			ci.cancel();
		}
	}

	@Inject(method = "getXpToDrop", at = @At("HEAD"), cancellable = true)
	private void dedsafio3$keepXp(CallbackInfoReturnable<Integer> cir) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (ModEffects.KEEP_INVENTORY != null && self.hasStatusEffect(ModEffects.KEEP_INVENTORY)) {
			cir.setReturnValue(0);
		}
	}

	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void dedsafio3$soulIcon(CallbackInfoReturnable<Text> cir) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (SoulIcons.has(self.getUuid())) {
			cir.setReturnValue(Text.empty().append(cir.getReturnValue()).append(Text.literal(" ")).append(SoulIcons.icon()));
		}
	}
}
