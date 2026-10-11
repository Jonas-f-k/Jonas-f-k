package com.shadowslave.memory;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.content.Content;
import com.shadowslave.core.Rules;
import com.shadowslave.network.SpellNetwork;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class MemoryTokenItem extends Item {
  public MemoryTokenItem(Properties properties) {
    super(properties);
  }

  public static String memoryId(ItemStack stack) {
    return stack
        .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        .copyTag()
        .getString("memory");
  }

  public static int rank(ItemStack s) {
    var n = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    var m = Content.memories.get(memoryId(s));
    return n.contains("rank") ? Math.clamp(n.getInt("rank"), 0, 6) : m == null ? 0 : m.rank();
  }

  public static int tier(ItemStack s) {
    return Math.clamp(
        s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("tier"),
        1,
        7);
  }

  public static ItemStack create(String id) {
    return create(id, Content.memories.containsKey(id) ? Content.memories.get(id).rank() : 0, 1);
  }

  public static ItemStack create(String id, int rank, int tier) {
    var stack = new ItemStack(ShadowSlaveMod.MEMORY.get());
    var tag = new CompoundTag();
    tag.putString("memory", id);
    tag.putInt("rank", Math.clamp(rank, 0, 6));
    tag.putInt("tier", Math.clamp(tier, 1, 7));
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    if (Content.memories.containsKey(id))
      stack.set(DataComponents.CUSTOM_NAME, Component.literal(Content.memories.get(id).name()));
    return stack;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    var stack = player.getItemInHand(hand);
    if (player instanceof ServerPlayer p) {
      var d = ShadowSlaveMod.soul(p);
      String id = memoryId(stack);
      if (!d.initiated() || d.stage.equals("nightmare"))
        p.displayClientMessage(
            Component.literal("Complete your First Nightmare before binding Memories."), true);
      else if (!Content.memories.containsKey(id))
        p.displayClientMessage(
            Component.literal("This Memory is not defined in the Spell catalogue."), true);
      else if (d.owned.contains(id))
        p.displayClientMessage(
            Component.literal(
                "You already possess this Memory. You can give this token to another player."),
            true);
      else {
        d.owned.add(id);
        d.memoryRanks.put(id, rank(stack));
        d.memoryTiers.put(id, tier(stack));
        stack.shrink(1);
        SpellNetwork.sync(p);
        p.displayClientMessage(
            Component.literal("Memory bound: " + Content.memories.get(id).name()), false);
      }
    }
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    var def = Content.memories.get(memoryId(stack));
    if (def != null) {
      lines.add(
          Component.literal(
              Rules.RANKS[rank(stack)] + " · Tier " + tier(stack) + " · " + def.type()));
      lines.add(Component.literal(def.description()));
    }
    lines.add(Component.literal("Right-click to bind to your Soul Sea."));
  }
}
