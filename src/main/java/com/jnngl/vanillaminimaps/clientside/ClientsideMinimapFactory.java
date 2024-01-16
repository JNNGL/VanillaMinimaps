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
