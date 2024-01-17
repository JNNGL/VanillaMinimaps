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

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.entity.Player;

public class FlatWorldMinimapRenderer implements MinimapLayerRenderer {

  @Override
  public void render(Minimap minimap, MinimapLayer layer, byte[] data) {
    Player player = minimap.holder();
    Location location = player.getLocation();
    World world = player.getWorld();

    for (int x = -64; x < 64; x++) {
      for (int z = -64; z < 64; z++) {
        int worldX = location.getBlockX() + x;
        int worldZ = location.getBlockZ() + z;
        int index = (127 - z - 64) * 128 + (127 - x - 64);
        Block block = world.getHighestBlockAt(worldX, worldZ);
        LevelAccessor accessor = ((CraftBlock) block).getHandle();
        BlockState state = ((CraftBlockData) block.getBlockData()).getState();
        MapColor color = state.getMapColor(accessor, new BlockPos(block.getX(), block.getY(), block.getZ()));
        int brightnessId = (block.getLightLevel() >> 2) - 1;
        if (brightnessId < 0) {
          brightnessId = 3;
        }
        MapColor.Brightness brightness = MapColor.Brightness.byId(brightnessId);
        data[index] = color.getPackedId(brightness);
      }
    }
  }
}
