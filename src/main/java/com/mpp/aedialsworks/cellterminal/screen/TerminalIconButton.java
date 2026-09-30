package com.mpp.aedialsworks.cellterminal.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Atlas-backed settings and item-icon tabs, with keyboard narration and full hover labels. */
public final class TerminalIconButton extends Button {
    public enum Icon { TAB, STYLE, LIMIT, VISIBILITY, REFRESH, FILTER, SEARCH, HELP, BACK, SETTINGS, SOURCE }
    private final Icon icon;
    private ItemStack item = ItemStack.EMPTY;
    private String badge = "";
    private int state;
    private boolean selected;

    public TerminalIconButton(int x, int y, int size, Component label, Icon icon, Runnable action) {
        super(x, y, size, size, label, button -> action.run(), DEFAULT_NARRATION);
        this.icon = icon;
        label(label);
    }

    public void label(Component label) { setMessage(label); setTooltip(Tooltip.create(label)); }
    public void item(ItemStack item) { this.item = item; }
    public void badge(String badge) { this.badge = badge; }
    public void state(int state) { this.state = state; }
    public void selected(boolean selected) { this.selected = selected; }

    @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int x = getX(), y = getY(), size = getWidth();
        boolean hover = isHoveredOrFocused();
        if (icon == Icon.TAB) {
            int color = !active ? 0xFF686868 : selected ? 0xFFC6C6C6 : hover ? 0xFFA0A0A0 : 0xFF8B8B8B;
            g.fill(x, y, x + size, y + size, color);
            g.fill(x, y, x + size, y + 1, 0xFFFFFFFF);
            g.fill(x, y, x + 1, y + size, 0xFFFFFFFF);
            g.fill(x + size - 1, y, x + size, y + size, 0xFF555555);
            if (!selected) g.fill(x, y + size - 1, x + size, y + size, 0xFF555555);
        } else if (icon == Icon.SEARCH) {
            TerminalAtlas.sprite(g, x, y, state * 10, 40 + (hover ? 10 : 0), 10);
        } else if (icon == Icon.HELP) {
            TerminalAtlas.sprite(g, x, y, 50, 40 + (hover ? 10 : 0), 10);
        } else if (icon == Icon.BACK) {
            TerminalAtlas.sprite(g, x, y, 36, 16 + (hover ? 12 : 0), 12);
        } else {
            TerminalAtlas.sprite(g, x, y, icon == Icon.REFRESH || icon == Icon.VISIBILITY && state == 2 ? 64 : 16, 60 + (hover ? 16 : 0), 16);
        }
        if (!item.isEmpty()) {
            int offset = (size - 16) / 2;
            g.renderItem(item, x + offset, y + offset);
        }
        if (icon == Icon.VISIBILITY && state == 0) TerminalAtlas.sprite(g, x + 3, y + 3, 30, 40, 10);
        if (icon == Icon.VISIBILITY && state == 1) TerminalAtlas.sprite(g, x, y, 96, 60, 16);
        if (icon == Icon.STYLE) {
            int h = state == 0 ? 7 : 12;
            g.fill(x + 4, y + 2, x + 12, y + 2 + h, 0xFFE8E8E8);
            g.fill(x + 5, y + 3, x + 11, y + 1 + h, 0xFF606060);
            g.fill(x + 5, y + h - 1, x + 11, y + h, 0xFFE8E8E8);
        }
        if (icon == Icon.FILTER) g.fill(x + 11, y + 11, x + 15, y + 15, state == 0 ? 0xFFE1E1E1 : state == 1 ? 0xFF49D449 : 0xFFDD5555);
        if (!badge.isEmpty()) {
            var font = Minecraft.getInstance().font;
            g.drawString(font, badge, x + (size - font.width(badge)) / 2, y + (size - 8) / 2, 0xFFFFFF, true);
        }
        if (!active) g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x88707070);
    }
}
