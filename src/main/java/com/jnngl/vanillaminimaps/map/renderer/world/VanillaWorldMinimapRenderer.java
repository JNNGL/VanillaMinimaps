/*
 *  Copyright (C) 2024  JNNGL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jnngl.vanillaminimaps.map.renderer.world;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.WorldMapCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;

import java.util.concurrent.atomic.AtomicInteger;

public class VanillaWorldMinimapRenderer implements CacheableWorldMinimapRenderer {

  private final WorldMapCache<VanillaWorldMinimapRenderer> cache = new WorldMapCache<>(this);

  private double fillColorSet(Level level, LevelChunk chunk, BlockPos.MutableBlockPos blockPos1,
                              BlockPos.MutableBlockPos blockPos2, int worldX, int worldZ, AtomicInteger l2,
                              Multiset<MapColor> multiset) {
    double d1 = 0.0;
    int i3;
    if (level.dimensionType().hasCeiling()) {
      i3 = worldX + worldZ * 231871;
      i3 = i3 * i3 * 31287121 + i3 * 11;
      if ((i3 >> 20 & 1) == 0) {
        multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(level, BlockPos.ZERO), 10);
      } else {
        multiset.add(Blocks.STONE.defaultBlockState().getMapColor(level, BlockPos.ZERO), 100);
      }

      d1 = 100.0;
    } else {
      blockPos1.set(worldX, 0, worldZ);
      int k3 = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, blockPos1.getX(), blockPos1.getZ()) + 1;
      BlockState blockState;
      if (k3 <= level.getMinBuildHeight() + 1) {
        blockState = Blocks.BEDROCK.defaultBlockState();
      } else {
        do {
          --k3;
          blockPos1.setY(k3);
          blockState = chunk.getBlockState(blockPos1);
        } while (blockState.getMapColor(level, blockPos1) == MapColor.NONE && k3 > level.getMinBuildHeight());

        if (k3 > level.getMinBuildHeight() && !blockState.getFluidState().isEmpty()) {
          int l3 = k3 - 1;
          blockPos2.set(blockPos1);

          BlockState iblockdata1;
          do {
            blockPos2.setY(l3--);
            iblockdata1 = chunk.getBlockState(blockPos2);
            l2.incrementAndGet();
          } while (l3 > level.getMinBuildHeight() && !iblockdata1.getFluidState().isEmpty());

          blockState = this.getCorrectStateForFluidBlock(level, blockState, blockPos1);
        }
      }

      d1 += k3;
      multiset.add(blockState.getMapColor(level, blockPos1));
    }

    return d1;
  }

  private void storeMapColor(Multiset<MapColor> multiset, double d0, double d1, int l2, int x, int z, int index, byte[] data) {
    MapColor mapColor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.NONE);
    double d2;
    MapColor.Brightness brightness;
    if (mapColor == MapColor.WATER) {
      d2 = (double) l2 * 0.1 + (double) (x + z & 1) * 0.2;
      if (d2 < 0.5) {
        brightness = MapColor.Brightness.HIGH;
      } else if (d2 > 0.9) {
        brightness = MapColor.Brightness.LOW;
      } else {
        brightness = MapColor.Brightness.NORMAL;
      }
    } else {
      d2 = (d1 - d0) * 4.0 / (double) 5 + ((double) (x + z & 1) - 0.5) * 0.4;
      if (d2 > 0.6) {
        brightness = MapColor.Brightness.HIGH;
      } else if (d2 < -0.6) {
        brightness = MapColor.Brightness.LOW;
      } else {
        brightness = MapColor.Brightness.NORMAL;
      }
    }

    data[index] = mapColor.getPackedId(brightness);
  }

  private BlockState getCorrectStateForFluidBlock(Level world, BlockState state, BlockPos pos) {
    FluidState fluid = state.getFluidState();
    return !fluid.isEmpty() && !state.isFaceSturdy(world, pos, Direction.UP) ? fluid.createLegacyBlock() : state;
  }

  @Override
  public void renderFully(World world, int blockX, int blockZ, byte[] data) {
    Level level = ((CraftWorld) world).getHandle();

    BlockPos.MutableBlockPos blockPos1 = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos blockPos2 = new BlockPos.MutableBlockPos();

    for (int x = 0; x < 128; ++x) {
      double d0 = 0.0;

      for (int z = -1; z < 128; ++z) {
        int worldX = x + blockX;
        int worldZ = z + blockZ;
        Multiset<MapColor> multiset = LinkedHashMultiset.create();
        LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(worldX), SectionPos.blockToSectionCoord(worldZ));
        if (!chunk.isEmpty()) {
          AtomicInteger l2 = new AtomicInteger();
          double d1 = fillColorSet(level, chunk, blockPos1, blockPos2, worldX, worldZ, l2, multiset);

          if (z >= 0) {
            int index = (127 - z) * 128 + (127 - x);
            storeMapColor(multiset, d0, d1, l2.get(), x, z, index, data);
          }

          d0 = d1;
        }
      }
    }
  }

  @Override
  public void updateBlock(Block block, int index, byte[] data) {
    Level world = ((CraftWorld) block.getWorld()).getHandle();

    int width = 64;
    int height = 64;
    int blockX = Mth.floor(block.getX() - (double) width) + 64;
    int blockZ = Mth.floor(block.getZ() - (double) height) + 64;

    BlockPos.MutableBlockPos blockPos1 = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos blockPos2 = new BlockPos.MutableBlockPos();

    double d0 = 0.0;

    for (int z = -1; z < 1; ++z) {
      int worldZ = z + blockZ;
      Multiset<MapColor> multiset = LinkedHashMultiset.create();
      LevelChunk chunk = world.getChunk(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(worldZ));
      if (!chunk.isEmpty()) {
        AtomicInteger l2 = new AtomicInteger();
        double d1 = fillColorSet(world, chunk, blockPos1, blockPos2, blockX, worldZ, l2, multiset);
        if (z == 0) {
          storeMapColor(multiset, d0, d1, l2.get(), blockX, worldZ, index, data);
        } else {
          d0 = d1;
        }
      }
    }
  }

  @Override
  public WorldMapCache<?> getWorldMapCache() {
    return cache;
  }
}
