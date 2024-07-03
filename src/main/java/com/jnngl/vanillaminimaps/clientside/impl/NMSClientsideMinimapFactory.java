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

package com.jnngl.vanillaminimaps.clientside.impl;

import com.jnngl.vanillaminimaps.clientside.ClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.EntityHandle;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.MinimapScreenPosition;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.renderer.MinimapLayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class NMSClientsideMinimapFactory implements ClientsideMinimapFactory {

  private static int MAP_ID_COUNTER = -100000;

  private ItemFrame createLayerFrame(World world, ItemStack item, Direction direction) {
    ItemFrame itemFrame = new ItemFrame(EntityType.ITEM_FRAME, ((CraftWorld) world).getHandle());
    itemFrame.setDirection(direction);
    itemFrame.setItem(item);
    itemFrame.setInvisible(true);

    return itemFrame;
  }

  @Override
  public MinimapLayer createMinimapLayer(World world, MinimapLayerRenderer renderer) {
    int mapId = MAP_ID_COUNTER--;

    ItemStack item = new ItemStack(Items.FILLED_MAP);
    Map<String, Integer> customDataMapId = new LinkedHashMap<>();
    customDataMapId.put("map", mapId);
    item.getItem().components().getOrDefault(DataComponents.CUSTOM_DATA, customDataMapId);
//    item.getItem().components().getOrDefault(DataComponents.MAP_ID, customDataMapId);

    ItemFrame upperFrame = createLayerFrame(world, item, Direction.DOWN);
    ItemFrame lowerFrame = createLayerFrame(world, item, Direction.UP);

    return new MinimapLayer(new MapId(mapId), new EntityHandle<>(upperFrame), new EntityHandle<>(lowerFrame), renderer);
  }

  @Override
  public Minimap createMinimap(Player holder, MinimapScreenPosition position, MinimapLayer primaryLayer,
                               Map<String, SecondaryMinimapLayer> secondaryLayers) {
    Minimap minimap = new Minimap(holder, position, primaryLayer, new LinkedHashMap<>());
    if (secondaryLayers != null) {
      minimap.secondaryLayers().putAll(secondaryLayers);
    }
    return minimap;
  }
}
