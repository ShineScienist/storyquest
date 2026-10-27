package com.shinecreator.storyquest.client.gui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

/**
 * Multi-line input. Minecraft always draws its text in a light colour, so the
 * themed background is darkened to keep the text readable on parchment pages.
 */
public class QuestMultiLineBox extends MultiLineEditBox {
    public QuestMultiLineBox(Font font, int x, int y, int width, int height) {
        super(font, x, y, width, height, Component.empty(), Component.empty());
    }

    @Override
    protected void renderBackground(GuiGraphics graphics) {
        QuestTheme theme = QuestTheme.current();
        QuestUiRenderer.tint(graphics, 0.26F, 0.21F, 0.16F, 1.0F);
        QuestUiRenderer.drawInset(graphics, getX(), getY(), getWidth(), getHeight());
        QuestUiRenderer.clearTint(graphics);
        if (isFocused()) {
            int color = theme.accent();
            int right = getX() + getWidth() - 1;
            int bottom = getY() + getHeight() - 1;
            graphics.fill(getX(), getY(), right, getY() + 1, color);
            graphics.fill(getX(), bottom, right, bottom + 1, color);
            graphics.fill(getX(), getY(), getX() + 1, bottom, color);
            graphics.fill(right, getY(), right + 1, bottom, color);
        }
    }
}
