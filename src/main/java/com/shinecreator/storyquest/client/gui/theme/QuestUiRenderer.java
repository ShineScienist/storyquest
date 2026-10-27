package com.shinecreator.storyquest.client.gui.theme;

import com.shinecreator.storyquest.client.gui.theme.QuestTheme.Slice;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws the 9-slice textures and small reusable decorations of the active theme.
 */
public final class QuestUiRenderer {
    private QuestUiRenderer() {}

    public static QuestTheme theme() {
        return QuestTheme.current();
    }

    /**
     * Draws the main screen panel. Book themes get a leather cover with an
     * inner paper page, paper themes use a single parchment layer.
     */
    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        QuestTheme theme = QuestTheme.current();
        blitNine(graphics, theme.panel(), x, y, width, height);
        if (theme.page() != null && width > 12 && height > 12) {
            blitNine(graphics, theme.page(), x + 6, y + 6, width - 12, height - 12);
        }
    }

    public static void drawInset(GuiGraphics graphics, int x, int y, int width, int height) {
        blitNine(graphics, QuestTheme.current().inset(), x, y, width, height);
    }

    /**
     * Compact HUD background. Book keeps only the leather cover so the light
     * HUD text stays readable; Paper uses the parchment panel as is.
     */
    public static void drawHudPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        blitNine(graphics, QuestTheme.current().panel(), x, y, width, height);
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y, int width, int height) {
        blitNine(graphics, QuestTheme.current().slot(), x, y, width, height);
    }

    public static void drawButton(GuiGraphics graphics, Slice slice, int x, int y, int width, int height) {
        blitNine(graphics, slice, x, y, width, height);
    }

    public static void drawDivider(GuiGraphics graphics, int x, int y, int width) {
        Slice divider = QuestTheme.current().divider();
        tile(graphics, divider, x, y, width, divider.height(), 0, 0, divider.width(), divider.height());
    }

    /** Vertical leather strip between the quest list and the details, Book only. */
    public static void drawSpine(GuiGraphics graphics, int x, int y, int height) {
        Slice spine = QuestTheme.current().spine();
        if (spine == null) {
            return;
        }
        tile(graphics, spine, x, y, spine.width(), height, 0, 0, spine.width(), spine.height());
    }

    /** Theme-coloured ornament: a wax seal for Paper, a ribbon for Book. */
    public static void drawDecoration(GuiGraphics graphics, int x, int y) {
        Slice decoration = QuestTheme.current().decoration();
        graphics.blit(decoration.location(), x, y, decoration.width(), decoration.height(),
                0.0F, 0.0F, decoration.width(), decoration.height(),
                decoration.width(), decoration.height());
    }

    public static void drawIcon(GuiGraphics graphics, String name, int x, int y, int size) {
        graphics.blit(QuestTheme.current().icon(name), x, y, size, size,
                0.0F, 0.0F, 16, 16, 16, 16);
    }

    public static void drawCheckbox(GuiGraphics graphics, int x, int y, boolean checked) {
        drawSlot(graphics, x, y, 11, 11);
        if (checked) {
            drawIcon(graphics, "check", x, y, 11);
        }
    }

    public static void drawScrollbar(GuiGraphics graphics, int x, int y, int height,
                                     int scroll, int maxScroll, int visibleHeight) {
        if (height <= 0) {
            return;
        }
        QuestTheme theme = QuestTheme.current();
        Slice track = theme.scrollTrack();
        graphics.blit(track.location(), x, y, 6, height, 0.0F, 0.0F, 8, 16, 8, 16);
        if (maxScroll <= 0 || visibleHeight <= 0) {
            return;
        }
        Slice thumb = theme.scrollThumb();
        int contentHeight = visibleHeight + maxScroll;
        int thumbHeight = Math.min(height, Math.max(12, (int) ((long) height * visibleHeight / contentHeight)));
        int thumbY = y + (int) ((long) (height - thumbHeight) * scroll / maxScroll);
        graphics.blit(thumb.location(), x, thumbY, 6, thumbHeight, 0.0F, 0.0F, 8, 16, 8, 16);
    }

    /** Multiplies the colour channels of a texture blit. */
    public static void tint(GuiGraphics graphics, float red, float green, float blue, float alpha) {
        graphics.setColor(red, green, blue, alpha);
    }

    public static void clearTint(GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static int alpha(int rgb, float alpha) {
        int value = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (value << 24) | (rgb & 0x00FFFFFF);
    }

    public static void blitNine(GuiGraphics graphics, Slice slice, int x, int y, int width, int height) {
        if (slice == null || width <= 0 || height <= 0) {
            return;
        }
        int border = slice.border();
        int textureWidth = slice.width();
        int textureHeight = slice.height();
        if (border <= 0) {
            tile(graphics, slice, x, y, width, height, 0, 0, textureWidth, textureHeight);
            return;
        }
        if (width < border * 2 || height < border * 2) {
            graphics.blit(slice.location(), x, y, width, height,
                    0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
            return;
        }

        graphics.blit(slice.location(), x, y, border, border,
                0.0F, 0.0F, border, border, textureWidth, textureHeight);
        graphics.blit(slice.location(), x + width - border, y, border, border,
                textureWidth - border, 0.0F, border, border, textureWidth, textureHeight);
        graphics.blit(slice.location(), x, y + height - border, border, border,
                0.0F, textureHeight - border, border, border, textureWidth, textureHeight);
        graphics.blit(slice.location(), x + width - border, y + height - border, border, border,
                textureWidth - border, textureHeight - border, border, border, textureWidth, textureHeight);

        tile(graphics, slice, x + border, y, width - border * 2, border,
                border, 0, textureWidth - border * 2, border);
        tile(graphics, slice, x + border, y + height - border, width - border * 2, border,
                border, textureHeight - border, textureWidth - border * 2, border);
        tile(graphics, slice, x, y + border, border, height - border * 2,
                0, border, border, textureHeight - border * 2);
        tile(graphics, slice, x + width - border, y + border, border, height - border * 2,
                textureWidth - border, border, border, textureHeight - border * 2);
        tile(graphics, slice, x + border, y + border, width - border * 2, height - border * 2,
                border, border, textureWidth - border * 2, textureHeight - border * 2);
    }

    private static void tile(GuiGraphics graphics, Slice slice, int x, int y, int width, int height,
                             int u, int v, int tileWidth, int tileHeight) {
        if (width <= 0 || height <= 0 || tileWidth <= 0 || tileHeight <= 0) {
            return;
        }
        for (int offsetY = 0; offsetY < height; offsetY += tileHeight) {
            int partHeight = Math.min(tileHeight, height - offsetY);
            for (int offsetX = 0; offsetX < width; offsetX += tileWidth) {
                int partWidth = Math.min(tileWidth, width - offsetX);
                graphics.blit(slice.location(), x + offsetX, y + offsetY, partWidth, partHeight,
                        (float) u, (float) v, partWidth, partHeight,
                        slice.width(), slice.height());
            }
        }
    }
}
