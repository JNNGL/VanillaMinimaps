package com.jnngl.vanillaminimaps.clientside.impl;

import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.clientside.AbstractMinimapPacketSender;
import com.jnngl.vanillaminimaps.injection.PassengerRewriter;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collections;

public class NMSMinimapPacketSender extends AbstractMinimapPacketSender {

  private final VanillaMinimaps plugin;

  public NMSMinimapPacketSender(VanillaMinimaps plugin) {
    this.plugin = plugin;
  }

  @Override
  public void updateLayer(Player viewer, MinimapLayer layer, int x, int y, int width, int height, byte[] data) {
    MapItemSavedData.MapPatch patch = new MapItemSavedData.MapPatch(x, y, width, height, data);
    ((CraftPlayer) viewer).getHandle().connection.send(new ClientboundMapItemDataPacket(layer.mapId(), (byte) 0, false, Collections.emptyList(), patch));
  }

  private void spawnItemFrame(ServerPlayerConnection connection, ItemFrame itemFrame) {
    ServerPlayer player = connection.getPlayer();
    itemFrame.setPos(player.getX(), player.getY(), player.getZ());
    connection.send(itemFrame.getAddEntityPacket());
    var metadata = itemFrame.getEntityData().getNonDefaultValues();
    if (metadata != null && !metadata.isEmpty()) {
      connection.send(new ClientboundSetEntityDataPacket(itemFrame.getId(), metadata));
    }
  }

  @Override
  public void spawnLayer(Player viewer, MinimapLayer layer) {
    ItemFrame lowerFrame = (ItemFrame) layer.lowerFrame().entity();
    ItemFrame upperFrame = (ItemFrame) layer.upperFrame().entity();

    ServerPlayerConnection connection = ((CraftPlayer) viewer).getHandle().connection;
    spawnItemFrame(connection, lowerFrame);
    spawnItemFrame(connection, upperFrame);

    PassengerRewriter rewriter = plugin.getPassengerRewriter(viewer);
    rewriter.addPassenger(viewer.getEntityId(), lowerFrame.getId());
    rewriter.addPassenger(viewer.getEntityId(), upperFrame.getId());

    connection.send(new ClientboundSetPassengersPacket(((CraftPlayer) viewer).getHandle()));
  }

  @Override
  public void despawnLayer(Player viewer, MinimapLayer layer) {
    ItemFrame lowerFrame = (ItemFrame) layer.lowerFrame().entity();
    ItemFrame upperFrame = (ItemFrame) layer.upperFrame().entity();

    ServerPlayerConnection connection = ((CraftPlayer) viewer).getHandle().connection;
    connection.send(new ClientboundRemoveEntitiesPacket(lowerFrame.getId(), upperFrame.getId()));

    PassengerRewriter rewriter = plugin.getPassengerRewriter(viewer);
    rewriter.removePassenger(viewer.getEntityId(), lowerFrame.getId());
    rewriter.removePassenger(viewer.getEntityId(), upperFrame.getId());

    connection.send(new ClientboundSetPassengersPacket(((CraftPlayer) viewer).getHandle()));
  }
}
