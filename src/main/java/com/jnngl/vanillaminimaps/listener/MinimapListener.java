package com.jnngl.vanillaminimaps.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.clientside.ClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.MinimapPacketSender;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import com.jnngl.vanillaminimaps.map.renderer.MinimapIconRenderer;
import com.jnngl.vanillaminimaps.map.renderer.WorldMinimapLayerRenderer;
import com.jnngl.vanillaminimaps.map.renderer.encoder.PrimaryMapEncoder;
import com.jnngl.vanillaminimaps.map.renderer.encoder.SecondaryMapEncoder;
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

      Minimap minimap = minimapFactory.createMinimap(player, new WorldMinimapLayerRenderer());
      packetSender.spawnMinimap(minimap);

      playerMinimaps.put(player, minimap);

      MinimapLayer playerIconBaseLayer = minimapFactory.createMinimapLayer(player.getWorld(), null);
      SecondaryMinimapLayer playerIconLayer = new SecondaryMinimapLayer(playerIconBaseLayer, new MinimapIconRenderer(PLAYER_ICON), false, false, 64, 64, 0.1F);
      minimap.secondaryLayers().put("player", playerIconLayer);

      packetSender.spawnLayer(player, playerIconBaseLayer);

      updateMinimap(minimap);
    });
  }

  private void updateMinimap(Minimap minimap) {
    byte[] layer = new byte[128 * 128];
    minimap.primaryLayer().renderer().render(minimap, minimap.primaryLayer(), layer);
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
    updateMinimap(minimap);
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

    Minimap minimap = playerMinimaps.get(event.getPlayer());
    if (minimap == null) {
      return;
    }

    updateMinimap(minimap);
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
      playerMinimaps.remove(event.getPlayer());
    });
  }
}
