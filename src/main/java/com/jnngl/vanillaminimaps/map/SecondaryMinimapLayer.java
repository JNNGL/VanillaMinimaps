package com.jnngl.vanillaminimaps.map;

import com.jnngl.vanillaminimaps.map.renderer.SecondaryMinimapLayerRenderer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SecondaryMinimapLayer {

  private final MinimapLayer baseLayer;
  private final SecondaryMinimapLayerRenderer renderer;
  private boolean trackLocation;
  private boolean keepOnEdge;
  private int positionX;
  private int positionZ;
  private float depth;

}
