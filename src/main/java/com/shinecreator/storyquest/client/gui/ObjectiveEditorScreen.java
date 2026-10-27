package com.shinecreator.storyquest.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.shinecreator.storyquest.client.gui.theme.QuestButton;
import com.shinecreator.storyquest.client.gui.theme.QuestEditBox;
import com.shinecreator.storyquest.client.gui.theme.QuestTheme;
import com.shinecreator.storyquest.client.gui.theme.QuestUiRenderer;
import com.shinecreator.storyquest.quest.MarkerIcon;
import com.shinecreator.storyquest.quest.Objective;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Editor for a single objective: manual or "visit this place".
 */
public class ObjectiveEditorScreen extends Screen {
    private record Label(int x, int y, Component text) {}

    private final QuestFormScreen parent;
    private final int index;
    private final Objective draft;
    private final boolean isNew;
    private final List<Label> labels = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private EditBox textBox;
    private EditBox dimensionBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private EditBox radiusBox;
    private int markerSummaryX = -1;
    private int markerSummaryY;

    public ObjectiveEditorScreen(QuestFormScreen parent, int index, Objective objective, boolean isNew) {
        super(Component.translatable("storyquest.editor.objective_title"));
        this.parent = parent;
        this.index = index;
        this.draft = objective;
        this.isNew = isNew;
    }

