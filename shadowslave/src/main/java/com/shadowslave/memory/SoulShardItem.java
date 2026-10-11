package com.shadowslave.memory;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.entity.CreatureEcology;
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

public final class SoulShardItem extends Item {
  public SoulShardItem(Properties p) {
    super(p);
  }

  public static ItemStack create(int rank, int fragments) {
    var stack = new ItemStack(ShadowSlaveMod.SOUL_SHARD.get());
    var tag = new CompoundTag();
    tag.putInt("rank", Math.clamp(rank, 0, 6));
    tag.putInt("fragments", Math.max(1, fragments));
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    return stack;
  }

  public static int fragments(ItemStack stack) {
    var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    return tag.contains("fragments") ? Math.clamp(tag.getInt("fragments"), 1, 1000) : 10;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    var stack = player.getItemInHand(hand);
    if (player instanceof ServerPlayer p) {
      var d = ShadowSlaveMod.soul(p);
      if (!d.initiated()) {
        p.displayClientMessage(
            Component.literal("Complete your First Nightmare before absorbing Soul Shards."), true);
        return InteractionResultHolder.fail(stack);
      }
      int amount = fragments(stack);
      com.shadowslave.player.SoulGrowth.gain(p, amount);
      stack.shrink(1);
      p.displayClientMessage(
          Component.literal("Soul strengthened · +" + amount + " fragments"), true);
      SpellNetwork.sync(p);
    }
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    lines.add(
        Component.literal(
            CreatureEcology.RANKS[Math.clamp(tag.getInt("rank"), 0, 6)]
                + " · "
                + fragments(stack)
                + " Soul Fragments"));
    lines.add(Component.literal("Right-click to absorb."));
  }
}
