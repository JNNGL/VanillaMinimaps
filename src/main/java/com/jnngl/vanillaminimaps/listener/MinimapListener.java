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

package com.jnngl.vanillaminimaps.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.clientside.ClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.MinimapPacketSender;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import com.jnngl.vanillaminimaps.map.renderer.MinimapLayerRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.WorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.MinimapIconRenderer;
import com.jnngl.vanillaminimaps.map.renderer.encoder.PrimaryMapEncoder;
import com.jnngl.vanillaminimaps.map.renderer.encoder.SecondaryMapEncoder;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.WorldMapCache;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MinimapListener implements Listener {

  @SneakyThrows
  private static MinimapIcon loadIcon(String path) {
    return MinimapIcon.fromBufferedImage(ImageIO.read(Objects.requireNonNull(MinimapListener.class.getResourceAsStream(path))));
  }

  private static final MinimapIcon PLAYER_ICON = loadIcon("/minimap/player.png");
  private static final MinimapIcon DEATH_ICON = loadIcon("/minimap/death.png");

  @Getter
  private final Map<Player, Minimap> playerMinimaps = new HashMap<>();
  private final Map<Player, IntIntImmutablePair> playerSections = new HashMap<>();
  private final VanillaMinimaps plugin;

  public MinimapListener(VanillaMinimaps plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      Player player = event.getPlayer();
      ClientsideMinimapFactory minimapFactory = plugin.getDefaultClientsideMinimapFactory();
      MinimapPacketSender packetSender = plugin.getDefaultMinimapPacketSender();

      WorldMinimapRenderer worldRenderer = plugin.getDefaultWorldRenderer();
      Minimap minimap = minimapFactory.createMinimap(player, worldRenderer);
      packetSender.spawnMinimap(minimap);

      playerMinimaps.put(player, minimap);

      MinimapLayer playerIconBaseLayer = minimapFactory.createMinimapLayer(player.getWorld(), null);
      SecondaryMinimapLayer playerIconLayer = new SecondaryMinimapLayer(playerIconBaseLayer, new MinimapIconRenderer(PLAYER_ICON), false, false, 64, 64, 0.1F);
      minimap.secondaryLayers().put("player", playerIconLayer);

      packetSender.spawnLayer(player, playerIconBaseLayer);
      
      if (worldRenderer instanceof CacheableWorldMinimapRenderer cacheable) {
        cacheable.getWorldMapCache().setCallback(player.getUniqueId(), area -> updateMinimap(minimap, player.getX(), player.getZ(), false));
      }

      updateMinimap(minimap, player.getX(), player.getZ(), true);
    });
  }

  private void updateMinimap(Minimap minimap, double playerX, double playerZ, boolean updateViewerKeys) {
    byte[] layer = new byte[128 * 128];
    MinimapLayerRenderer primaryRenderer = minimap.primaryLayer().renderer();
    if (primaryRenderer instanceof CacheableWorldMinimapRenderer cacheableRenderer) {
      int blockX = (int) Math.floor(playerX);
      int blockZ = (int) Math.floor(playerZ);
      int alignedTrackX = (blockX >> 7) << 7;
      int alignedTrackZ = (blockZ >> 7) << 7;
      Location location = minimap.holder().getLocation();
      int alignedX = (location.getBlockX() >> 7) << 7;
      int alignedZ = (location.getBlockZ() >> 7) << 7;
      int offsetX = location.getBlockX() % 128;
      int offsetZ = location.getBlockZ() % 128;
      if (offsetX < 0) {
        offsetX += 128;
      }
      if (offsetZ < 0) {
        offsetZ += 128;
      }
      byte[] data = cacheableRenderer.getWorldMapCache().get(minimap.holder().getWorld(), alignedX, alignedZ);
      byte[] dataRight = cacheableRenderer.getWorldMapCache().get(minimap.holder().getWorld(), alignedX + 128, alignedZ);
      byte[] dataUpRight = cacheableRenderer.getWorldMapCache().get(minimap.holder().getWorld(), alignedX + 128, alignedZ + 128);
      byte[] dataUp = cacheableRenderer.getWorldMapCache().get(minimap.holder().getWorld(), alignedX, alignedZ + 128);
      LongList usedChunks = LongList.of(
          WorldMapCache.getKey(minimap.holder().getWorld(), alignedTrackX, alignedTrackZ),
          WorldMapCache.getKey(minimap.holder().getWorld(), alignedTrackX + 128, alignedTrackZ),
          WorldMapCache.getKey(minimap.holder().getWorld(), alignedTrackX + 128, alignedTrackZ + 128),
          WorldMapCache.getKey(minimap.holder().getWorld(), alignedTrackX, alignedTrackZ + 128)
      );
      for (int z = 0; z < 128; z++) {
        for (int x = 0; x < 128; x++) {
          int dataX = x - offsetX;
          int dataZ = z - offsetZ;
          byte[] buffer = data;

          if (dataX < 0 && dataZ < 0) {
            dataX += 128;
            dataZ += 128;
            buffer = dataUpRight;
          } else if (dataX < 0) {
            dataX += 128;
            buffer = dataRight;
          } else if (dataZ < 0) {
            dataZ += 128;
            buffer = dataUp;
          }

          if (dataX > 0 && dataZ > 0) {
            layer[(127 - dataZ) * 128 + (127 - dataX)] = buffer[(127 - z) * 128 + (127 - x)];
          }
        }
      }

      if (updateViewerKeys) {
        cacheableRenderer.getWorldMapCache().setViewerChunks(minimap.holder().getUniqueId(), usedChunks);
      }
    } else {
      primaryRenderer.render(minimap, minimap.primaryLayer(), layer);
    }
    PrimaryMapEncoder.encodePrimaryLayer(minimap, layer);
    plugin.getDefaultMinimapPacketSender().updateLayer(minimap.holder(), minimap.primaryLayer(), 0, 0, 128, 128, layer);

    for (SecondaryMinimapLayer secondary : minimap.secondaryLayers().values()) {
      byte[] secondaryLayer = new byte[128 * 128];
      if (secondary.getRenderer() != null) {
        secondary.getRenderer().render(minimap, secondary, secondaryLayer);
      } else if (secondary.getBaseLayer().renderer() != null) {
        secondary.getBaseLayer().renderer().render(minimap, secondary.getBaseLayer(), secondaryLayer);
      }
      SecondaryMapEncoder.encodeSecondaryLayer(minimap, secondary, secondaryLayer);
      plugin.getDefaultMinimapPacketSender().updateLayer(minimap.holder(), secondary.getBaseLayer(), 0, 0, 128, 128, secondaryLayer);
    }
  }

  private void respawnMinimap(Minimap minimap) {
    MinimapPacketSender packetSender = plugin.getDefaultMinimapPacketSender();
    packetSender.despawnMinimap(minimap);
    packetSender.spawnMinimap(minimap);
    updateMinimap(minimap, minimap.holder().getX(), minimap.holder().getZ(), true);
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent event) {
    Minimap minimap = playerMinimaps.get(event.getPlayer());
    if (minimap == null) {
      return;
    }

    Player player = event.getPlayer();
    ClientsideMinimapFactory minimapFactory = plugin.getDefaultClientsideMinimapFactory();
    MinimapPacketSender packetSender = plugin.getDefaultMinimapPacketSender();

    SecondaryMinimapLayer currentDeathPoint = minimap.secondaryLayers().get("death_point");
    if (currentDeathPoint != null) {
      packetSender.despawnLayer(player, currentDeathPoint.getBaseLayer());
    }

    Location deathLocation = event.getPlayer().getLocation();
    MinimapLayer iconBaseLayer = minimapFactory.createMinimapLayer(player.getWorld(), null);
    SecondaryMinimapLayer iconLayer = new SecondaryMinimapLayer(iconBaseLayer, new MinimapIconRenderer(DEATH_ICON), true, true, deathLocation.getBlockX(), deathLocation.getBlockZ(), 0.05F);
    minimap.secondaryLayers().put("death_point", iconLayer);

    packetSender.spawnLayer(player, iconBaseLayer);
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    if (!event.hasChangedPosition()) {
      return;
    }

    Location diff = event.getTo().clone().subtract(event.getFrom());
    diff.setY(0.0);

    if (diff.lengthSquared() == 0.0) {
      return;
    }

    Minimap minimap = playerMinimaps.get(event.getPlayer());
    if (minimap == null) {
      return;
    }

    IntIntImmutablePair previous = playerSections.get(event.getPlayer());
    int currentX = event.getTo().getBlockX() >> 7;
    int currentZ = event.getTo().getBlockZ() >> 7;
    boolean changedSection = previous == null || currentZ != previous.rightInt() || currentX != previous.leftInt();
    updateMinimap(minimap, event.getTo().getX(), event.getTo().getZ(), changedSection);
    if (changedSection) {
      playerSections.put(event.getPlayer(), IntIntImmutablePair.of(currentX, currentZ));
    }
  }

  @EventHandler
  public void onRespawn(PlayerPostRespawnEvent event) {
    Minimap minimap = playerMinimaps.get(event.getPlayer());
    if (minimap == null) {
      return;
    }

    respawnMinimap(minimap);
  }

  @EventHandler
  public void onWorldChange(PlayerChangedWorldEvent event) {
    Minimap minimap = playerMinimaps.get(event.getPlayer());
    if (minimap == null) {
      return;
    }

    respawnMinimap(minimap);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      playerSections.remove(event.getPlayer());
      Minimap minimap = playerMinimaps.remove(event.getPlayer());
      MinimapLayerRenderer primaryRenderer = minimap.primaryLayer().renderer();
      if (primaryRenderer instanceof CacheableWorldMinimapRenderer cacheableRenderer) {
        cacheableRenderer.getWorldMapCache().releaseViewer(event.getPlayer().getUniqueId());
      }
    });
  }
}
