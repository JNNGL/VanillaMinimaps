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

package com.jnngl.vanillaminimaps.command;

import com.google.common.collect.ImmutableList;
import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.clientside.SteerableLockedView;
import com.jnngl.vanillaminimaps.config.Config;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.MinimapScreenPosition;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.fullscreen.FullscreenMinimap;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import com.jnngl.vanillaminimaps.map.marker.MarkerMinimapLayer;
import com.jnngl.vanillaminimaps.map.renderer.MinimapIconRenderer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class MinimapCommand extends BrigadierCommand {

  public MinimapCommand(VanillaMinimaps plugin) {
    super(plugin);
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    var positionLiteral = Commands.literal("position");
    for (MinimapScreenPosition position : MinimapScreenPosition.values()) {
      positionLiteral.then(Commands.literal(position.toString().toLowerCase(Locale.ENGLISH))
          .executes(ctx -> position(ctx, position)));
    }

    dispatcher.register(
        Commands.literal("minimap")
            .then(Commands.literal("enable")
                .executes(context -> enable(context, sourceAsSinglePlayer(context)))
                .then(targetsExecute(this::enable)))
            .then(Commands.literal("disable")
                .executes(context -> disable(context, sourceAsSinglePlayer(context)))
                .then(targetsExecute(this::disable)))
            .then(positionLiteral)
            .then(Commands.literal("marker")
                .then(Commands.literal("add")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("icon", StringArgumentType.string())
                            .suggests(this::suggestIcons)
                            .executes(context -> addMarker(context, sourceAsSinglePlayer(context)))
                            .then(targetsExecute(this::addMarker)))))
                .then(Commands.literal("set")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(this::suggestMarkers)
                        .then(Commands.literal("icon")
                            .then(Commands.argument("icon", StringArgumentType.string())
                                .suggests(this::suggestIcons)
                                .executes(context -> changeMarkerIcon(context, sourceAsSinglePlayer(context)))
                                .then(targetsExecute(this::changeMarkerIcon))))
                        .then(Commands.literal("name")
                            .then(Commands.argument("new_name", StringArgumentType.string())
                                .executes(context -> renameMarker(context, sourceAsSinglePlayer(context)))
                                .then(targetsExecute(this::renameMarker))))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(this::suggestMarkers)
                        .executes(context -> removeMarker(context, sourceAsSinglePlayer(context)))
                        .then(targetsExecute(this::removeMarker))))
            .then(Commands.literal("fullscreen")
                .executes(context -> fullscreen(context, sourceAsSinglePlayer(context)))
                .then(targetsExecute(this::fullscreen))))
    );
  }

  @FunctionalInterface
  private interface MultiTargetCommand {
    int run(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) throws CommandSyntaxException;
  }

  private static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targetsExecute(final MultiTargetCommand command) {
    return Commands.argument("targets", EntityArgument.players())
            .requires(source -> source.hasPermission(2))
            .executes(context -> command.run(context, EntityArgument.getPlayers(context, "targets")));
  }

  private static Collection<ServerPlayer> sourceAsSinglePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return ImmutableList.of(context.getSource().getPlayerOrException());
  }
  
  private boolean save(Minimap minimap) {
    try {
      getPlugin().playerDataStorage().save(minimap);
      return true;
    } catch (Throwable e) {
      e.printStackTrace();
      return false;
    }
  }

  private int enable(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
    int affected = 0;

    for (ServerPlayer serverPlayer : targets) {
      Player player = serverPlayer.getBukkitEntity();

      try {
        getPlugin().playerDataStorage().enableMinimap(player);
        getPlugin().playerDataStorage().restore(getPlugin(), player);
      } catch (Throwable e) {
        e.printStackTrace();
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": Unable to load player data, see console for error"));
        continue;
      }

      affected++;
    }

    return affected;
  }

  private int disable(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
    int affected = 0;

    for (ServerPlayer serverPlayer : targets) {
      Player player = serverPlayer.getBukkitEntity();

      getPlugin().minimapListener().disableMinimap(player);

      try {
        getPlugin().playerDataStorage().disableMinimap(player);
      } catch (SQLException e) {
        e.printStackTrace();
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": Unable to save player data, see console for error"));
        continue;
      }

      affected++;
    }

    return affected;
  }

  private int position(CommandContext<CommandSourceStack> ctx, MinimapScreenPosition position) throws CommandSyntaxException {
    ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
    Player player = serverPlayer.getBukkitEntity();

    Minimap minimap = getPlugin().getPlayerMinimap(player);
    if (minimap == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Minimap is disabled");
    }

    minimap.screenPosition(position);
    minimap.update(getPlugin());

    if (!save(minimap)) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Unable to save player data, see console for error");
    }

    return 1;
  }

  private CompletableFuture<Suggestions> suggestIcons(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    Set<String> keys = getPlugin().iconProvider().genericIconKeys();
    keys.stream()
        .filter(key -> key.startsWith(builder.getRemaining()))
        .forEach(builder::suggest);
    return builder.buildFuture();
  }

  private CompletableFuture<Suggestions> suggestMarkers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    ServerPlayer serverPlayer = ctx.getSource().getPlayer();
    if (serverPlayer == null) {
      return builder.buildFuture();
    }

    Player player = serverPlayer.getBukkitEntity();
    Minimap minimap = getPlugin().getPlayerMinimap(player);
    if (minimap == null) {
      return builder.buildFuture();
    }

    minimap.secondaryLayers().forEach((name, value) -> {
      if ("player".equals(name) || "death_point".equals(name)) {
        return;
      }

      if (!name.startsWith(builder.getRemaining())) {
        return;
      }

      String suggestedName = name;
      if (suggestedName.contains(" ")) {
        suggestedName = "\"" + suggestedName + "\"";
      }

      builder.suggest(suggestedName);
    });

    return builder.buildFuture();
  }

  private MinimapIcon minimapIcon(String iconName) throws CommandSyntaxException {
    MinimapIcon icon = null;
    if (!getPlugin().iconProvider().specialIconKeys().contains(iconName)) {
      icon = getPlugin().iconProvider().getIcon(iconName);
    }

    if (icon == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Invalid icon.");
    }

    return icon;
  }

  private boolean modifyMarker(CommandSourceStack source,
                               Player player,
                               String markerName,
                               BiConsumer<Minimap, SecondaryMinimapLayer> consumer) throws CommandSyntaxException {
    Minimap minimap = getPlugin().getPlayerMinimap(player);
    if (minimap == null) {
      source.sendFailure(Component.literal(player.getName() + ": Minimap is disabled"));
      return false;
    }

    if ("player".equals(markerName) || "death_point".equals(markerName)) {
      source.sendFailure(Component.literal(player.getName() + ": This marker cannot be modified"));
      return false;
    }

    SecondaryMinimapLayer marker = minimap.secondaryLayers().get(markerName);
    if (marker == null) {
      source.sendFailure(Component.literal(player.getName() + ": There is no such marker"));
      return false;
    }

    consumer.accept(minimap, marker);
    minimap.update(getPlugin());

    if (!save(minimap)) {
      source.sendFailure(Component.literal(player.getName() + ": Unable to save player data, see console for error"));
      return false;
    }

    return true;
  }

  private int changeMarkerIcon(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
    String markerName = StringArgumentType.getString(ctx, "name");
    String iconName = StringArgumentType.getString(ctx, "icon");

    MinimapIcon icon = minimapIcon(iconName);

    int affected = 0;

    for (ServerPlayer serverPlayer : targets) {
      Player player = serverPlayer.getBukkitEntity();

      if(modifyMarker(ctx.getSource(), player, markerName, (minimap, marker) -> {
        marker.setRenderer(new MinimapIconRenderer(icon));
      })) {
        affected++;
      }
    }

    return affected;
  }

  private int renameMarker(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
    String markerName = StringArgumentType.getString(ctx, "name");
    String newMarkerName = StringArgumentType.getString(ctx, "new_name");

    int affected = 0;

    for (ServerPlayer serverPlayer : targets) {
      Player player = serverPlayer.getBukkitEntity();

      if (modifyMarker(ctx.getSource(), player, markerName, (minimap, marker) -> {
        if (minimap.secondaryLayers().remove(markerName, marker)) {
          minimap.secondaryLayers().put(newMarkerName, marker);
        }
      })) {
        affected++;
      }
    }

    return affected;
  }

  private int removeMarker(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
    String markerName = StringArgumentType.getString(ctx, "name");

    int affected = 0;

    for (ServerPlayer serverPlayer : targets) {
      Player player = serverPlayer.getBukkitEntity();

      Minimap minimap = getPlugin().getPlayerMinimap(player);
      if (minimap == null) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": Minimap is disabled"));
        continue;
      }

      if ("player".equals(markerName) || "death_point".equals(markerName)) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": This marker cannot be removed"));
        continue;
      }

      SecondaryMinimapLayer marker = minimap.secondaryLayers().remove(markerName);
      if (marker == null) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": There is no such marker"));
        continue;
      }

      getPlugin().packetSender().despawnLayer(minimap.holder(), marker.getBaseLayer());
      minimap.update(getPlugin());

      if (!save(minimap)) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": Unable to save player data, see console for error"));
        continue;
      }

      affected++;
    }

    return affected;
  }

  private int addMarker(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
    String markerName = StringArgumentType.getString(ctx, "name");
    String iconName = StringArgumentType.getString(ctx, "icon");

    int affected = 0;

    for (ServerPlayer serverPlayer : targets) {
      Player player = serverPlayer.getBukkitEntity();

      Minimap minimap = getPlugin().getPlayerMinimap(player);
      if (minimap == null) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": Minimap is disabled"));
        continue;
      }

      MinimapIcon icon = minimapIcon(iconName);

      if ("player".equals(markerName) || "death_point".equals(markerName)) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": This marker name is unavailable"));
        continue;
      }

      if (minimap.secondaryLayers().containsKey(markerName)) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": Marker with this name already exists"));
        continue;
      }

      int markers = (int) minimap.secondaryLayers().entrySet().stream()
              .filter(entry -> !"player".equals(entry.getKey())
                      && !"death_point".equals(entry.getKey())
                      && entry.getValue() instanceof MarkerMinimapLayer)
              .count();

      if (markers >= Config.instance().markers.customMarkers.limit) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": You cannot place more than " +
                Config.instance().markers.customMarkers.limit + " markers."));
        continue;
      }

      float depth = 0.05F + minimap.secondaryLayers().size() * 0.01F;
      MinimapLayer iconBaseLayer = getPlugin().clientsideMinimapFactory().createMinimapLayer(player.getWorld(), null);
      SecondaryMinimapLayer iconLayer = new MarkerMinimapLayer(iconBaseLayer, new MinimapIconRenderer(icon), true,
              Config.instance().markers.customMarkers.stickToBorder, player.getWorld(), (int) player.getX(), (int) player.getZ(), depth);
      minimap.secondaryLayers().put(markerName, iconLayer);

      getPlugin().packetSender().spawnLayer(player, iconBaseLayer);
      minimap.update(getPlugin());

      if (!save(minimap)) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": Unable to save player data, see console for error"));
        continue;
      }

      affected++;
    }

    return affected;
  }

  private int fullscreen(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
    int affected = 0;

    for (ServerPlayer serverPlayer : targets) {
      Player player = serverPlayer.getBukkitEntity();

      Minimap minimap = getPlugin().getPlayerMinimap(player);
      if (minimap == null) {
        ctx.getSource().sendFailure(Component.literal(player.getName() + ": Minimap is disabled"));
        continue;
      }

      if (getPlugin().getFullscreenMinimap(player) != null) {
        getPlugin().minimapListener().closeFullscreen(player);
        continue;
      }

      FullscreenMinimap fullscreenMinimap = FullscreenMinimap.create(getPlugin(), minimap);
      SteerableLockedView view = getPlugin().minimapListener().openFullscreen(fullscreenMinimap);

      view.onSneak(v -> getPlugin().minimapListener().closeFullscreen(player));

      affected++;
    }

    return affected;
  }
}
