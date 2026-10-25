package com.shinecreator.storyquest.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.shinecreator.storyquest.client.StoryQuestClientConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/**
 * A simple, story-friendly dialogue window. Pages are separated by {@code |}.
 */
public class DialogueScreen extends Screen {
    private final String title;
    private final String speaker;
    private final String rawText;
    private final List<List<FormattedCharSequence>> pages = new ArrayList<>();

    private int page;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private Button nextButton;

    public DialogueScreen(String title, String speaker, String text) {
        super(Component.literal(title == null ? "" : title));
        this.title = title == null ? "" : title;
        this.speaker = speaker == null ? "" : speaker;
        this.rawText = text == null ? "" : text;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(400, width - 30);
        panelHeight = Math.min(240, height - 30);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        pages.clear();
        int textWidth = panelWidth - 32;
        int maxLines = Math.max(4, (panelHeight - 92) / 11);
        for (String pageText : rawText.split("\\|")) {
            String trimmed = pageText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            List<FormattedCharSequence> lines = font.split(Component.literal(trimmed), textWidth);
            for (int i = 0; i < lines.size(); i += maxLines) {
                pages.add(new ArrayList<>(lines.subList(i, Math.min(lines.size(), i + maxLines))));
            }
        }
        if (pages.isEmpty()) {
            pages.add(new ArrayList<>(font.split(Component.literal("..."), textWidth)));
        }
        page = Math.min(page, pages.size() - 1);

        int buttonY = panelY + panelHeight - 26;
        nextButton = addRenderableWidget(Button.builder(Component.translatable("storyquest.button.next"), button -> advance())
                .bounds(panelX + 14, buttonY, 80, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("storyquest.button.close"), button -> onClose())
                .bounds(panelX + panelWidth - 94, buttonY, 80, 18).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int accent = StoryQuestClientConfig.accentColor();
        int panel = 0xE8101218;

        graphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, accent);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, panel);

        if (!title.isBlank()) {
            graphics.drawString(font, Component.literal(title), panelX + 16, panelY + 10, 0xFF9AA0B4, false);
        }
        if (!speaker.isBlank()) {
            graphics.drawString(font, Component.literal(speaker).withStyle(ChatFormatting.BOLD),
                    panelX + 16, panelY + 26, accent, false);
        }
        graphics.fill(panelX + 14, panelY + 41, panelX + panelWidth - 14, panelY + 42,
                (accent & 0x00FFFFFF) | 0x60000000);

        int textY = panelY + 52;
        List<FormattedCharSequence> lines = pages.get(page);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, panelX + 16, textY, 0xFFE8E8EE, false);
            textY += 11;
        }

        if (pages.size() > 1) {
            String indicator = (page + 1) + " / " + pages.size();
            graphics.drawString(font, indicator, panelX + panelWidth - 16 - font.width(indicator),
                    panelY + panelHeight - 36, 0xFF7C8090, false);
        }

        nextButton.visible = page < pages.size() - 1;
        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            advance();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            advance();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void advance() {
        if (page < pages.size() - 1) {
            page++;
        } else {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
