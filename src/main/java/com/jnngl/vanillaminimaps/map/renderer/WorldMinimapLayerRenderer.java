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
    Location location = player.getLocation();
    Level world = ((CraftWorld) player.getWorld()).getHandle();

    int i = 1;
    int j = 64;
    int k = 64;
    int l = Mth.floor(player.getX() - (double)j) / i + 64;
    int i1 = Mth.floor(player.getZ() - (double)k) / i + 64;

    BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos blockposition_mutableblockposition1 = new BlockPos.MutableBlockPos();

    for(int k1 = 0; k1 < 128; ++k1) {
      double d0 = 0.0;

      for(int l1 = 0; l1 < 128; ++l1) {
        int j2 = (k1 - 64) + l;
        int k2 = (l1 - 64) + i1;
        Multiset<MapColor> multiset = LinkedHashMultiset.create();
        LevelChunk chunk = world.getChunkIfLoaded(SectionPos.blockToSectionCoord(j2), SectionPos.blockToSectionCoord(k2));
        if (chunk != null && !chunk.isEmpty()) {
          int l2 = 0;
          double d1 = 0.0;
          int i3;
          if (world.dimensionType().hasCeiling()) {
            i3 = j2 + k2 * 231871;
            i3 = i3 * i3 * 31287121 + i3 * 11;
            if ((i3 >> 20 & 1) == 0) {
              multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(world, BlockPos.ZERO), 10);
            } else {
              multiset.add(Blocks.STONE.defaultBlockState().getMapColor(world, BlockPos.ZERO), 100);
            }

            d1 = 100.0;
          } else {
            for(i3 = 0; i3 < i; ++i3) {
              for(int j3 = 0; j3 < i; ++j3) {
                blockposition_mutableblockposition.set(j2 + i3, 0, k2 + j3);
                int k3 = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, blockposition_mutableblockposition.getX(), blockposition_mutableblockposition.getZ()) + 1;
                BlockState iblockdata;
                if (k3 <= world.getMinBuildHeight() + 1) {
                  iblockdata = Blocks.BEDROCK.defaultBlockState();
                } else {
                  do {
                    --k3;
                    blockposition_mutableblockposition.setY(k3);
                    iblockdata = chunk.getBlockState(blockposition_mutableblockposition);
                  } while(iblockdata.getMapColor(world, blockposition_mutableblockposition) == MapColor.NONE && k3 > world.getMinBuildHeight());

                  if (k3 > world.getMinBuildHeight() && !iblockdata.getFluidState().isEmpty()) {
                    int l3 = k3 - 1;
                    blockposition_mutableblockposition1.set(blockposition_mutableblockposition);

                    BlockState iblockdata1;
                    do {
                      blockposition_mutableblockposition1.setY(l3--);
                      iblockdata1 = chunk.getBlockState(blockposition_mutableblockposition1);
                      ++l2;
                    } while(l3 > world.getMinBuildHeight() && !iblockdata1.getFluidState().isEmpty());

                    iblockdata = this.getCorrectStateForFluidBlock(world, iblockdata, blockposition_mutableblockposition);
                  }
                }

                d1 += (double)k3 / (double)(i * i);
                multiset.add(iblockdata.getMapColor(world, blockposition_mutableblockposition));
              }
            }
          }

          l2 /= i * i;
          MapColor materialmapcolor = (MapColor)Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.NONE);
          double d2;
          MapColor.Brightness materialmapcolor_a;
          if (materialmapcolor == MapColor.WATER) {
            d2 = (double)l2 * 0.1 + (double)(k1 + l1 & 1) * 0.2;
            if (d2 < 0.5) {
              materialmapcolor_a = MapColor.Brightness.HIGH;
            } else if (d2 > 0.9) {
              materialmapcolor_a = MapColor.Brightness.LOW;
            } else {
              materialmapcolor_a = MapColor.Brightness.NORMAL;
            }
          } else {
            d2 = (d1 - d0) * 4.0 / (double)(i + 4) + ((double)(k1 + l1 & 1) - 0.5) * 0.4;
            if (d2 > 0.6) {
              materialmapcolor_a = MapColor.Brightness.HIGH;
            } else if (d2 < -0.6) {
              materialmapcolor_a = MapColor.Brightness.LOW;
            } else {
              materialmapcolor_a = MapColor.Brightness.NORMAL;
            }
          }

          d0 = d1;
          int index = (127 - l1) * 128 + (127 - k1);
          data[index] = materialmapcolor.getPackedId(materialmapcolor_a);
        }
      }
    }

//    for (int x = -64; x < 64; x++) {
//      for (int z = -64; z < 64; z++) {
//        int worldX = location.getBlockX() + x;
//        int worldZ = location.getBlockZ() + z;
//        int index = (127 - z - 64) * 128 + (127 - x - 64);
//        Block block = world.getHighestBlockAt(worldX, worldZ);
//        LevelAccessor accessor = ((CraftBlock) block).getHandle();
//        BlockState state = ((CraftBlockData) block.getBlockData()).getState();
//        MapColor color = state.getMapColor(accessor, new BlockPos(block.getX(), block.getY(), block.getZ()));
//        int brightnessId = (block.getLightLevel() >> 2) - 1;
//        if (brightnessId < 0) {
//          brightnessId = 3;
//        }
//        MapColor.Brightness brightness = MapColor.Brightness.byId(brightnessId);
//        data[index] = color.getPackedId(brightness);
//      }
//    }
  }

  private BlockState getCorrectStateForFluidBlock(Level world, BlockState state, BlockPos pos) {
    FluidState fluid = state.getFluidState();
    return !fluid.isEmpty() && !state.isFaceSturdy(world, pos, Direction.UP) ? fluid.createLegacyBlock() : state;
  }
}
