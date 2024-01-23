package com.jnngl.vanillaminimaps.clientside.impl;

import com.jnngl.vanillaminimaps.clientside.SteerableLockedView;
import com.jnngl.vanillaminimaps.clientside.SteerableViewFactory;
import org.bukkit.entity.Player;

public class NMSSteerableViewFactory implements SteerableViewFactory {

  @Override
  public SteerableLockedView lockedView(Player player) {
    return new NMSSteerableLockedView(player);
  }
}
