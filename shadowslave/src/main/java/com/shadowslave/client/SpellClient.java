package com.shadowslave.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.content.Content;
import com.shadowslave.core.Rules;
import com.shadowslave.network.SpellNetwork;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.nbt.Tag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ShadowSlaveMod.ID, value = Dist.CLIENT)
public final class SpellClient {
  public static final KeyMapping OPEN = key("soul_sea", GLFW.GLFW_KEY_O),
      CAST = key("cast", GLFW.GLFW_KEY_R),
      AWAKENED = key("awakened", GLFW.GLFW_KEY_G),
      ASCENDED = key("ascended", GLFW.GLFW_KEY_H),
      TRANSFORM = key("transform", GLFW.GLFW_KEY_J),
      SUMMON = key("summon", GLFW.GLFW_KEY_V),
      TOGGLE = key("toggle", GLFW.GLFW_KEY_B);

  private static KeyMapping key(String name, int code) {
    return new KeyMapping(
        "key.shadowslave." + name, InputConstants.Type.KEYSYM, code, "key.categories.shadowslave");
  }

  @EventBusSubscriber(
      modid = ShadowSlaveMod.ID,
      value = Dist.CLIENT,
      bus = EventBusSubscriber.Bus.MOD)
  public static final class Registration {
    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) {
      e.enqueueWork(
          () -> {
            for (var item :
                java.util.List.of(
                    ShadowSlaveMod.MEMORY.get(), ShadowSlaveMod.MANIFESTED_BLADE.get()))
              net.minecraft.client.renderer.item.ItemProperties.register(
                  item,
                  ShadowSlaveMod.id("design"),
                  (stack, level, entity, seed) -> {
                    var m =
                        Content.memories.get(
                            com.shadowslave.memory.MemoryTokenItem.memoryId(stack));
                    return m == null ? 0 : m.design();
                  });
          });
    }

