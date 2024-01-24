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
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.fullscreen.FullscreenMinimap;
import com.jnngl.vanillaminimaps.map.fullscreen.FullscreenSecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public record MinimapIconRenderer(MinimapIcon icon, @Nullable MinimapIcon fullscreenIcon) implements SecondaryMinimapLayerRenderer {

  public MinimapIconRenderer(MinimapIcon icon) {
    this(icon, icon);
  }

  @Override
  public void render(Minimap minimap, SecondaryMinimapLayer layer, byte[] data) {
    Location location = minimap.holder().getLocation();
    int trackedX = layer.getPositionX();
    int trackedZ = layer.getPositionZ();
    if (layer.isTrackLocation()) {
      trackedZ = location.getBlockX() - layer.getPositionX();
      trackedX = location.getBlockZ() - layer.getPositionZ();
      if (layer.isKeepOnEdge()) {
        Vector direction = new Vector(trackedX, 0, trackedZ);
        if (direction.lengthSquared() > 60 * 60) {
          direction.normalize().multiply(60);
          trackedX = direction.getBlockX();
          trackedZ = direction.getBlockZ();
        }
      }
      trackedX += 64;
      trackedZ += 64;
    }

    renderIcon(icon, data, trackedX, trackedZ, (x, y) -> x * 128 + y, (x, y) -> y * icon.width() + icon.width() - 1 - x);
  }

  @Override
  public void renderFullscreen(FullscreenMinimap minimap, FullscreenSecondaryMinimapLayer layer, int chunkX, int chunkZ, byte[] data) {
    if (fullscreenIcon == null) {
      return;
    }

    int worldX = layer.base().getPositionX();
    int worldZ = layer.base().getPositionZ();
    if (!layer.base().isTrackLocation()) {
      worldX += (int) (minimap.getHolder().getX() - 64);
      worldZ += (int) (minimap.getHolder().getZ() - 64);
    }

    int mapX = worldX - (chunkX << 7);
    int mapZ = worldZ - (chunkZ << 7);
    if (mapX < -icon.width() ||
        mapZ < -icon.height() ||
        mapX > 128 + icon.width() ||
        mapZ > 128 + icon.height()) {
      return;
    }

    renderIcon(fullscreenIcon, data, mapX, mapZ, (x, y) -> (127 - x) * 128 + y, (x, y) -> y * icon.width() + x);
  }

  private void renderIcon(MinimapIcon icon, byte[] data, int mapX, int mapZ,
                          BiFunction<Integer, Integer, Integer> indexMapper,
                          BiFunction<Integer, Integer, Integer> fetchIndexMapper) {
    int offsetX = -icon.width() / 2;
    int offsetY = -icon.height() / 2;
    for (int y = 0; y < icon.height(); y++) {
      int globalY = y + offsetY + mapZ;
      if (globalY < 0) {
        continue;
      }

      if (globalY >= 128) {
        break;
      }

      for (int x = 0; x < icon.width(); x++) {
        int globalX = x + offsetX + mapX;
        if (globalX < 0) {
          continue;
        }

        if (globalX >= 128) {
          break;
        }

        byte color = icon.data()[fetchIndexMapper.apply(x, y)];
        if (color == 0) {
          continue;
        }

        data[indexMapper.apply(globalX, globalY)] = color;
      }
    }
  }
}
