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

package com.jnngl.vanillaminimaps;

import com.jnngl.vanillaminimaps.clientside.ClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.MinimapPacketSender;
import com.jnngl.vanillaminimaps.clientside.SteerableViewFactory;
import com.jnngl.vanillaminimaps.clientside.impl.NMSClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.impl.NMSMinimapPacketSender;
import com.jnngl.vanillaminimaps.clientside.impl.NMSSteerableViewFactory;
import com.jnngl.vanillaminimaps.command.MinimapCommand;
import com.jnngl.vanillaminimaps.command.NMSCommandDispatcherAccessor;
import com.jnngl.vanillaminimaps.config.BlockConfig;
import com.jnngl.vanillaminimaps.config.Config;
import com.jnngl.vanillaminimaps.injection.PassengerRewriter;
import com.jnngl.vanillaminimaps.listener.MinimapBlockListener;
import com.jnngl.vanillaminimaps.listener.MinimapListener;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapProvider;
import com.jnngl.vanillaminimaps.map.fullscreen.FullscreenMinimap;
import com.jnngl.vanillaminimaps.map.icon.provider.BuiltinMinimapIconProvider;
import com.jnngl.vanillaminimaps.map.icon.provider.MinimapIconProvider;
import com.jnngl.vanillaminimaps.map.renderer.world.WorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.provider.BuiltinMinimapWorldRendererProvider;
import com.jnngl.vanillaminimaps.map.renderer.world.provider.MinimapWorldRendererProvider;
import com.jnngl.vanillaminimaps.storage.MinimapPlayerDatabase;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class VanillaMinimaps extends JavaPlugin implements MinimapProvider, Listener {

  private static final AtomicReference<VanillaMinimaps> PLUGIN = new AtomicReference<>();

  public static VanillaMinimaps get() {
    return PLUGIN.get();
  }

  @Getter
  private final Map<Player, PassengerRewriter> passengerRewriters = new HashMap<>();

  @MonotonicNonNull
  private ClientsideMinimapFactory defaultClientsideMinimapFactory;

  @MonotonicNonNull
  private MinimapPacketSender defaultMinimapPacketSender;

  @MonotonicNonNull
  private WorldMinimapRenderer defaultWorldRenderer;

  @MonotonicNonNull
  private MinimapIconProvider minimapIconProvider;

  @MonotonicNonNull
  private MinimapWorldRendererProvider worldRendererProvider;

  @MonotonicNonNull
  private MinimapBlockListener minimapBlockListener;

  @MonotonicNonNull
  private MinimapListener minimapListener;

  @MonotonicNonNull
  private SteerableViewFactory steerableViewFactory;

  @MonotonicNonNull
  private MinimapPlayerDatabase playerDataStorage;

  @Override
  @SneakyThrows
  public void onEnable() {
    System.setProperty("com.j256.simplelogging.level", "ERROR");
    PLUGIN.set(this);

    Path dataPath = getDataFolder().toPath();
    Config.instance().reload(dataPath.resolve("config.yml"));
    BlockConfig.instance().reload(dataPath.resolve("blocks.yml"));

    Metrics metrics = new Metrics(this, 20833);
    metrics.addCustomChart(new SimplePie("minimap_renderer", () -> Config.instance().defaultMinimapRenderer));
    metrics.addCustomChart(new SimplePie("default_position", () -> Config.instance().defaultPosition.toString().toLowerCase(Locale.ROOT)));
    metrics.addCustomChart(new SimplePie("enabled_by_default", () -> String.valueOf(Config.instance().enabledByDefault)));

    Path iconsPath = dataPath.resolve("icons");
    Files.createDirectories(iconsPath);

    playerDataStorage = new MinimapPlayerDatabase(dataPath.resolve("players.db"));

    defaultClientsideMinimapFactory = new NMSClientsideMinimapFactory();
    defaultMinimapPacketSender = new NMSMinimapPacketSender(this);
    minimapIconProvider = new BuiltinMinimapIconProvider(iconsPath);
    worldRendererProvider = new BuiltinMinimapWorldRendererProvider();
    minimapBlockListener = new MinimapBlockListener(this);
    minimapListener = new MinimapListener(this);
    steerableViewFactory = new NMSSteerableViewFactory();

    defaultWorldRenderer = worldRendererProvider.create(Config.instance().defaultMinimapRenderer);
    if (defaultWorldRenderer == null) {
      throw new IllegalArgumentException("default-world-renderer");
    }

    if (defaultWorldRenderer instanceof CacheableWorldMinimapRenderer cacheable) {
      minimapBlockListener.registerCache(cacheable.getWorldMapCache());
    }

    Bukkit.getPluginManager().registerEvents(this, this);
    Bukkit.getPluginManager().registerEvents(minimapListener, this);
    minimapBlockListener.registerListener(this);

    new MinimapCommand(this).register(NMSCommandDispatcherAccessor.vanillaDispatcher());
  }

  @Override
  @SneakyThrows
  public void onDisable() {
    playerDataStorage.close();
  }

  @Override
  public ClientsideMinimapFactory clientsideMinimapFactory() {
    return defaultClientsideMinimapFactory;
  }

  @Override
  public MinimapPacketSender packetSender() {
    return defaultMinimapPacketSender;
  }

  @Override
  public WorldMinimapRenderer worldRenderer() {
    return defaultWorldRenderer;
  }

  @Override
  public MinimapIconProvider iconProvider() {
    return minimapIconProvider;
  }

  @Override
  public MinimapWorldRendererProvider worldRendererProvider() {
    return worldRendererProvider;
  }

  @Override
  public MinimapListener minimapListener() {
    return minimapListener;
  }

  @Override
  public MinimapBlockListener minimapBlockListener() {
    return minimapBlockListener;
  }

  @Override
  public SteerableViewFactory steerableViewFactory() {
    return steerableViewFactory;
  }

  @Override
  public MinimapPlayerDatabase playerDataStorage() {
    return playerDataStorage;
  }

  @Override
  public Minimap getPlayerMinimap(Player player) {
    return minimapListener.getPlayerMinimaps().get(player.getUniqueId());
  }

  @Override
  public FullscreenMinimap getFullscreenMinimap(Player player) {
    return minimapListener.getFullscreenMinimaps().get(player.getUniqueId());
  }

  public PassengerRewriter getPassengerRewriter(Player player) {
    return passengerRewriters.get(player);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerJoin(PlayerJoinEvent event) {
    PassengerRewriter rewriter = new PassengerRewriter();
    ((CraftPlayer) event.getPlayer()).getHandle().connection.connection.channel.pipeline().addBefore("packet_handler", "passenger_rewriter", rewriter);
    passengerRewriters.put(event.getPlayer(), rewriter);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerQuit(PlayerQuitEvent event) {
    passengerRewriters.remove(event.getPlayer());
  }
}