    @Override
    protected void init() {
        QuestTheme theme = QuestTheme.current();
        int padding = theme.padding();
        panelWidth = Math.min(430, width - 16);
        panelHeight = Math.min(286, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        contentX = panelX + padding;
        contentY = panelY + 38;
        contentWidth = panelWidth - padding * 2;

        labels.clear();
        textBox = null;
        dimensionBox = null;
        xBox = null;
        yBox = null;
        zBox = null;
        radiusBox = null;
        markerSummaryX = -1;

        int y = contentY;
        addRenderableWidget(QuestButton.normal(typeLabel(), button -> {
            captureFields();
            draft.type = draft.type == Objective.Type.VISIT ? Objective.Type.MANUAL : Objective.Type.VISIT;
            rebuildWidgets();
        }, contentX, y, 150, 18));
        y += 26;

        labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.objective_text")));
        textBox = addEditBox(contentX + 96, y, contentWidth - 96, draft.text, 200);
        y += 26;

        if (draft.type == Objective.Type.VISIT) {
            labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.dimension")));
            dimensionBox = addEditBox(contentX + 96, y, contentWidth - 96, draft.normalizeDimension(), 100);
            y += 26;

            int fieldWidth = (contentWidth - 96 - 8) / 3;
            labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.coordinates")));
            xBox = addEditBox(contentX + 96, y, fieldWidth, format(draft.x), 32);
            yBox = addEditBox(contentX + 96 + fieldWidth + 4, y, fieldWidth, format(draft.y), 32);
            zBox = addEditBox(contentX + 96 + (fieldWidth + 4) * 2, y, fieldWidth, format(draft.z), 32);
            y += 26;

            labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.radius")));
            radiusBox = addEditBox(contentX + 96, y, 60, format(draft.radius), 10);
            addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.editor.my_position"),
                    button -> useMyPosition(), contentX + 164, y, 120, 18));
            y += 26;

            labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.marker")));
            addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.editor.marker_button"),
                    button -> openMarkerEditor(), contentX + 96, y, 120, 18));
            markerSummaryX = contentX + 96 + 126;
            markerSummaryY = y + 3;
            y += 26;

            int bottom = panelY + panelHeight - theme.padding() - 26;
            if (y + 10 < bottom) {
                labels.add(new Label(contentX, y, Component.translatable("storyquest.editor.hint.visit")));
            }
        } else {
            labels.add(new Label(contentX, y, Component.translatable("storyquest.editor.hint.manual")));
        }

        int bottom = panelY + panelHeight - theme.padding() - 18;
        addRenderableWidget(QuestButton.primary(Component.translatable("storyquest.button.ok"),
                button -> confirm(), panelX + panelWidth - theme.padding() - 70, bottom, 70, 18));
        addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.button.cancel"),
                button -> cancel(), panelX + panelWidth - theme.padding() - 70 - 6 - 76, bottom, 76, 18));
    }

    private EditBox addEditBox(int x, int y, int width, String value, int maxLength) {
        QuestEditBox box = new QuestEditBox(font, x, y, width, 18);
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    private Component typeLabel() {
        Component type = Component.translatable(draft.type.translationKey());
        return Component.translatable("storyquest.editor.type_label", type);
    }

    private void useMyPosition() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        draft.dimension = player.level().dimension().location().toString();
        draft.x = player.getX();
        draft.y = player.getY();
        draft.z = player.getZ();
        if (dimensionBox != null) {
            dimensionBox.setValue(draft.dimension);
        }
        if (xBox != null) {
            xBox.setValue(format(draft.x));
        }
        if (yBox != null) {
            yBox.setValue(format(draft.y));
        }
        if (zBox != null) {
            zBox.setValue(format(draft.z));
        }
    }

    /** Copies the values currently typed into the widgets back into the draft. */
    private void captureFields() {
        if (textBox != null) {
            draft.text = textBox.getValue();
        }
        if (draft.type == Objective.Type.VISIT) {
            if (dimensionBox != null) {
                draft.dimension = dimensionBox.getValue();
            }
            draft.x = parseDouble(xBox, draft.x);
            draft.y = parseDouble(yBox, draft.y);
            draft.z = parseDouble(zBox, draft.z);
            draft.radius = Math.max(0.5D, Math.min(512.0D, parseDouble(radiusBox, draft.radius)));
        }
    }

    private void openMarkerEditor() {
        captureFields();
        if (minecraft != null) {
            minecraft.setScreen(new MarkerEditorScreen(this, draft, draft.copy()));
        }
    }

    private void confirm() {
        draft.text = textBox == null ? draft.text : textBox.getValue().trim();
        if (draft.text.isBlank()) {
            draft.text = Component.translatable("storyquest.editor.objective", index + 1).getString();
        }
        if (draft.type == Objective.Type.VISIT) {
            if (dimensionBox != null) {
                draft.dimension = dimensionBox.getValue().trim();
            }
            draft.x = parseDouble(xBox, draft.x);
            draft.y = parseDouble(yBox, draft.y);
            draft.z = parseDouble(zBox, draft.z);
            draft.radius = Math.max(0.5D, Math.min(512.0D, parseDouble(radiusBox, draft.radius)));
        }
        parent.applyObjective(index, draft);
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        QuestTheme theme = QuestTheme.current();
        int padding = theme.padding();
        QuestUiRenderer.drawPanel(graphics, panelX, panelY, panelWidth, panelHeight);
        QuestUiRenderer.drawDivider(graphics, contentX, panelY + 30, contentWidth);

        Component header = isNew
                ? Component.translatable("storyquest.editor.new_objective_title")
                : Component.translatable("storyquest.editor.objective_title_number", index + 1);
        graphics.drawString(font, header, contentX, panelY + 9, theme.text(), false);

        for (Label label : labels) {
            graphics.drawString(font, label.text(), label.x(), label.y(), theme.textDim(), false);
        }

        if (draft.type == Objective.Type.VISIT && markerSummaryX >= 0) {
            if (!draft.markerVisible) {
                graphics.drawString(font, Component.translatable("storyquest.editor.marker_hidden"),
                        markerSummaryX, markerSummaryY, theme.textMuted(), false);
            } else {
                MarkerIcon icon = draft.markerIcon == null ? MarkerIcon.CHECK : draft.markerIcon;
                MarkerIcons.draw(graphics, icon, markerSummaryX, markerSummaryY - 4, 16,
                        draft.markerColor, 1.0F);
                String summary = MarkerEditorScreen.formatColor(draft.markerColor) + "  " + draft.markerLabel();
                int maxWidth = Math.max(30, contentX + contentWidth - (markerSummaryX + 20));
                summary = font.plainSubstrByWidth(summary, maxWidth);
                graphics.drawString(font, summary, markerSummaryX + 20, markerSummaryY,
                        theme.textDim(), false);
            }
        }

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static double parseDouble(EditBox box, double fallback) {
        if (box == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(box.getValue().trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
