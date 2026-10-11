package com.shadowslave.client;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.nightmare.Nightmares;
import com.shadowslave.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Gently blended air fog only; immersion and status-effect fog retain their vanilla behavior. */
@EventBusSubscriber(modid = ShadowSlaveMod.ID, value = Dist.CLIENT)
public final class RealmAtmosphere {
  private static float warmth;

  private static boolean inRealm() {
    var l = Minecraft.getInstance().level;
    return l != null && (DreamRealm.isDream(l) || Nightmares.isTrial(l));
  }

  private static boolean clearVision() {
    var p = Minecraft.getInstance().player;
    return p != null && !p.hasEffect(MobEffects.BLINDNESS) && !p.hasEffect(MobEffects.DARKNESS);
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    var mc = Minecraft.getInstance();
    if (!inRealm() || mc.player == null) {
      warmth = 0;
      return;
    }
    float target = Sanctuary.isSafe(mc.level, mc.player.blockPosition()) ? 1 : 0;
    warmth += (target - warmth) * .055f;
  }

  @SubscribeEvent
  public static void color(ViewportEvent.ComputeFogColor event) {
    if (!inRealm()
        || DreamRealm.isDream(Minecraft.getInstance().level)
        || !clearVision()
        || event.getCamera().getFluidInCamera() != FogType.NONE) return;
    event.setRed(event.getRed() * .58f + (.21f + warmth * .19f) * .42f);
    event.setGreen(event.getGreen() * .58f + (.23f + warmth * .11f) * .42f);
    event.setBlue(event.getBlue() * .58f + (.25f + warmth * .015f) * .42f);
  }

  @SubscribeEvent
  public static void fog(ViewportEvent.RenderFog event) {
    if (!inRealm()
        || !clearVision()
        || event.getType() != FogType.NONE
        || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
    if (DreamRealm.isDream(Minecraft.getInstance().level)) return;
    float far = Math.min(event.getFarPlaneDistance(), 96 + warmth * 64);
    event.setFarPlaneDistance(far);
    event.setNearPlaneDistance(
        Math.min(event.getNearPlaneDistance(), far * (.22f + warmth * .33f)));
    event.setCanceled(true);
  }
}
