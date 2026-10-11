package com.shadowslave.world;

import com.shadowslave.ShadowSlaveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Chunk-clipped catenaries: links never write into an unavailable generation neighbor. */
public final class IslandChainsFeature extends Feature<NoneFeatureConfiguration> {
  public IslandChainsFeature() {
    super(NoneFeatureConfiguration.CODEC);
  }

  @Override
  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
    var l = c.level();
    int x0 = (c.origin().getX() >> 4) << 4, z0 = (c.origin().getZ() >> 4) << 4;
    for (int cx = Math.floorDiv(x0, Archipelago.SPACING) - 1; cx <= Math.floorDiv(x0 + 15, Archipelago.SPACING) + 1; cx++)
      for (int cz = Math.floorDiv(z0, Archipelago.SPACING) - 1; cz <= Math.floorDiv(z0 + 15, Archipelago.SPACING) + 1; cz++) {
        for (var edge : Archipelago.links(Archipelago.cluster(RealmMap.layout().seed,cx,cz))) {
          var a = edge.a();
          var b = edge.b();
          // Biomes can be sampled without loading a distant chunk.
          if (!l.getLevel()
                  .getChunkSource()
                  .getGenerator()
                  .getBiomeSource()
                  .getNoiseBiome(
                      a.x() >> 2,
                      a.y() >> 2,
                      a.z() >> 2,
                      l.getLevel().getChunkSource().randomState().sampler())
                  .is(ShadowSlaveMod.id("chained_isles"))
              || !l.getLevel()
                  .getChunkSource()
                  .getGenerator()
                  .getBiomeSource()
                  .getNoiseBiome(
                      b.x() >> 2,
                      b.y() >> 2,
                      b.z() >> 2,
                      l.getLevel().getChunkSource().randomState().sampler())
                  .is(ShadowSlaveMod.id("chained_isles"))) continue;
          double dx = b.x() - a.x(), dz = b.z() - a.z(), len = Math.sqrt(dx * dx + dz * dz);
          for (int x = x0; x < x0 + 16; x++)
            for (int z = z0; z < z0 + 16; z++) {
              double t = ((x - a.x()) * dx + (z - a.z()) * dz) / (len * len);
              if (t < 0 || t > 1) continue;
              double side = ((x - a.x()) * (-dz) + (z - a.z()) * dx) / len;
              if (Math.abs(side) > 3.5) continue;
              int y = (int) Math.round(a.y() + (b.y() - a.y()) * t - 2 - Math.min(40,len*.25) * t * (1 - t));
              // Alternating horizontal and upright 8-block iron links around a narrow traversable
              // spine.
              int phase = Math.floorMod((int) Math.round(t * len), 8);
              boolean upright = ((int) (t * len) / 8 & 1) == 0;
              if (Math.abs(side) < 1.2 || Math.abs(side - 2.4) < 1.05)
                put(l, new BlockPos(x, y, z), RealmBlocks.IRON.get().defaultBlockState());
              if (upright && Math.abs(side) < .9) {
                if (phase == 0 || phase == 7)
                  for (int dy = -2; dy <= 2; dy++)
                    put(l, new BlockPos(x, y + dy, z), RealmBlocks.IRON.get().defaultBlockState());
                else {
                  put(l, new BlockPos(x, y - 2, z), RealmBlocks.IRON.get().defaultBlockState());
                  put(l, new BlockPos(x, y + 2, z), RealmBlocks.IRON.get().defaultBlockState());
                }
              }
              if (!upright && (Math.abs(side) > 2.2 || phase == 0 || phase == 7))
                put(l, new BlockPos(x, y, z), RealmBlocks.IRON.get().defaultBlockState());
            }
        }
      }
    return true;
  }

  private static void put(
      net.minecraft.world.level.WorldGenLevel l,
      BlockPos p,
      net.minecraft.world.level.block.state.BlockState s) {
    if (l.isEmptyBlock(p)) l.setBlock(p, s, 2);
  }
}
