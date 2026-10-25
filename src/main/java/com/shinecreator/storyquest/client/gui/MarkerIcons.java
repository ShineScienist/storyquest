package com.shinecreator.storyquest.client.gui;

import com.shinecreator.storyquest.quest.MarkerIcon;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Small helper for drawing tinted marker glyphs in GUI screens.
 */
final class MarkerIcons {
    private MarkerIcons() {}

    static void draw(GuiGraphics graphics, MarkerIcon icon, int x, int y, int size, int rgb, float alpha) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        graphics.setColor(red, green, blue, alpha);
        graphics.blit(icon.texture(), x, y, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
