package com.mpp.aedialsworks.powertools;
import appeng.api.networking.security.IActionHost;
import net.minecraft.world.entity.player.Player;
public interface PowerMenuHost extends IActionHost {
    PowerConfigurable configuration();
    String powerKind();
    boolean canUsePower(Player player);
}
