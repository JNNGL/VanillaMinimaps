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

import com.jnngl.vanillaminimaps.config.BlockConfig;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.WorldMapCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;

import java.util.ArrayList;
import java.util.Collections;

public class FlatWorldMinimapRenderer implements CacheableWorldMinimapRenderer {

  private final WorldMapCache<FlatWorldMinimapRenderer> cache = new WorldMapCache<>(this);

  @Override
  public void renderFully(World world, int centerX, int centerZ, byte[] data) {

    int size = 256;
    int step = (int) (size/128.0);

    for (int x = 0; x < size; x+= step) {
      for (int z = 0; z < size; z+= step) {

        int worldX = centerX + x;
        int worldZ = centerZ + z;

        int index = (int) ((127 - z/step) * 128 + (127 - x/step));

        Block block = world.getHighestBlockAt(worldX, worldZ);
        storeBlockColor(data, index, block);

      }
    }
  }

  private void storeBlockColor(byte[] data, int index, Block block) {
    LevelAccessor accessor = ((CraftBlock) block).getHandle();
    BlockState state = ((CraftBlockData) block.getBlockData()).getState();
    MapColor color = BlockConfig.instance().getResolvedOverrides().get(state);
    if (color == null) {
      color = state.getMapColor(accessor, new BlockPos(block.getX(), block.getY(), block.getZ()));
    }
    int brightnessId = (block.getLightLevel() >> 2) - 1;
    if (brightnessId < 0) {
      brightnessId = 3;
    }
    MapColor.Brightness brightness = MapColor.Brightness.byId(brightnessId);
    data[index] = color.getPackedId(brightness);
  }

  @Override
  public void updateBlock(Block block, int index, byte[] data) {
    storeBlockColor(data, index, block);
  }

  @Override
  public WorldMapCache<?> getWorldMapCache() {
    return cache;
  }
}
