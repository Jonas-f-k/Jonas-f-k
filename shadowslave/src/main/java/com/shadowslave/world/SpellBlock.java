package com.shadowslave.world;

import com.shadowslave.nightmare.Nightmares;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public class SpellBlock extends Block {
  public enum Kind {
    ALTAR,
    EXIT,
    GATEWAY
  }

  private final Kind kind;

  public SpellBlock(Kind kind, Properties properties) {
    super(properties.noOcclusion());
    this.kind = kind;
  }

  private static final VoxelShape ALTAR_SHAPE =
      Shapes.or(
          Block.box(0, 0, 0, 16, 4, 16),
          Block.box(3, 4, 3, 13, 11, 13),
          Block.box(1, 11, 1, 15, 14, 15));
  private static final VoxelShape GATE_SHAPE =
      Shapes.or(
          Block.box(0, 0, 0, 16, 3, 16),
          Block.box(3, 3, 3, 13, 13, 13),
          Block.box(2, 13, 2, 14, 16, 14));

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return kind == Kind.ALTAR ? ALTAR_SHAPE : GATE_SHAPE;
  }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    if (random.nextInt(4) == 0)
      level.addParticle(
          kind == Kind.GATEWAY ? ParticleTypes.WAX_ON : ParticleTypes.ASH,
          pos.getX() + .5 + (random.nextDouble() - .5) * .8,
          pos.getY() + 1.05,
          pos.getZ() + .5 + (random.nextDouble() - .5) * .8,
          0,
          .012,
          0);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (player instanceof ServerPlayer p)
      switch (kind) {
        case ALTAR -> Nightmares.altar(p, pos);
        case EXIT -> Nightmares.finish(p, pos);
        case GATEWAY -> DreamRealm.gateway(p, pos);
      }
    return InteractionResult.sidedSuccess(level.isClientSide);
  }
}
