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

package com.jnngl.vanillaminimaps.clientside.impl;

import com.google.common.collect.ImmutableList;
import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.clientside.SteerableLockedView;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class NMSSteerableLockedView implements SteerableLockedView {

  private static final NonNullList<ItemStack> EMPTY_INVENTORY =
      IntStream.rangeClosed(0, 45).mapToObj(i -> ItemStack.EMPTY)
          .collect(NonNullList::create, NonNullList::add, NonNullList::addAll);

  private final Player player;
  protected final ServerPlayer viewer;
  private final Location origin;
  private Consumer<Void> sneakCallback;
  private boolean active;

  protected byte convertAngle(float angle) {
    return (byte) Math.floor(angle * 256.0F / 360.0F);
  }

  public NMSSteerableLockedView(Player player) {
    this.player = player;

    ServerLevel level = ((CraftWorld) player.getWorld()).getHandle();
    this.viewer = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "_"), ClientInformation.createDefault());
    this.viewer.setInvisible(true);
    this.viewer.setNoGravity(true);
    this.viewer.setSilent(true);
    this.viewer.setPos(player.getX(), Math.floor(player.getY()), player.getZ());
    this.viewer.setRot(player.getYaw(), player.getPitch());
    this.viewer.passengers = ImmutableList.of(((CraftPlayer) player).getHandle());
    this.origin = player.getLocation();

    ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
    ServerGamePacketListenerImpl connection = serverPlayer.connection;
    connection.send(new ClientboundPlayerInfoUpdatePacket(
        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER), List.of(
        new ClientboundPlayerInfoUpdatePacket.Entry(
            viewer.getUUID(), viewer.getGameProfile(),
            false, 0, GameType.CREATIVE, null, null)
    )));
    connection.send(viewer.getAddEntityPacket());
    connection.send(new ClientboundRotateHeadPacket(viewer, convertAngle(player.getYaw())));
    List<SynchedEntityData.DataValue<?>> metadata = viewer.getEntityData().getNonDefaultValues();
    if (metadata != null && !metadata.isEmpty()) {
      connection.send(new ClientboundSetEntityDataPacket(viewer.getId(), metadata));
    }

    connection.send(new ClientboundPlayerInfoUpdatePacket(
        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE),
        new ClientboundPlayerInfoUpdatePacket.Entry(serverPlayer.getUUID(), null, true, 0, GameType.ADVENTURE, null, null))
    );
    connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, GameType.SPECTATOR.getId()));

    inject(connection.connection.channel);

    Bukkit.getScheduler().runTaskLater(VanillaMinimaps.get(), () -> {
      connection.send(new ClientboundSetCameraPacket(viewer));
      connection.send(new ClientboundSetPassengersPacket(viewer));
      connection.send(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(viewer.getUUID())));

      int stateId = serverPlayer.inventoryMenu.incrementStateId();
      connection.send(new ClientboundContainerSetContentPacket(0, stateId, EMPTY_INVENTORY, ItemStack.EMPTY), null);
    }, 7L);

    active = true;
  }

  protected void inject(Channel channel) {
    channel.pipeline().addBefore("packet_handler", "view_handler",
        new ChannelDuplexHandler() {

          private static final Set<Class<? extends Packet<?>>> BLOCKED_INBOUND_PACKETS =
              Set.of(
                  ServerboundInteractPacket.class,
                  ServerboundContainerClickPacket.class,
                  ServerboundContainerButtonClickPacket.class,
                  ServerboundContainerClosePacket.class,
                  ServerboundEditBookPacket.class,
                  ServerboundSetCarriedItemPacket.class,
                  ServerboundSetBeaconPacket.class,
                  ServerboundSignUpdatePacket.class,
                  ServerboundSetStructureBlockPacket.class,
                  ServerboundSetJigsawBlockPacket.class,
                  ServerboundSetCreativeModeSlotPacket.class,
                  ServerboundSelectTradePacket.class,
                  ServerboundSetCommandBlockPacket.class,
                  ServerboundSetCommandMinecartPacket.class,
                  ServerboundUseItemPacket.class,
                  ServerboundUseItemOnPacket.class,
                  ServerboundPickItemPacket.class,
                  ServerboundPlaceRecipePacket.class,
                  ServerboundRenameItemPacket.class,
                  ServerboundPlayerActionPacket.class,
                  ServerboundSwingPacket.class,
                  ServerboundPlayerAbilitiesPacket.class
              );

          @Override
          public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
            if (BLOCKED_INBOUND_PACKETS.contains(msg.getClass())) {
              return;
            }

            if (msg instanceof ServerboundPlayerInputPacket packet) {
              if (packet.isShiftKeyDown()) {
                if (sneakCallback != null) {
                  sneakCallback.accept(null);
                }
              }
            }

            super.channelRead(ctx, msg);
          }

          @Override
          public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof ClientboundGameEventPacket packet) {
              if (packet.getEvent() == ClientboundGameEventPacket.CHANGE_GAME_MODE &&
                  packet.getParam() != GameType.ADVENTURE.getId()) {
                return;
              }
            } else if (msg instanceof ClientboundPlayerInfoUpdatePacket packet &&
                packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)) {
              EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = packet.actions();
              List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>();
              packet.entries().forEach(entry -> {
                if (entry.gameMode() == GameType.SPECTATOR || !entry.profileId().equals(player.getUniqueId())) {
                  entries.add(entry);
                }
              });

              if (!entries.isEmpty()) {
                super.write(ctx, new ClientboundPlayerInfoUpdatePacket(actions, entries), promise);
              }

              return;
            }

            super.write(ctx, msg, promise);
          }

          @Override
          public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
            destroy();
            super.channelInactive(ctx);
          }
        }
    );
  }

  protected void deject(Channel channel) {
    if (channel.isActive()) {
      channel.pipeline().remove("view_handler");
    }
  }

  @Override
  public void onSneak(Consumer<Void> callback) {
    this.sneakCallback = callback;
  }

  @Override
  public void destroy() {
    if (!active) {
      return;
    }

    active = false;
    ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
    ServerGamePacketListenerImpl connection = serverPlayer.connection;

    deject(connection.connection.channel);
    connection.send(new ClientboundSetCameraPacket(serverPlayer));
    connection.send(new ClientboundRemoveEntitiesPacket(viewer.getId()));
    List<SynchedEntityData.DataValue<?>> metadata = serverPlayer.getEntityData().getNonDefaultValues();
    if (metadata != null && !metadata.isEmpty()) {
      connection.send(new ClientboundSetEntityDataPacket(serverPlayer.getId(), metadata));
    }

    int stateId = serverPlayer.inventoryMenu.incrementStateId();
    connection.send(new ClientboundContainerSetContentPacket(
        0, stateId, serverPlayer.inventoryMenu.remoteSlots, serverPlayer.inventoryMenu.getCarried()));

    connection.send(new ClientboundPlayerInfoUpdatePacket(
        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE),
        new ClientboundPlayerInfoUpdatePacket.Entry(serverPlayer.getUUID(), null, true, 0, serverPlayer.gameMode.getGameModeForPlayer(), null, null))
    );
    connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, serverPlayer.gameMode.getGameModeForPlayer().getId()));

    Bukkit.getScheduler().runTask(VanillaMinimaps.get(), () -> player.teleport(origin));
  }
}
