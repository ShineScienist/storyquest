package com.shinecreator.storyquest.client.gui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Single-line input styled as a recessed slot of the active theme.
 */
public class QuestEditBox extends EditBox {
    public QuestEditBox(Font font, int x, int y, int width, int height) {
        super(font, x, y, width, height, Component.empty());
        setBordered(false);
        setTextColor(QuestTheme.current().text());
        setTextColorUneditable(QuestTheme.current().textMuted());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        QuestTheme theme = QuestTheme.current();
        setTextColor(theme.text());
        setTextColorUneditable(theme.textMuted());
        QuestUiRenderer.drawInset(graphics, getX() - 4, getY() - 1, getWidth() + 8, getHeight() + 2);

        // Unbordered edit boxes draw their text at the top edge, shift it to the middle.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, (getHeight() - 8) / 2.0F, 0.0F);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }
}
