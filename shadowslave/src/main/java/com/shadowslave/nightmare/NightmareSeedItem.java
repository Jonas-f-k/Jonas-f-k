package com.shadowslave.nightmare;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class NightmareSeedItem extends Item {
  public NightmareSeedItem(Properties p) {
    super(p);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    var stack = player.getItemInHand(hand);
    if (player instanceof ServerPlayer p && Nightmares.ascend(p)) stack.shrink(1);
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }
}
