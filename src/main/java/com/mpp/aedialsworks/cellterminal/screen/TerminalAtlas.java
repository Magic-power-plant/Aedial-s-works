package com.mpp.aedialsworks.cellterminal.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Pixel geometry and original DiskTerminal atlas; no resampled bitmap or generic menu buttons. */
public final class TerminalAtlas {
    public static final int WIDTH = 208, ROW_HEIGHT = 18, CONTENT_TOP = 34;
    public static final int TEXT = 0x404040, SECONDARY = 0x707070;
    private static final ResourceLocation ATLAS = new ResourceLocation("aedialsworks", "textures/gui/cellterminal/atlas.png");
    private TerminalAtlas() {}

    public static void sprite(GuiGraphics g, int x, int y, int u, int v, int size) {
        g.blit(ATLAS, x, y, u, v, size, size, 128, 128);
    }

    public static void slot(GuiGraphics g, int x, int y, boolean partition) {
        g.blit(ATLAS, x, y, partition ? 18 : 0, 108, 18, 18, 128, 128);
    }

    /** Original AE2 bevel: white upper edge, dark lower edge and a two-pixel inset. */
    public static void panel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x + 2, y, x + width - 2, y + height, 0xFF151515);
        g.fill(x, y + 2, x + width, y + height - 2, 0xFF151515);
        g.fill(x + 1, y + 2, x + width - 1, y + height - 2, 0xFFC6C6C6);
        g.fill(x + 2, y + 1, x + width - 2, y + height - 1, 0xFFC6C6C6);
        g.fill(x + 2, y + 1, x + width - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 2, x + 3, y + height - 3, 0xFFFFFFFF);
        g.fill(x + 3, y + height - 3, x + width - 2, y + height - 1, 0xFF555555);
        g.fill(x + width - 3, y + 3, x + width - 1, y + height - 2, 0xFF555555);
    }

    public static void inset(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xFF373737);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFB9B9B9);
        g.fill(x + 1, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        g.fill(x + width - 1, y + 1, x + width, y + height, 0xFFFFFFFF);
    }
}