    @SubscribeEvent
    public static void armor(EntityRenderersEvent.AddLayers e) {
      BeastRendering.bake(e.getEntityModels());
      for (var skin : e.getSkins()) {
        net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = e.getSkin(skin);
        if (renderer != null)
          renderer.addLayer(new MemoryArmorLayer(renderer, e.getEntityModels()));
      }
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
      e.register(OPEN);
      e.register(CAST);
      e.register(AWAKENED);
      e.register(ASCENDED);
      e.register(TRANSFORM);
      e.register(SUMMON);
      e.register(TOGGLE);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
      e.registerEntityRenderer(ShadowSlaveMod.CREATURE.get(), NightmareRenderer::new);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
      e.registerLayerDefinition(NightmareModel.LAYER, NightmareModel::createLayer);
      e.registerLayerDefinition(BeastModel.LAYER, BeastModel::createLayer);
      MemoryArmorLayer.register(e);
    }
  }

  @SubscribeEvent
  public static void arm(RenderArmEvent e) {
    if (com.shadowslave.combat.BeastForms.active(e.getPlayer()) >= 0) BeastRendering.arm(e);
    else MemoryArmorLayer.renderArm(e);
  }

  @SubscribeEvent
  public static void beast(RenderPlayerEvent.Pre e) {
    BeastRendering.render(e);
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    if (mc.player == null) {
      SpellNetwork.clientState = new net.minecraft.nbt.CompoundTag();
      SpellNetwork.appearances.clear();
      SpellNetwork.forms.clear();
      SpellNetwork.beastAttacks.clear();
      SpellNetwork.visuals.clear();
      return;
    }
    while (OPEN.consumeClick()) {
      SpellNetwork.send("refresh", "");
      mc.setScreen(new SoulSeaScreen());
    }
    if (mc.screen != null) return;
    while (ASCENDED.consumeClick()) SpellNetwork.send("ascended", "");
    while (TRANSFORM.consumeClick()) SpellNetwork.send("transform", "");
    while (AWAKENED.consumeClick()) SpellNetwork.send("awakened", "");
    while (CAST.consumeClick()) SpellNetwork.send("cast", "");
    while (TOGGLE.consumeClick()) SpellNetwork.send("toggle", "");
    while (SUMMON.consumeClick()) {
      for (var v : SpellNetwork.clientState.getList("equipped", Tag.TAG_STRING)) {
        String id = v.getAsString();
        var memory = Content.memories.get(id);
        if (memory != null && memory.type().equals("weapon")) {
          SpellNetwork.send("manifest", id);
          break;
        }
      }
    }
  }

  @SubscribeEvent
  public static void hud(RenderGuiEvent.Post e) {
    var mc = Minecraft.getInstance();
    if (mc.player == null || mc.options.hideGui || mc.screen instanceof SoulSeaScreen) return;
    var d = SpellNetwork.clientState;
    if (d.isEmpty()) return;
    var g = e.getGuiGraphics();
    String stage = d.getString("stage");
    if (com.shadowslave.world.Sanctuary.isSafe(mc.level, mc.player.blockPosition())) {
      String refuge = "Sanctuary · Sheltered";
      int right = mc.getWindow().getGuiScaledWidth() - 10;
      g.fill(right - mc.font.width(refuge) - 8, 7, right + 4, 26, 0xC5191B1D);
      g.drawString(mc.font, refuge, right - mc.font.width(refuge), 13, 0xE2C18D, false);
    }
    if (stage.equals("aspirant") || stage.equals("preparing") || stage.equals("retry")) {
      String text =
          stage.equals("aspirant")
              ? "First Nightmare · Day "
                  + Math.min(d.getLong("countdownTicks") / 24000, 1 + d.getLong("elapsed") / 24000)
                  + " / "
                  + d.getLong("countdownTicks") / 24000
              : stage.equals("retry")
                  ? "Nightmare failed · O to retry"
                  : "The Spell is forming your trial...";
      g.fill(7, 7, 15 + mc.font.width(text), 25, 0xD517191B);
      g.drawString(mc.font, text, 11, 12, 0xD7CCBA, false);
      return;
    }
    if (stage.equals("nightmare")) {
      int score = d.getInt("points"), rank = Rules.aspectRank(score, d.getBoolean("divine"));
      g.fill(7, 7, 214, 58, 0xD517191B);
      g.drawString(
          mc.font,
          d.getInt("targetRank") > 1
              ? "Ascension: " + (d.getInt("targetRank") == 3 ? "Master" : "Saint")
              : "Appraisal: " + Rules.RANKS[rank] + " · " + score,
          12,
          12,
          rank == 6 ? 0xE8C56D : 0xD8C6A8,
          false);
      bar(
          g,
          12,
          25,
          195,
          5,
          rank == 6 ? 1 : Math.min(1, score / (double) Rules.nextThreshold(score)),
          rank == 6 ? 0xFFE8C56D : 0xFFBBA072);
      g.drawString(
          mc.font,
          "Seals "
              + d.getInt("altars")
              + "/"
              + d.getInt("sealCount")
              + "   Kills "
              + d.getInt("kills"),
          12,
          35,
          0xBEC4D3,
          false);
      g.drawString(
          mc.font,
          d.getInt("targetRank") > 1
              ? (d.getBoolean("divine")
                  ? "Ruler defeated · complete conduits"
                  : "Defeat the ruler and restore seals")
              : (d.getBoolean("divine")
                  ? "Divine secured · finish the trial"
                  : "Divine requires the hidden boss"),
          12,
          46,
          0x999891,
          false);
      return;
    }
    int x=12,y=12,accent=SpellTheme.accent(d.getInt("family"));
    SpellTheme.panel(g,x-5,y-5,202,94,accent);
    g.drawString(mc.font,"SOUL ESSENCE",x,y,0xAFBFCD,false);
    String essence=d.getInt("essence")+" / "+d.getInt("maxEssence");
    g.drawString(mc.font,essence,x+188-mc.font.width(essence),y,SpellTheme.TEXT,false);
    bar(g,x,y+16,188,6,d.getInt("essence")/(double)Math.max(1,d.getInt("maxEssence")),accent);
    int cores=Math.clamp(d.getInt("cores"),1,7);
    for(int i=0;i<cores;i++)SpellTheme.core(g,x+i*13,y+29,9,i<cores-1?1:d.getDouble("coreSaturation"),accent);
    String growth=(int)(Math.clamp(d.getDouble("coreSaturation"),0,1)*100)+"% · ×"+String.format(java.util.Locale.ROOT,"%.1f",d.getDouble("abilityMultiplier"));
    g.drawString(mc.font,growth,x+188-mc.font.width(growth),y+30,0xB8C8D0,false);
    int form=d.getInt("beastForm");
    String[] keys={"R","G","H","J"};
    String[] timers={form>=0?"beastCooldownRemaining":"cooldownRemaining","awakenedCooldownRemaining","ascendedCooldownRemaining","transformCooldownRemaining"};
    for(int i=0;i<4;i++){
      int xx=x+i*49;boolean unlocked=d.getInt("soulRank")>=i+1;long remaining=d.getLong(timers[i]);
      g.fill(xx,y+47,xx+42,y+68,unlocked?0xFF27374B:0xFF19222F);
      g.drawString(mc.font,keys[i],xx+4,y+52,unlocked?accent:0x697586,false);
      String state=unlocked?(remaining>0?(remaining+19)/20+"s":"◆"):"-";
      g.drawString(mc.font,state,xx+38-mc.font.width(state),y+52,unlocked?0xDCE4E5:0x697586,false);
    }
    if(form>=0 && form<com.shadowslave.combat.BeastForms.ALL.size())g.drawString(mc.font,com.shadowslave.combat.BeastForms.ALL.get(form).name()+" · "+(d.getLong("formRemaining")+19)/20+"s",x,y+75,SpellTheme.GOLD,false);
    else g.drawString(mc.font,"[O] Soul Sea · [B] Memories",x,y+75,0x8FA3B5,false);
    var memories=d.getList("equipped",Tag.TAG_STRING);
    int right=mc.getWindow().getGuiScaledWidth()-12;
    for(int i=0;i<7;i++){
      int sx=right-7*20+i*20,sy=36;boolean used=i<memories.size();
      g.fill(sx,sy,sx+18,sy+18,used&&d.getBoolean("active")?accent:SpellTheme.EDGE);
      g.fill(sx+1,sy+1,sx+17,sy+17,SpellTheme.INK);
      if(used&&Content.memories.containsKey(memories.getString(i)))g.renderItem(com.shadowslave.memory.MemoryTokenItem.create(memories.getString(i)),sx+1,sy+1);
    }
  }

  static void bar(GuiGraphics g, int x, int y, int w, int h, double amount, int color) {
    g.fill(x-1, y-1, x+w+1, y+h+1, SpellTheme.EDGE);
    g.fill(x, y, x + w, y + h, SpellTheme.INK);
    int end=x+(int)(w*Math.clamp(amount,0,1));
    g.fill(x,y,end,y+h,color);
    if(end>x)g.fill(x,y,end,y+1,0x66FFFFFF);
  }
}
