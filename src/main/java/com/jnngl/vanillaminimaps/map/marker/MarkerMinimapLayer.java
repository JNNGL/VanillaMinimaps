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

package com.jnngl.vanillaminimaps.map.marker;

import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.renderer.SecondaryMinimapLayerRenderer;
import org.bukkit.World;

public class MarkerMinimapLayer extends SecondaryMinimapLayer {

  public MarkerMinimapLayer(MinimapLayer baseLayer, SecondaryMinimapLayerRenderer renderer, boolean trackLocation,
                            boolean keepOnEdge, World world, int positionX, int positionZ, float depth) {
    super(baseLayer, renderer, trackLocation, keepOnEdge, world, positionX, positionZ, depth);
  }
}
