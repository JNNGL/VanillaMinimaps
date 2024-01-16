package com.jnngl.vanillaminimaps.map.renderer;

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;

public interface SecondaryMinimapLayerRenderer {

  void render(Minimap minimap, SecondaryMinimapLayer layer, byte[] data);
}
