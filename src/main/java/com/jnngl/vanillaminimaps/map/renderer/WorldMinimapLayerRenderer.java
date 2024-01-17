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

package com.jnngl.vanillaminimaps.map.renderer;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.entity.Player;

public class WorldMinimapLayerRenderer implements MinimapLayerRenderer {

  @Override
  public void render(Minimap minimap, MinimapLayer layer, byte[] data) {
    Player player = minimap.holder();
    Level world = ((CraftWorld) player.getWorld()).getHandle();

    // From Minecraft code
    int width = 64;
    int height = 64;
    int playerX = Mth.floor(player.getX() - (double) width) + 64;
    int playerZ = Mth.floor(player.getZ() - (double) height) + 64;

    BlockPos.MutableBlockPos blockPos1 = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos blockPos2 = new BlockPos.MutableBlockPos();

    for (int x = 0; x < 128; ++x) {
      double d0 = 0.0;

      for (int z = 0; z < 128; ++z) {
        int worldX = (x - 64) + playerX;
        int worldZ = (z - 64) + playerZ;
        Multiset<MapColor> multiset = LinkedHashMultiset.create();
        LevelChunk chunk = world.getChunkIfLoaded(SectionPos.blockToSectionCoord(worldX), SectionPos.blockToSectionCoord(worldZ));
        if (chunk != null && !chunk.isEmpty()) {
          int l2 = 0;
          double d1 = 0.0;
          int i3;
          if (world.dimensionType().hasCeiling()) {
            i3 = worldX + worldZ * 231871;
            i3 = i3 * i3 * 31287121 + i3 * 11;
            if ((i3 >> 20 & 1) == 0) {
              multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(world, BlockPos.ZERO), 10);
            } else {
              multiset.add(Blocks.STONE.defaultBlockState().getMapColor(world, BlockPos.ZERO), 100);
            }

            d1 = 100.0;
          } else {
            blockPos1.set(worldX, 0, worldZ);
            int k3 = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, blockPos1.getX(), blockPos1.getZ()) + 1;
            BlockState blockState;
            if (k3 <= world.getMinBuildHeight() + 1) {
              blockState = Blocks.BEDROCK.defaultBlockState();
            } else {
              do {
                --k3;
                blockPos1.setY(k3);
                blockState = chunk.getBlockState(blockPos1);
              } while (blockState.getMapColor(world, blockPos1) == MapColor.NONE && k3 > world.getMinBuildHeight());

              if (k3 > world.getMinBuildHeight() && !blockState.getFluidState().isEmpty()) {
                int l3 = k3 - 1;
                blockPos2.set(blockPos1);

                BlockState iblockdata1;
                do {
                  blockPos2.setY(l3--);
                  iblockdata1 = chunk.getBlockState(blockPos2);
                  ++l2;
                } while (l3 > world.getMinBuildHeight() && !iblockdata1.getFluidState().isEmpty());

                blockState = this.getCorrectStateForFluidBlock(world, blockState, blockPos1);
              }
            }

            d1 += k3;
            multiset.add(blockState.getMapColor(world, blockPos1));
          }

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

          d0 = d1;
          int index = (127 - z) * 128 + (127 - x);
          data[index] = mapColor.getPackedId(brightness);
        }
      }
    }
  }

  private BlockState getCorrectStateForFluidBlock(Level world, BlockState state, BlockPos pos) {
    FluidState fluid = state.getFluidState();
    return !fluid.isEmpty() && !state.isFaceSturdy(world, pos, Direction.UP) ? fluid.createLegacyBlock() : state;
  }
}
