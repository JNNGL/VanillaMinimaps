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

package com.jnngl.vanillaminimaps.clientside;

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.renderer.MinimapLayerRenderer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;

public interface ClientsideMinimapFactory {

  MinimapLayer createMinimapLayer(World world, MinimapLayerRenderer renderer);

  Minimap createMinimap(Player holder, MinimapLayer primaryLayer, Map<String, SecondaryMinimapLayer> secondaryLayers);

  default Minimap createMinimap(Player holder, MinimapLayer primaryLayer) {
    return createMinimap(holder, primaryLayer, null);
  }

  default Minimap createMinimap(Player holder, MinimapLayerRenderer worldRenderer) {
    return createMinimap(holder, createMinimapLayer(holder.getWorld(), worldRenderer));
  }
}
