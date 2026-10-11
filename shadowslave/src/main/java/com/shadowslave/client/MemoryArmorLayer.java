package com.shadowslave.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.content.Content;
import com.shadowslave.network.SpellNetwork;
import java.util.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Soul armor follows the player's actual pose; specialty pieces replace their body region. */
public final class MemoryArmorLayer
    extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
  private static final String[] STYLES = {
    "hood", "knight", "raven", "antlers", "sun", "ice", "petal", "coral", "oracle", "horns", "leaf",
    "nomad", "wings", "gear", "halo"
  };
  private static MemoryArmorLayer firstPerson;
  private final Map<String, HumanoidModel<AbstractClientPlayer>> shells = new HashMap<>(),
      trims = new HashMap<>();

  private static ModelLayerLocation layer(String style, boolean trim) {
    return new ModelLayerLocation(
        ShadowSlaveMod.id("memory_armor_" + style), trim ? "trim" : "main");
  }

  public static void register(EntityRenderersEvent.RegisterLayerDefinitions e) {
    for (String s : STYLES)
      for (boolean trim : new boolean[] {false, true})
        e.registerLayerDefinition(layer(s, trim), () -> mesh(s, trim));
  }

  public MemoryArmorLayer(
      RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
      EntityModelSet models) {
    super(parent);
    firstPerson = this;
    for (String s : STYLES) {
      shells.put(s, new HumanoidModel<>(models.bakeLayer(layer(s, false))));
      trims.put(s, new HumanoidModel<>(models.bakeLayer(layer(s, true))));
    }
  }

  private static CubeListBuilder box() {
    return CubeListBuilder.create().texOffs(0, 0);
  }

  private static LayerDefinition mesh(String style, boolean trim) {
    MeshDefinition mesh = new MeshDefinition();
    var root = mesh.getRoot();
    var head = box();
    var body = box();
    var arm = box();
    var leg = box();
    float extra = .55f;
    var d = new CubeDeformation(extra);
    if (!trim) {
      head.addBox(-4, -8, -4, 8, 2, 8, d)
          .addBox(-4, -6, -4, 1, 6, 8, d)
          .addBox(3, -6, -4, 1, 6, 8, d)
          .addBox(-3, -6, 3, 6, 6, 1, d);
      if (!List.of("hood", "oracle", "petal", "nomad").contains(style))
        head.addBox(-3, -3, -4.2f, 6, 3, 1, d);
      body.addBox(-4, 0, -2, 8, 5, 4, d)
          .addBox(-3.7f, 5, -2.4f, 7.4f, 3.5f, 4.8f)
          .addBox(-4.4f, 9, -2.8f, 8.8f, 3, 5.6f)
          .addBox(-3.5f, 1, 2.5f, 7, 14, .6f);
      arm.addBox(-2, -2, -2, 4, 7, 4, d).addBox(-2, 7, -2, 4, 3, 4, d);
      leg.addBox(-2, 0, -2, 4, 12, 4, d);
      if (List.of("hood", "oracle", "petal", "nomad", "raven").contains(style)) {
        body.addBox(-4.6f, 10, -2.6f, 9.2f, 4, 5.2f);
        leg.addBox(-2.5f, 0, -2.5f, 5, 5, 5);
      }
      if (List.of("knight", "gear", "ice", "horns").contains(style))
        arm.addBox(-3, -3, -3, 6, 3, 6);
      if (List.of("petal", "leaf", "antlers").contains(style)) {
        arm.addBox(-3.5f, -3, -3, 7, 2, 6)
            .addBox(-3, -1, -3.4f, 6, 3, 6.8f)
            .addBox(-2.5f, 2, -3, 5, 2, 6);
        body.addBox(-5, 10, -3, 3, 6, 1).addBox(2, 10, -3, 3, 6, 1).addBox(-2, 12, -3.2f, 4, 5, 1);
      }
      if (style.equals("wings") || style.equals("raven"))
        body.addBox(-9, 1, 2.6f, 5, 11, 1).addBox(4, 1, 2.6f, 5, 11, 1);
      // Broad sculpted relief, separate joints and cloth panels rather than a recolored vanilla
      // shell.
      body.addBox(-3.6f, 1, -3.0f, 3.1f, 3, .7f)
          .addBox(.5f, 1, -3.0f, 3.1f, 3, .7f)
          .addBox(-3.5f, 5, -3.1f, 7, 1, .6f);
      arm.addBox(-2.3f, 7, -2.8f, 4.6f, 2, .8f).addBox(-2.4f, 10, -2.6f, 4.8f, 1.2f, 4.8f);
      switch (style) {
        case "knight" -> {
          head.addBox(-3.5f, -6.2f, -5.0f, 7, 1, .6f).addBox(-.5f, -10, -1, 1, 3, 4);
          body.addBox(-1.4f, 1, -3.5f, 2.8f, 5, .6f);
          arm.addBox(-3.4f, -3.8f, -3.2f, 6.8f, 1.4f, 6.4f).addBox(-3.1f, -2, -3, 6.2f, 2, 6);
        }
        case "hood" -> {
          head.addBox(-4.5f, -9, -4.2f, 9, 1.2f, 8.4f);
          body.addBox(-4.9f, -1, -3, 9.8f, 2, 6).addBox(-4.1f, 4, 3, 8.2f, 13, .7f);
        }
        case "raven" -> {
          arm.addBox(-4, -3, -3, 8, 2, 6)
              .addBox(-3.5f, -1, -3, 7, 2, 6)
              .addBox(-3, 1, -2.7f, 6, 2, 5.4f);
          body.addBox(-5, 9, 3, 3, 8, .8f).addBox(2, 9, 3, 3, 8, .8f);
        }
        case "antlers" -> {
          head.addBox(-3.7f, -7, -5, 7.4f, 2, .7f);
          arm.addBox(-3, -3, -3.6f, 6, 4, 1);
          body.addBox(-.8f, 1, -3.7f, 1.6f, 6, .7f);
        }
        case "sun" -> {
          arm.addBox(-3.7f, -3.5f, -3, 7.4f, 2, 6).addBox(-3, -1, -3.2f, 6, 2, 6.4f);
          body.addBox(-3, 2, -3.3f, 6, 3, .8f).addBox(-3.2f, 11, -3.4f, 6.4f, 4, .7f);
        }
        case "ice" -> {
          head.addBox(-3, -9, -3, 6, 1.4f, 6);
          arm.addBox(-3.6f, -5, -1, 2, 4, 2).addBox(1.6f, -4, -1, 2, 3, 2);
          body.addBox(-3.8f, 2, -3.1f, 2, 5, .7f).addBox(1.8f, 2, -3.1f, 2, 5, .7f);
        }
        case "petal" -> {
          head.addBox(-4.8f, -8.8f, -2.8f, 3, 2, 3).addBox(1.8f, -8.8f, -2.8f, 3, 2, 3);
          body.addBox(-4.3f, 8, -3.2f, 3, 7, .7f)
              .addBox(1.3f, 8, -3.2f, 3, 7, .7f)
              .addBox(-1.5f, 10, -3.7f, 3, 6, .7f);
        }
        case "coral" -> {
          arm.addBox(-3.5f, -3.5f, -2.5f, 7, 2, 5).addBox(-3, -2, -3, 6, 3, 6);
          head.addBox(-5, -5, 0, 1.2f, 6, 4).addBox(3.8f, -5, 0, 1.2f, 6, 4);
          body.addBox(-3.7f, 2, -3.4f, 7.4f, 1, .7f).addBox(-3.5f, 4, -3.4f, 7, 1, .7f);
        }
        case "oracle" -> {
          body.addBox(-4.5f, -3, 1.5f, 9, 4, 1)
              .addBox(-4.2f, 11, -3.4f, 2.5f, 7, .7f)
              .addBox(1.7f, 11, -3.4f, 2.5f, 7, .7f);
          head.addBox(-4.5f, -9, -4.5f, 9, 1, 9);
        }
        case "horns" -> {
          arm.addBox(-3.3f, -5, -1.4f, 2, 4, 2.8f).addBox(1.3f, -4, -1.4f, 2, 3, 2.8f);
          body.addBox(-3.7f, 2, -3.4f, 7.4f, 2, .8f).addBox(-3.2f, 5, -3.2f, 6.4f, 2, .6f);
        }
        case "leaf" -> {
          body.addBox(-4.5f, 10, -3.5f, 3.3f, 6, .7f).addBox(1.2f, 10, -3.5f, 3.3f, 6, .7f);
          arm.addBox(-3.3f, -4, -3, 6.6f, 2, 6);
          head.addBox(-3.5f, -9, -3.5f, 7, 1, 7);
        }
        case "nomad" -> {
          body.addBox(-4.4f, -1, -3.1f, 8.8f, 2, 6.2f)
              .addBox(-2.6f, 1, -3.4f, 5.2f, 7, .7f)
              .addBox(3, 8, -1.5f, 2, 3, 3);
          arm.addBox(-2.6f, 5, -2.6f, 5.2f, 1, 5.2f);
        }
        case "wings" -> {
          for (int i = 0; i < 3; i++)
            body.addBox(-10 + i, 2 + i * 3, 3, 4, 5, 1).addBox(6 - i, 2 + i * 3, 3, 4, 5, 1);
          arm.addBox(-3, -3, -3, 6, 2, 6);
        }
        case "gear" -> {
          body.addBox(-3.8f, 0, -3, 1.3f, 8, 1)
              .addBox(2.5f, 0, -3, 1.3f, 8, 1)
              .addBox(-2.2f, 3, -3.5f, 4.4f, 3, 1);
          arm.addBox(-3.4f, -3, -3.4f, 6.8f, 2, 6.8f).addBox(-2.6f, 7, 2, 5.2f, 2, 1.5f);
        }
        case "halo" -> {
          body.addBox(-3.8f, 0, -3.2f, 7.6f, 2, .7f).addBox(-1, 2, -3.6f, 2, 5, .7f);
          arm.addBox(-3, -3, -3, 6, 2, 6);
        }
      }
    } else {
      body.addBox(-4.7f, 8, -2.7f, 9.4f, 1.4f, 5.4f)
          .addBox(-1, 2, -3.2f, 2, 3, 1)
          .addBox(-3.8f, 0, -2.8f, 1, 6, .5f)
          .addBox(2.8f, 0, -2.8f, 1, 6, .5f);
      arm.addBox(-2.7f, 6.5f, -2.7f, 5.4f, 1.2f, 5.4f);
      leg.addBox(-2.7f, 8, -2.7f, 5.4f, 1.2f, 5.4f);
      if (List.of("antlers", "horns", "coral", "leaf").contains(style)) {
        for (int sign : new int[] {-1, 1}) {
          head.addBox(sign < 0 ? -6 : 4, -12, 0, 2, 6, 2);
          head.addBox(sign < 0 ? -8 : 6, -13, 0, 2, 2, 2);
          if (!style.equals("horns")) head.addBox(sign < 0 ? -7 : 5, -16, 0, 1, 4, 1);
        }
      } else if (style.equals("halo") || style.equals("sun")) {
        head.addBox(-6, -11, -5, 12, 1, 1)
            .addBox(-6, -11, 4, 12, 1, 1)
            .addBox(-6, -11, -4, 1, 1, 8)
            .addBox(5, -11, -4, 1, 1, 8);
      } else if (style.equals("ice") || style.equals("oracle")) {
        head.addBox(-1, -13, -1, 2, 5, 2).addBox(-4, -10, -1, 1, 3, 1).addBox(3, -10, -1, 1, 3, 1);
      } else if (style.equals("petal")) {
        head.addBox(-5, -9, -3, 3, 2, 3).addBox(2, -9, -3, 3, 2, 3).addBox(-1, -10, -4, 2, 2, 2);
      } else if (style.equals("wings") || style.equals("raven")) {
        head.addBox(-6, -9, 0, 2, 5, 3).addBox(4, -9, 0, 2, 5, 3);
      } else if (style.equals("gear")) {
        head.addBox(-5, -6, -5, 4, 3, 1).addBox(1, -6, -5, 4, 3, 1);
      } else if (style.equals("nomad")) {
        head.addBox(-4, -4, -5, 8, 2, 1);
        body.addBox(-2, 0, -3, 4, 8, 1);
      } else head.addBox(-4, -6, -5, 8, 1, 1);
    }
    root.addOrReplaceChild("head", head, PartPose.ZERO);
    root.addOrReplaceChild("hat", box(), PartPose.ZERO);
    root.addOrReplaceChild("body", body, PartPose.ZERO);
    root.addOrReplaceChild("right_arm", box(), PartPose.offset(-5, 2, 0))
        .addOrReplaceChild("shell", arm, PartPose.offset(-1, 0, 0));
    root.addOrReplaceChild("left_arm", box(), PartPose.offset(5, 2, 0))
        .addOrReplaceChild("shell", arm, PartPose.offset(1, 0, 0));
    for (String side : List.of("right", "left")) {
      var limb =
          root.addOrReplaceChild(
              side + "_leg", box(), PartPose.offset(side.equals("right") ? -1.9f : 1.9f, 12, 0));
      var upper = box();
      var boot = box();
      if (trim) {
        upper.addBox(-2.7f, 1, -2.7f, 5.4f, 1, 5.4f);
        boot.addBox(-2.7f, 8, -2.7f, 5.4f, 1.2f, 5.4f);
      } else {
        upper.addBox(-2, 0, -2, 4, 8, 4, d).addBox(-1.8f, 4, -2.9f, 3.6f, 3, .8f);
        boot.addBox(-2, 8, -2.5f, 4, 4, 4.5f, d)
            .addBox(-2.2f, 10, -3.4f, 4.4f, 2, 1)
            .addBox(-1.8f, 8, -3.1f, 3.6f, 1, .8f);
      }
      limb.addOrReplaceChild("leggings", upper, PartPose.ZERO);
      limb.addOrReplaceChild("boots", boot, PartPose.ZERO);
    }
    return LayerDefinition.create(mesh, 64, 64);
  }

  private static String material(Content.Memory m, boolean trim) {
    if (trim)
      return "armor/material_"
          + (m.silhouette().equals("ice") || m.silhouette().equals("halo") ? 13 : 12);
    int tile =
        switch (m.silhouette()) {
          case "hood" -> 1;
          case "raven" -> 10;
          case "antlers", "leaf" -> 3;
          case "sun", "halo" -> 4;
          case "ice" -> 6;
          case "petal" -> 5;
          case "coral" -> 7;
          case "oracle" -> 8;
          case "horns" -> 9;
          case "nomad" -> 11;
          case "wings" -> 13;
          default -> 0;
        };
    return "armor/material_" + tile;
  }

  private static int tint(int rgb) {
    int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
    return 0xFF000000
        | ((160 + r * 95 / 255) << 16)
        | ((160 + g * 95 / 255) << 8)
        | (160 + b * 95 / 255);
  }

  public static void renderArm(net.neoforged.neoforge.client.event.RenderArmEvent e) {
    if (firstPerson == null || e.getPlayer().isInvisible()) return;
    Content.Memory full = null, chest = null;
    for (String id : SpellNetwork.appearances.getOrDefault(e.getPlayer().getUUID(), List.of())) {
      var m = Content.memories.get(id);
      if (m != null) {
        if (m.type().equals("armor")) full = m;
        if (m.type().equals("chest")) chest = m;
      }
    }
    var memory = chest != null ? chest : full;
    if (memory == null) return;
    var atlas = net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
    for (boolean trim : new boolean[] {false, true}) {
      var sprite =
          net.minecraft.client.Minecraft.getInstance()
              .getTextureAtlas(atlas)
              .apply(ShadowSlaveMod.id(material(memory, trim)));
      var map = trim ? firstPerson.trims : firstPerson.shells;
      var model = map.getOrDefault(memory.silhouette(), map.get("knight"));
      var arm =
          e.getArm() == net.minecraft.world.entity.HumanoidArm.RIGHT
              ? model.rightArm
              : model.leftArm;
      arm.resetPose();
      arm.xRot = arm.yRot = arm.zRot = 0;
      arm.visible = true;
      arm.render(
          e.getPoseStack(),
          sprite.wrap(e.getMultiBufferSource().getBuffer(trim ? RenderType.eyes(atlas) : RenderType.entityCutoutNoCull(atlas))),
          trim ? 0xF000F0 : e.getPackedLight(),
          OverlayTexture.NO_OVERLAY,
          tint(trim ? memory.trim() : memory.color()));
    }
  }

  @Override
  public void render(
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      float limbSwing,
      float limbAmount,
      float partial,
      float age,
      float yaw,
      float pitch) {
    if (player.isInvisible()) return;
    var ids = SpellNetwork.appearances.getOrDefault(player.getUUID(), List.of());
    var pieces = new HashMap<String, Content.Memory>();
    Content.Memory full = null;
    for (String id : ids) {
      var m = Content.memories.get(id);
      if (m == null) continue;
      if (m.type().equals("armor")) full = m;
      else pieces.put(m.type(), m);
    }
    for (String slot : List.of("head", "chest", "legs", "feet")) {
      var m = pieces.getOrDefault(slot, full);
      if (m == null) continue;
      for (boolean trim : new boolean[] {false, true}) {
        var model =
            (trim ? trims : shells)
                .getOrDefault(m.silhouette(), (trim ? trims : shells).get("knight"));
        getParentModel().copyPropertiesTo(model);
        model.setAllVisible(false);
        model.head.visible = slot.equals("head");
        model.body.visible = slot.equals("chest");
        model.leftArm.visible = model.rightArm.visible = slot.equals("chest");
        model.leftLeg.visible = model.rightLeg.visible = slot.equals("legs") || slot.equals("feet");
        for (var limb : List.of(model.leftLeg, model.rightLeg)) {
          limb.getChild("leggings").visible = slot.equals("legs");
          limb.getChild("boots").visible = slot.equals("feet");
        }
        var atlas = net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
        var sprite =
            net.minecraft.client.Minecraft.getInstance()
                .getTextureAtlas(atlas)
                .apply(ShadowSlaveMod.id(material(m, trim)));
        model.renderToBuffer(
            pose,
            sprite.wrap(buffers.getBuffer(trim ? RenderType.eyes(atlas) : RenderType.entityCutoutNoCull(atlas))),
            trim ? 0xF000F0 : light,
            OverlayTexture.NO_OVERLAY,
            tint(trim ? m.trim() : m.color()));
      }
    }
  }
}
