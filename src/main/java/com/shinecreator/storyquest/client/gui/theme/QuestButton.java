package com.shinecreator.storyquest.client.gui.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A button drawn with the active theme's pixel-art 9-slice texture.
 */
public class QuestButton extends Button {
    public enum Style {
        NORMAL,
        PRIMARY
    }

    private final Style style;
    private boolean selected;

    private QuestButton(int x, int y, int width, int height, Component message,
                        OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        this.style = style;
    }

    public static QuestButton of(Component message, OnPress onPress, int x, int y,
                                 int width, int height, Style style) {
        return new QuestButton(x, y, width, height, message, onPress, style);
    }

    public static QuestButton normal(Component message, OnPress onPress, int x, int y,
                                     int width, int height) {
        return of(message, onPress, x, y, width, height, Style.NORMAL);
    }

    public static QuestButton primary(Component message, OnPress onPress, int x, int y,
                                      int width, int height) {
        return of(message, onPress, x, y, width, height, Style.PRIMARY);
    }

    /** Marks the button as the current tab or page. */
    public QuestButton setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        QuestTheme theme = QuestTheme.current();
        boolean hovered = active && isHoveredOrFocused();
        QuestTheme.Slice texture;
        int textColor;

        if (!active && !selected) {
            texture = theme.buttonDisabled();
            textColor = theme.textMuted();
        } else if (style == Style.PRIMARY || selected) {
            texture = hovered ? theme.buttonPrimaryHover() : theme.buttonPrimary();
            textColor = theme.onAccent();
        } else {
            texture = hovered ? theme.buttonHover() : theme.button();
            textColor = theme.buttonText();
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        QuestUiRenderer.drawButton(graphics, texture, getX(), getY(), getWidth(), getHeight());
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (isFocused()) {
            drawFocus(graphics, theme);
        }

        int color = QuestUiRenderer.alpha(textColor, alpha);
        int textY = getY() + (getHeight() - 8) / 2;
        var font = Minecraft.getInstance().font;
        Component label = getMessage();
        int maxWidth = Math.max(0, getWidth() - 8);
        if (font.width(label) > maxWidth) {
            label = Component.literal(font.plainSubstrByWidth(label.getString(), maxWidth));
        }
        graphics.drawString(font, label, getX() + (getWidth() - font.width(label)) / 2, textY, color, false);
    }

    private void drawFocus(GuiGraphics graphics, QuestTheme theme) {
        int x = getX();
        int y = getY();
        int right = x + getWidth() - 1;
        int bottom = y + getHeight() - 1;
        int color = theme.accent();
        graphics.fill(x, y, right, y + 1, color);
        graphics.fill(x, bottom, right, bottom + 1, color);
        graphics.fill(x, y, x + 1, bottom, color);
        graphics.fill(right, y, right + 1, bottom, color);
    }
}
