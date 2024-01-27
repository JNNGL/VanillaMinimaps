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

package com.jnngl.vanillaminimaps.map.fullscreen;

import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.config.Config;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.MinimapProvider;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.renderer.MinimapLayerRenderer;
import com.jnngl.vanillaminimaps.map.renderer.encoder.FullscreenMapEncoder;
import com.jnngl.vanillaminimaps.map.renderer.world.WorldMinimapRenderer;
import lombok.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class FullscreenMinimap {

  private static final byte[] LOADING_MAP = new byte[128 * 128];

  static {
    Arrays.fill(LOADING_MAP, (byte) 119);
  }

  private final Minimap baseMinimap;
  private final Player holder;
  private final List<FullscreenMinimapLayer> primaryLayer;
  private final List<FullscreenSecondaryMinimapLayer> secondaryLayers;
  private final MinimapLayer backgroundLayer;
  private final int startX;
  private final int startZ;
  private final int width;
  private final int height;
  private double transitionState;

  private static double easeOutCubic(double x) {
    return 1 - Math.pow(1 - x, 3);
  }

  public CompletableFuture<Void> fadeIn(MinimapProvider provider, Function<Double, Double> easing, int duration) {
    CompletableFuture<Void> transitionFuture = new CompletableFuture<>();
    BukkitTask task = Bukkit.getScheduler().runTaskTimer(VanillaMinimaps.getPlugin(VanillaMinimaps.class), new Runnable() {

      private int tick = 0;

      @Override
      public void run() {
        if (++tick > duration) {
          return;
        }

        double transition = (double) tick / duration;
        transition = easing.apply(transition);

        FullscreenMinimap.this.transitionState = transition;

        byte[] meta = new byte[128];
        FullscreenMapEncoder.encodeBackground(FullscreenMinimap.this, meta);
        provider.packetSender().updateLayer(holder, backgroundLayer, 0, 0, 128, 1, meta);

        primaryLayer.forEach(layer -> {
          byte[] layerMeta = new byte[128];
          FullscreenMapEncoder.encodePrimaryLayer(FullscreenMinimap.this, layer, layerMeta);
          provider.packetSender().updateLayer(holder, layer.base(), 0, 0, 128, 1, layerMeta);
        });

        if (tick == duration) {
          transitionFuture.complete(null);
        }
      }
    }, 0L, 1L);

    return transitionFuture.whenComplete((v, t) -> task.cancel());
  }

  public CompletableFuture<Void> fadeOut(MinimapProvider provider, Function<Double, Double> easing, int duration) {
    return fadeIn(provider, x -> 1.0 - easing.apply(x), duration);
  }

  public void spawn(MinimapProvider provider) {
    PriorityQueue<Map.Entry<FullscreenMinimapLayer, Integer>> chunkQueue = new PriorityQueue<>(Map.Entry.comparingByValue());

    provider.packetSender().spawnFixedLayer(holder, backgroundLayer);

    World world = holder.getWorld();
    Location holderPosition = holder.getLocation();
    primaryLayer.forEach(layer -> {
      provider.packetSender().updateLayer(holder, layer.base(), 0, 0, 128, 128, LOADING_MAP);

      byte[] meta = new byte[128];
      FullscreenMapEncoder.encodePrimaryLayer(FullscreenMinimap.this, layer, meta);
      provider.packetSender().updateLayer(holder, layer.base(), 0, 0, 128, 1, meta);

      provider.packetSender().spawnLayer(holder, layer.base());
      Location segmentPosition = new Location(world, (layer.chunkX() << 7) + 64, holderPosition.getY(), (layer.chunkZ() << 7) + 64);
      chunkQueue.offer(new AbstractMap.SimpleImmutableEntry<>(layer, (int) segmentPosition.distanceSquared(holderPosition)));
    });

    fadeIn(provider, FullscreenMinimap::easeOutCubic, 20);

    ExecutorService executor = Executors.newFixedThreadPool(4);

    Map.Entry<FullscreenMinimapLayer, Integer> entry;
    while ((entry = chunkQueue.poll()) != null) {
      FullscreenMinimapLayer layer = entry.getKey();
      executor.execute(() -> {
        byte[] data = new byte[128 * 128];
        MinimapLayerRenderer renderer = layer.base().renderer();
        if (renderer instanceof WorldMinimapRenderer worldRenderer) {
          worldRenderer.renderFully(world, layer.chunkX() << 7, layer.chunkZ() << 7, data);
        } else {
          renderer.render(baseMinimap, layer.base(), data);
        }

        byte[] buffer = new byte[128 * 128];
        for (int z = 0; z < 128; z++) {
          for (int x = 0; x < 128; x++) {
            buffer[x * 128 + z] = data[(127 - z) * 128 + x];
          }
        }

        secondaryLayers.forEach(secondary -> {
          SecondaryMinimapLayer baseLayer = secondary.base();
          if (baseLayer.getRenderer() != null) {
            baseLayer.getRenderer().renderFullscreen(FullscreenMinimap.this, secondary, layer.chunkX(), layer.chunkZ(), buffer);
          }
        });

        FullscreenMapEncoder.encodePrimaryLayer(FullscreenMinimap.this, layer, buffer);
        provider.packetSender().updateLayer(holder, layer.base(), 0, 0, 128, 128, buffer);
      });
    }

    executor.shutdown();
  }

  public void despawn(MinimapProvider provider, Consumer<Void> callback) {
    fadeOut(provider, FullscreenMinimap::easeOutCubic, 10).whenComplete((v, t) -> {
      provider.packetSender().despawnLayer(holder, backgroundLayer);
      primaryLayer.forEach(layer -> provider.packetSender().despawnLayer(holder, layer.base()));
      if (callback != null) {
        callback.accept(null);
      }
    });
  }

  public static FullscreenMinimap create(MinimapProvider provider, Minimap minimap, int segmentsX, int segmentsZ) {
    List<FullscreenMinimapLayer> layers = new ArrayList<>();
    List<FullscreenSecondaryMinimapLayer> secondary = new ArrayList<>();

    World world = minimap.holder().getWorld();
    MinimapLayerRenderer primaryRenderer = minimap.primaryLayer().renderer();

    int halfSegmentsX = segmentsX / 2;
    int halfSegmentsZ = segmentsZ / 2;
    int startX = (int) minimap.holder().getX() >> 7;
    int startZ = (int) minimap.holder().getZ() >> 7;
    for (int x = startX - halfSegmentsX; x <= startX + halfSegmentsX; x++) {
      for (int z = startZ - halfSegmentsZ; z <= startZ + halfSegmentsZ; z++) {
        MinimapLayer baseLayer = provider.clientsideMinimapFactory().createMinimapLayer(world, primaryRenderer);
        layers.add(new FullscreenMinimapLayer(baseLayer, x, z, x - startX + halfSegmentsX, z - startZ + halfSegmentsZ));
      }
    }

    minimap.secondaryLayers().forEach((key, value) -> {
      int worldX = value.getPositionX();
      int worldZ = value.getPositionZ();
      if (!value.isTrackLocation()) {
        worldX += (int) (minimap.holder().getX() - 64);
        worldZ += (int) (minimap.holder().getZ() - 64);
      }
      worldX >>= 7;
      worldZ >>= 7;
      secondary.add(new FullscreenSecondaryMinimapLayer(value, worldX, worldZ,
          worldX - startX + halfSegmentsX, worldZ - startZ + halfSegmentsZ));
    });

    MinimapLayer backgroundLayer = provider.clientsideMinimapFactory().createMinimapLayer(world, null);
    return new FullscreenMinimap(minimap, minimap.holder(), layers, secondary, backgroundLayer,
        (startX - halfSegmentsX) << 7, (startZ - halfSegmentsZ) << 7, halfSegmentsX * 2 + 1, halfSegmentsZ * 2 + 1);
  }

  public static FullscreenMinimap create(MinimapProvider provider, Minimap minimap) {
    return create(provider, minimap, Config.instance().fullscreen.segmentsX, Config.instance().fullscreen.segmentsZ);
  }
}
