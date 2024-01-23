package com.jnngl.vanillaminimaps.clientside;

import java.util.function.Consumer;

public interface SteerableLockedView {

  void onSneak(Consumer<Void> callback);

  void destroy();
}
