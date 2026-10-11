package com.shadowslave.client;

import com.mojang.math.Axis;
import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.combat.BeastForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.event.*;

public final class BeastRendering {
  private static BeastModel model;
  private static final int[] LIGHT = {
    0xFFFFBF62, 0xFFDADCE9, 0xFF78E3DA, 0xFFFFD687, 0xFFB6DF96, 0xFFFFECBF, 0xFFBFC4FF
  };

  public static void bake(EntityModelSet models) {
    model = new BeastModel(models.bakeLayer(BeastModel.LAYER));
  }

  public static void render(RenderPlayerEvent.Pre e) {
    if (model == null || !(e.getEntity() instanceof AbstractClientPlayer p)) return;
    int form = BeastForms.active(p);
    if (form < 0) return;
    e.setCanceled(true);
    if (p.isInvisible()) return;
    float partial = e.getPartialTick(),
        body = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot),
        yaw = Mth.rotLerp(partial, p.yHeadRotO, p.yHeadRot) - body;
    boolean breathing =
        form == 0
            && com.shadowslave.network.SpellNetwork.beastAttacks.getOrDefault(p.getUUID(), 0L)
                > p.level().getGameTime();
    model.animate(
        form,
        p.walkAnimation.position(partial),
        p.walkAnimation.speed(partial),
        p.tickCount + partial,
        yaw,
        p.getXRot(),
        p.getAttackAnim(partial),
        p.getAbilities().flying || !p.onGround() && BeastForms.ALL.get(form).flies(),
        breathing);
    var pose = e.getPoseStack();
    pose.pushPose();
    pose.mulPose(Axis.YP.rotationDegrees(180 - body));
    pose.scale(-p.getBbWidth(), -p.getBbHeight() / 2, p.getBbWidth());
    pose.translate(0, -1.5, 0);
    var atlas = InventoryMenu.BLOCK_ATLAS;
    model.renderToBuffer(
        pose,
        e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(atlas)),
        e.getPackedLight(),
        LivingEntityRenderer.getOverlayCoords(p, 0),
        0xFFFFFFFF);
    var rune =
        Minecraft.getInstance()
            .getTextureAtlas(atlas)
            .apply(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                    "block/white_concrete"));
    model.glow(
        pose, rune.wrap(e.getMultiBufferSource().getBuffer(RenderType.eyes(atlas))), LIGHT[form]);
    pose.popPose();
  }

  public static void arm(RenderArmEvent e) {
    if (model == null) return;
    int form = BeastForms.active(e.getPlayer());
    if (form < 0) return;
    e.setCanceled(true);
    if (e.getPlayer().isInvisible()) return;
    model.animate(form, 0, 0, e.getPlayer().tickCount, 0, 0, 0, false, false);
    int sign = e.getArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? -1 : 1;
    var arm = model.claw(sign);
    arm.x = sign * 5;
    arm.y = 2;
    arm.z = 0;
    arm.xRot = arm.yRot = arm.zRot = 0;
    var atlas = InventoryMenu.BLOCK_ATLAS;
    var sprite =
        Minecraft.getInstance()
            .getTextureAtlas(atlas)
            .apply(ShadowSlaveMod.id("entity/beast_" + form));
    arm.render(
        e.getPoseStack(),
        sprite.wrap(e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(atlas))),
        e.getPackedLight(),
        OverlayTexture.NO_OVERLAY,
        0xFFFFFFFF);
  }
}
