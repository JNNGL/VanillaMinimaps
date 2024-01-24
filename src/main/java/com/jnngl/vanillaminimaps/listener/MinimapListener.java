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
import com.jnngl.vanillaminimaps.config.Config;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import com.jnngl.vanillaminimaps.map.renderer.MinimapLayerRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.WorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.MinimapIconRenderer;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import lombok.Getter;
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

import java.util.*;

public class MinimapListener implements Listener {

  @Getter
  private final Map<Player, Minimap> playerMinimaps = new HashMap<>();
  private final Map<Player, IntIntImmutablePair> playerSections = new HashMap<>();
  private final Set<UUID> requestedUpdates = new HashSet<>();
  private final VanillaMinimaps plugin;

  public MinimapListener(VanillaMinimaps plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!Config.instance().enabledByDefault) {
      return;
    }

    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> enableMinimap(event.getPlayer()));
  }

  public void enableMinimap(Player player) {
    if (playerMinimaps.containsKey(player)) {
      return;
    }

    ClientsideMinimapFactory minimapFactory = plugin.clientsideMinimapFactory();
    MinimapPacketSender packetSender = plugin.packetSender();

    WorldMinimapRenderer worldRenderer = plugin.worldRenderer();
    Minimap minimap = minimapFactory.createMinimap(player, Config.instance().defaultPosition, worldRenderer);
    packetSender.spawnMinimap(minimap);

    playerMinimaps.put(player, minimap);

    MinimapIcon playerIcon = plugin.iconProvider().getIcon("player");
    MinimapIcon offscreenPlayerIcon = plugin.iconProvider().getIcon("offscreen_player");
    if (playerIcon != null) {
      MinimapLayer playerIconBaseLayer = minimapFactory.createMinimapLayer(player.getWorld(), null);
      MinimapIconRenderer playerIconRenderer = new MinimapIconRenderer(playerIcon, offscreenPlayerIcon);
      SecondaryMinimapLayer playerIconLayer = new SecondaryMinimapLayer(playerIconBaseLayer, playerIconRenderer, false, false, 64, 64, 0.1F);
      minimap.secondaryLayers().put("player", playerIconLayer);

      packetSender.spawnLayer(player, playerIconBaseLayer);
    }

    if (worldRenderer instanceof CacheableWorldMinimapRenderer cacheable) {
      cacheable.getWorldMapCache().setCallback(player.getUniqueId(), area -> {
        if (area.x() >= player.getX() - 64 && area.y() >= player.getZ() - 64 &&
            area.x() + area.z() <= player.getX() + 64 && area.y() + area.w() <= player.getZ() + 64 &&
            requestedUpdates.add(player.getUniqueId())) {
          Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (requestedUpdates.remove(player.getUniqueId())) {
              minimap.update(plugin, player.getX(), player.getZ(), false);
            }
          });
        }
      });
    }

    minimap.update(plugin, player.getX(), player.getZ(), true);
  }

  public void disableMinimap(Player player) {
    playerSections.remove(player);
    Minimap minimap = playerMinimaps.remove(player);
    if (minimap != null) {
      MinimapLayerRenderer primaryRenderer = minimap.primaryLayer().renderer();
      if (primaryRenderer instanceof CacheableWorldMinimapRenderer cacheableRenderer) {
        cacheableRenderer.getWorldMapCache().releaseViewer(player.getUniqueId());
      }
      plugin.packetSender().despawnMinimap(minimap);
    }
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent event) {
    if (!Config.instance().markers.deathMarker.enabled) {
      return;
    }

    Minimap minimap = playerMinimaps.get(event.getPlayer());
    if (minimap == null) {
      return;
    }

    minimap.setDeathPoint(plugin, event.getPlayer().getLocation());
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
    minimap.update(plugin, event.getTo().getX(), event.getTo().getZ(), changedSection);
    requestedUpdates.remove(event.getPlayer().getUniqueId());
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

    minimap.respawn(plugin);
  }

  @EventHandler
  public void onWorldChange(PlayerChangedWorldEvent event) {
    Minimap minimap = playerMinimaps.get(event.getPlayer());
    if (minimap == null) {
      return;
    }

    minimap.respawn(plugin);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      disableMinimap(event.getPlayer());
    });
  }
}
