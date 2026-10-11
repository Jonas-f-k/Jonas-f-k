package com.shadowslave.mixin;

import com.shadowslave.combat.BeastForms;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Body, eye position, fit checks and collision use the same dimensions on both sides. */
@Mixin(Player.class)
public abstract class PlayerDimensionsMixin {
  @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
  private void shadowslave$beastDimensions(
      Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
    int form = BeastForms.active((Player) (Object) this);
    if (form >= 0) cir.setReturnValue(BeastForms.ALL.get(form).dimensions());
  }
}
