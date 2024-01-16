package com.jnngl.vanillaminimaps.injection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;

public class PassengerRewriter extends ChannelOutboundHandlerAdapter {

  private final Int2ObjectMap<IntList> passengers = new Int2ObjectOpenHashMap<>();

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof ClientboundSetPassengersPacket packet) {
      int vehicle = packet.getVehicle();
      IntList passengers = this.passengers.get(vehicle);
      if (passengers != null) {
        synchronized (passengers) {
          FriendlyByteBuf buf = new FriendlyByteBuf(ctx.alloc().ioBuffer());
          buf.writeVarInt(0x5D); // Packet ID
          buf.writeVarInt(packet.getVehicle()); // Vehicle ID
          buf.writeVarInt(packet.getPassengers().length + passengers.size()); // Passenger count
          for (int passenger : packet.getPassengers()) {
            buf.writeVarInt(passenger);
          }
          for (int passenger : passengers) {
            buf.writeVarInt(passenger);
          }
          ctx.write(buf);
          return;
        }
      }
    }

    ctx.write(msg);
  }

  public Int2ObjectMap<IntList> passengers() {
    return this.passengers;
  }

  public void addPassenger(int vehicle, int entity) {
    IntList list = passengers.computeIfAbsent(vehicle, k -> new IntArrayList());
    synchronized (list) {
      if (!list.contains(entity)) {
        list.add(entity);
      }
    }
  }

  public void removePassenger(int vehicle, int entity){
    IntList passengers = this.passengers.get(vehicle);
    if (passengers == null) {
      return;
    }

    synchronized (passengers) {
      passengers.rem(entity);
      if (passengers.isEmpty()) {
        this.passengers.remove(vehicle);
      }
    }
  }
}
