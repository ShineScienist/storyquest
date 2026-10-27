package com.shinecreator.storyquest.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import com.shinecreator.storyquest.client.gui.theme.QuestButton;
import com.shinecreator.storyquest.client.gui.theme.QuestEditBox;
import com.shinecreator.storyquest.client.gui.theme.QuestTheme;
import com.shinecreator.storyquest.client.gui.theme.QuestUiRenderer;
import com.shinecreator.storyquest.quest.Objective;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Per-objective marker settings: visibility, label, color, icon and the world beam.
 */
public class MarkerEditorScreen extends Screen {
    private record Label(int x, int y, Component text) {}

    private static final int[] PRESETS = {
            0xFFC94A, 0x7CD992, 0x6FA8FF, 0xFF6B6B, 0xC08CFF, 0xFFFFFF
    };

    private final Screen parent;
    private final Objective target;
    private final Objective draft;
    private final List<Label> labels = new ArrayList<>();
    private final List<int[]> swatches = new ArrayList<>();

    private Component error;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentBottom;
    private int previewX;
    private int previewY;
    private EditBox textBox;
    private EditBox colorBox;

    public MarkerEditorScreen(Screen parent, Objective target, Objective draft) {
        super(Component.translatable("storyquest.marker.title"));
        this.parent = parent;
        this.target = target;
        this.draft = draft;
    }

    @Override
    protected void init() {
        QuestTheme theme = QuestTheme.current();
        int padding = theme.padding();
        panelWidth = Math.min(440, width - 16);
        panelHeight = Math.min(228, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        contentX = panelX + padding;
        contentY = panelY + 38;
        contentWidth = panelWidth - padding * 2;
        contentBottom = panelY + panelHeight - padding - 18;

        labels.clear();
        swatches.clear();
        textBox = null;
        colorBox = null;

        int y = contentY;
        int flagWidth = (contentWidth - 6) / 2;
        addFlagButton(contentX, y, flagWidth, "storyquest.marker.show",
                () -> draft.markerVisible, () -> draft.markerVisible = !draft.markerVisible);
        addFlagButton(contentX + flagWidth + 6, y, flagWidth, "storyquest.marker.beam",
                () -> draft.markerBeam, () -> draft.markerBeam = !draft.markerBeam);
        y += 24;

        labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.marker.text")));
        textBox = addEditBox(contentX + 96, y, contentWidth - 96, draft.markerText, 64);
        textBox.setHint(Component.translatable("storyquest.marker.text_hint"));
        y += 24;

        labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.marker.color")));
        colorBox = addEditBox(contentX + 96, y, 70, formatColor(draft.markerColor), 7);
        int swatchStart = contentX + 96 + 70 + 8;
        int available = contentX + contentWidth - swatchStart;
        int swatchCount = Math.max(0, Math.min(PRESETS.length, (available + 4) / 18));
        int swatchX = swatchStart;
        for (int i = 0; i < swatchCount; i++) {
            swatches.add(new int[] {swatchX, y + 2, PRESETS[i]});
            swatchX += 18;
        }
        y += 24;

        labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.marker.icon")));
        addRenderableWidget(QuestButton.normal(Component.translatable(draft.markerIcon.translationKey()), button -> {
            draft.markerIcon = draft.markerIcon.next();
            button.setMessage(Component.translatable(draft.markerIcon.translationKey()));
        }, contentX + 96, y, 120, 18));
        previewX = contentX + 96 + 126;
        previewY = y + 1;
        y += 24;

        if (y + 10 < contentBottom) {
            labels.add(new Label(contentX, y, Component.translatable("storyquest.marker.hint")));
        }

        int bottom = panelY + panelHeight - padding - 18;
        addRenderableWidget(QuestButton.primary(Component.translatable("storyquest.button.ok"),
                button -> confirm(), panelX + panelWidth - padding - 70, bottom, 70, 18));
        addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.button.cancel"),
                button -> cancel(), panelX + panelWidth - padding - 70 - 6 - 76, bottom, 76, 18));
    }

    private void addFlagButton(int x, int y, int width, String key, BooleanSupplier getter, Runnable toggler) {
        QuestButton button = QuestButton.normal(flagLabel(key, getter.getAsBoolean()), widget -> {
            toggler.run();
            widget.setMessage(flagLabel(key, getter.getAsBoolean()));
        }, x, y, width, 18);
        addRenderableWidget(button);
    }

    private static Component flagLabel(String key, boolean value) {
        return Component.literal(value ? "[\u2714] " : "[  ] ").append(Component.translatable(key));
    }

    private EditBox addEditBox(int x, int y, int width, String value, int maxLength) {
        QuestEditBox box = new QuestEditBox(font, x, y, width, 18);
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    private void confirm() {
        Integer color = parseColor(colorBox == null ? null : colorBox.getValue());
        if (color == null) {
            error = Component.translatable("storyquest.marker.invalid_color");
            return;
        }
        draft.markerColor = color;
        draft.markerText = textBox == null ? "" : textBox.getValue().trim();
        if (draft.markerText.length() > 64) {
            draft.markerText = draft.markerText.substring(0, 64);
        }
        target.copyMarkerFrom(draft);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void cancel() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        cancel();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int[] swatch : swatches) {
            if (mouseX >= swatch[0] && mouseX < swatch[0] + 14
                    && mouseY >= swatch[1] && mouseY < swatch[1] + 14) {
                if (colorBox != null) {
                    colorBox.setValue(formatColor(swatch[2]));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        QuestTheme theme = QuestTheme.current();
        int padding = theme.padding();
        QuestUiRenderer.drawPanel(graphics, panelX, panelY, panelWidth, panelHeight);
        QuestUiRenderer.drawDivider(graphics, contentX, panelY + 30, contentWidth);

        graphics.drawString(font, Component.translatable("storyquest.marker.title"),
                contentX, panelY + 9, theme.text(), false);
        if (error != null) {
            graphics.drawString(font, error, contentX + contentWidth - font.width(error),
                    panelY + 9, theme.negative(), false);
        }

        for (Label label : labels) {
            graphics.drawString(font, label.text(), label.x(), label.y(), theme.textDim(), false);
        }

        int previewColor = previewColor();
        for (int[] swatch : swatches) {
            QuestUiRenderer.drawSlot(graphics, swatch[0] - 2, swatch[1] - 2, 18, 18);
            graphics.fill(swatch[0], swatch[1], swatch[0] + 14, swatch[1] + 14, 0xFF000000 | swatch[2]);
        }
        MarkerIcons.draw(graphics, draft.markerIcon, previewX, previewY, 16, previewColor, 1.0F);

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private int previewColor() {
        if (colorBox != null) {
            Integer parsed = parseColor(colorBox.getValue());
            if (parsed != null) {
                return parsed;
            }
        }
        return draft.markerColor & 0x00FFFFFF;
    }

    static Integer parseColor(String raw) {
        if (raw == null) {
            return null;
        }
        String hex = raw.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16) & 0x00FFFFFF;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static String formatColor(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0x00FFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
