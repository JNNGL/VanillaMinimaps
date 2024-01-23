package com.jnngl.vanillaminimaps.clientside;

import org.bukkit.entity.Player;

public interface SteerableViewFactory {

  SteerableLockedView lockedView(Player player);
}
