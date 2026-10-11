package com.shadowslave.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shadowslave.entity.NightmareCreature;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public final class NightmareRenderer extends MobRenderer<NightmareCreature, NightmareModel> {
  private static final ResourceLocation SKIN =
      net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
  private static final RenderType GLOW =
      RenderType.eyes(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
  private static final int[] COLORS = {
    0xFFFFAF61, 0xFFCBCAD8, 0xFF91DAD4, 0xFFFFD799, 0xFFB5D897, 0xFFE8E4DC, 0xFFC3C9FA
  };

  public NightmareRenderer(EntityRendererProvider.Context context) {
    super(context, new NightmareModel(context.bakeLayer(NightmareModel.LAYER)), .7f);
    addLayer(
        new RenderLayer<NightmareCreature, NightmareModel>(this) {
          @Override
          public void render(
              PoseStack pose,
              MultiBufferSource buffers,
              int light,
              NightmareCreature mob,
              float swing,
              float amount,
              float partial,
              float age,
              float yaw,
              float pitch) {
            if (!mob.isInvisible())
              getParentModel()
                  .renderGlow(
                      pose,
                      net.minecraft.client.Minecraft.getInstance()
                          .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                          .apply(ResourceLocation.withDefaultNamespace("block/white_concrete"))
                          .wrap(buffers.getBuffer(GLOW)),
                      mob.isEcho() ? 0xFFB8E6EF : mob.speciesId().equals("forsaken_knight") ? 0xFFDC554C : COLORS[Math.clamp(mob.family(), 0, 6)],
                      mob.isBoss());
          }
        });
  }

  @Override
  public ResourceLocation getTextureLocation(NightmareCreature entity) {
    return SKIN;
  }

  @Override
  protected void scale(NightmareCreature mob, PoseStack pose, float partial) {
    var s = mob.species();
    var size =
        s == null
            ? NightmareCreature.bodyDimensions(mob.family(), mob.isBoss())
            : net.minecraft.world.entity.EntityDimensions.scalable(s.width(), s.height());
    pose.scale(size.width(), size.height() / 2, size.width());
  }

  @Override
  protected float getShadowRadius(NightmareCreature mob) {
    return mob.getBbWidth() * .42f;
  }
}
