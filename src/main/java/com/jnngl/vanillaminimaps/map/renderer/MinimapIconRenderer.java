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
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public record MinimapIconRenderer(MinimapIcon icon) implements SecondaryMinimapLayerRenderer {

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

    int offsetX = -icon.width() / 2;
    int offsetY = -icon.height() / 2;
    for (int y = 0; y < icon.height(); y++) {
      int globalY = y + offsetY + trackedZ;
      if (globalY < 0) {
        continue;
      }

      if (globalY >= 128) {
        break;
      }

      for (int x = 0; x < icon.width(); x++) {
        int globalX = x + offsetX + trackedX;
        if (globalX < 0) {
          continue;
        }

        if (globalX >= 128) {
          break;
        }

        data[globalX * 128 + globalY] = icon.data()[y * icon.width() + x];
      }
    }
  }
}
