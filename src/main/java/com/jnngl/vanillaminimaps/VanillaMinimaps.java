package com.jnngl.vanillaminimaps;

import com.jnngl.vanillaminimaps.clientside.ClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.MinimapPacketSender;
import com.jnngl.vanillaminimaps.clientside.impl.NMSClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.impl.NMSMinimapPacketSender;
import com.jnngl.vanillaminimaps.injection.PassengerRewriter;
import com.jnngl.vanillaminimaps.listener.MinimapListener;
import com.jnngl.vanillaminimaps.map.Minimap;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.HashMap;
import java.util.Map;

public final class VanillaMinimaps extends JavaPlugin implements Listener {

  @Getter
  private final Map<Player, PassengerRewriter> passengerRewriters = new HashMap<>();

  @Getter
  @MonotonicNonNull
  private ClientsideMinimapFactory defaultClientsideMinimapFactory;

  @Getter
  @MonotonicNonNull
  private MinimapPacketSender defaultMinimapPacketSender;

  @Getter
  @MonotonicNonNull
  private MinimapListener minimapListener;

  @Override
  public void onEnable() {
    defaultClientsideMinimapFactory = new NMSClientsideMinimapFactory();
    defaultMinimapPacketSender = new NMSMinimapPacketSender(this);
    minimapListener = new MinimapListener(this);

    Bukkit.getPluginManager().registerEvents(this, this);
    Bukkit.getPluginManager().registerEvents(minimapListener, this);
  }

  public PassengerRewriter getPassengerRewriter(Player player) {
    return passengerRewriters.get(player);
  }

  public Minimap getPlayerMinimap(Player player) {
    return minimapListener.getPlayerMinimaps().get(player);
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
