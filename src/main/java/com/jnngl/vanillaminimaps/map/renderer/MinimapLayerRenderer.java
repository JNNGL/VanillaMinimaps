package com.jnngl.vanillaminimaps.map.renderer;

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;

public interface MinimapLayerRenderer {

  void render(Minimap minimap, MinimapLayer layer, byte[] data);
}
