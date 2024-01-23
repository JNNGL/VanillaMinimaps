package com.jnngl.vanillaminimaps.map.fullscreen;

import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.MinimapProvider;
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
  private final MinimapLayer backgroundLayer;
  private final int width;
  private final int height;
  private double transitionState;

  private static double easeOutCubic(double x) {
    return 1 - Math.pow(1 - x, 3);
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

    CompletableFuture<Void> transitionFuture = new CompletableFuture<>();
    BukkitTask task = Bukkit.getScheduler().runTaskTimer(VanillaMinimaps.getPlugin(VanillaMinimaps.class), new Runnable() {

      private int tick = 0;

      @Override
      public void run() {
        ++tick;
        if (tick > 20) {
          return;
        }

        double transition = (double) tick / 20;
        transition = easeOutCubic(transition);

        FullscreenMinimap.this.transitionState = transition;

        byte[] meta = new byte[128];
        FullscreenMapEncoder.encodeBackground(FullscreenMinimap.this, meta);
        provider.packetSender().updateLayer(holder, backgroundLayer, 0, 0, 128, 1, meta);

        primaryLayer.forEach(layer -> {
          byte[] layerMeta = new byte[128];
          FullscreenMapEncoder.encodePrimaryLayer(FullscreenMinimap.this, layer, layerMeta);
          provider.packetSender().updateLayer(holder, layer.base(), 0, 0, 128, 1, layerMeta);
        });

        if (tick == 20) {
          transitionFuture.complete(null);
        }
      }
    }, 0L, 1L);

    transitionFuture.whenComplete((v, t) -> task.cancel());

    ExecutorService executor = Executors.newFixedThreadPool(4);

    Map.Entry<FullscreenMinimapLayer, Integer> entry;
    while ((entry = chunkQueue.poll()) != null) {
      FullscreenMinimapLayer layer = entry.getKey();
      executor.execute(() -> {
        byte[] buffer = new byte[128 * 128];
        MinimapLayerRenderer renderer = layer.base().renderer();
        if (renderer instanceof WorldMinimapRenderer worldRenderer) {
          worldRenderer.renderFully(world, layer.chunkX() << 7, layer.chunkZ() << 7, buffer);
        } else {
          renderer.render(baseMinimap, layer.base(), buffer);
        }

        FullscreenMapEncoder.encodePrimaryLayer(FullscreenMinimap.this, layer, buffer);
        provider.packetSender().updateLayer(holder, layer.base(), 0, 0, 128, 128, buffer);
      });
    }

    executor.shutdown();
  }

  public void despawn(MinimapProvider provider, Consumer<Void> callback) {
    CompletableFuture<Void> transitionFuture = new CompletableFuture<>();
    BukkitTask task = Bukkit.getScheduler().runTaskTimer(VanillaMinimaps.getPlugin(VanillaMinimaps.class), new Runnable() {

      private int tick = 0;

      @Override
      public void run() {
        ++tick;
        if (tick > 10) {
          return;
        }

        double transition = (double) tick / 10;
        transition = 1.0 - easeOutCubic(transition);

        FullscreenMinimap.this.transitionState = transition;

        byte[] meta = new byte[128];
        FullscreenMapEncoder.encodeBackground(FullscreenMinimap.this, meta);
        provider.packetSender().updateLayer(holder, backgroundLayer, 0, 0, 128, 1, meta);

        primaryLayer.forEach(layer -> {
          byte[] layerMeta = new byte[128];
          FullscreenMapEncoder.encodePrimaryLayer(FullscreenMinimap.this, layer, layerMeta);
          provider.packetSender().updateLayer(holder, layer.base(), 0, 0, 128, 1, layerMeta);
        });

        if (tick == 10) {
          transitionFuture.complete(null);
        }
      }
    }, 0L, 1L);

    transitionFuture.whenComplete((v, t) -> {
      task.cancel();
      provider.packetSender().despawnLayer(holder, backgroundLayer);
      primaryLayer.forEach(layer -> provider.packetSender().despawnLayer(holder, layer.base()));
      if (callback != null) {
        callback.accept(null);
      }
    });
  }

  public static FullscreenMinimap of(MinimapProvider provider, Minimap minimap) {
    List<FullscreenMinimapLayer> layers = new ArrayList<>();

    World world = minimap.holder().getWorld();
    MinimapLayerRenderer primaryRenderer = minimap.primaryLayer().renderer();

    int startX = (int) minimap.holder().getX() >> 7;
    int startZ = (int) minimap.holder().getZ() >> 7;
    for (int x = startX - 2; x <= startX + 2; x++) {
      for (int z = startZ - 1; z <= startZ + 1; z++) {
        MinimapLayer baseLayer = provider.clientsideMinimapFactory().createMinimapLayer(world, primaryRenderer);
        layers.add(new FullscreenMinimapLayer(baseLayer, x, z, x - startX + 2, z - startZ + 1));
      }
    }

    MinimapLayer backgroundLayer = provider.clientsideMinimapFactory().createMinimapLayer(world, null);
    return new FullscreenMinimap(minimap, minimap.holder(), layers, backgroundLayer, 5, 3);
  }
}
