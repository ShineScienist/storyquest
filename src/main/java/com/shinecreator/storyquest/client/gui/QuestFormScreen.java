package com.shinecreator.storyquest.client.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.shinecreator.storyquest.client.gui.theme.QuestButton;
import com.shinecreator.storyquest.client.gui.theme.QuestEditBox;
import com.shinecreator.storyquest.client.gui.theme.QuestMultiLineBox;
import com.shinecreator.storyquest.client.gui.theme.QuestTheme;
import com.shinecreator.storyquest.client.gui.theme.QuestUiRenderer;
import com.shinecreator.storyquest.network.QuestPayloads;
import com.shinecreator.storyquest.quest.MarkerIcon;
import com.shinecreator.storyquest.quest.Objective;
import com.shinecreator.storyquest.quest.Quest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Form editor for one quest. Fields are split into tabs so the whole form fits on screen.
 */
public class QuestFormScreen extends Screen {
    private enum Tab {
        MAIN("storyquest.editor.tab.main"),
        TEXT("storyquest.editor.tab.text"),
        OBJECTIVES("storyquest.editor.tab.objectives"),
        SCRIPT("storyquest.editor.tab.script");

        private final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    private record Label(int x, int y, Component text) {}

    private final QuestEditorScreen parent;
    private final Quest draft;
    private final String originalId;
    private final Map<Tab, QuestButton> tabButtons = new EnumMap<>(Tab.class);
    private final List<Label> labels = new ArrayList<>();

    private Tab tab = Tab.MAIN;
    private Component error;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentBottom;
    private int objectiveScroll;
    private int objectiveRowsTop;
    private int objectiveRows;

    public QuestFormScreen(QuestEditorScreen parent, Quest quest) {
        super(Component.translatable("storyquest.editor.form_title"));
        this.parent = parent;
        this.draft = quest;
        this.originalId = quest.id;
        this.draft.sanitize();
    }

    @Override
    protected void init() {
        QuestTheme theme = QuestTheme.current();
        int padding = theme.padding();
        panelWidth = Math.min(540, width - 16);
        panelHeight = Math.min(340, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        contentX = panelX + padding;
        contentWidth = panelWidth - padding * 2;
        contentY = panelY + 50;
        contentBottom = panelY + panelHeight - padding - 24;

        labels.clear();
        tabButtons.clear();

        Tab[] tabs = Tab.values();
        int tabWidth = (contentWidth - (tabs.length - 1) * 4) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            Tab value = tabs[i];
            QuestButton button = addRenderableWidget(QuestButton.normal(Component.translatable(value.key),
                    widget -> switchTab(value), contentX + i * (tabWidth + 4), panelY + 26, tabWidth, 18));
            button.setSelected(value == tab);
            tabButtons.put(value, button);
        }

        int bottom = panelY + panelHeight - padding - 18;
        addRenderableWidget(QuestButton.primary(Component.translatable("storyquest.button.save"),
                button -> save(), panelX + panelWidth - padding - 76, bottom, 76, 18));
        addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.button.cancel"),
                button -> cancel(), panelX + panelWidth - padding - 76 - 6 - 78, bottom, 78, 18));

        switch (tab) {
            case MAIN -> buildMain();
            case TEXT -> buildText();
            case OBJECTIVES -> buildObjectives();
            case SCRIPT -> buildScript();
        }
    }

    // ------------------------------------------------------------------ tabs

    private void buildMain() {
        int gap = 12;
        int labelWidth = 84;
        int leftWidth = Math.max(150, (contentWidth - gap) * 55 / 100);
        int rightX = contentX + leftWidth + gap;
        int rightWidth = Math.max(90, contentWidth - leftWidth - gap);

        int y = contentY;
        labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.id")));
        addEditBox(contentX + labelWidth, y, leftWidth - labelWidth, draft.id, value -> draft.id = value, 64);
        y += 22;

        labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.title")));
        addEditBox(contentX + labelWidth, y, leftWidth - labelWidth, draft.title, value -> draft.title = value, 200);
        y += 22;

        labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.chapter")));
        addEditBox(contentX + labelWidth, y, leftWidth - labelWidth, draft.chapter, value -> draft.chapter = value, 100);
        y += 22;

        labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.order")));
        addEditBox(contentX + labelWidth, y, 50, Integer.toString(draft.order),
                value -> draft.order = parseInt(value, draft.order), 6);
        y += 22;

        int descHeight = Math.max(44, Math.min(96, contentBottom - 48 - (contentY + 12)));
        labels.add(new Label(rightX, contentY, Component.translatable("storyquest.editor.field.description")));
        addMultiLine(rightX, contentY + 12, rightWidth, descHeight, draft.description,
                value -> draft.description = value);

        int flagsTop = Math.max(y + 6, contentY + 12 + descHeight + 6);
        int flagWidth = (contentWidth - 6) / 2;
        addFlagButton(contentX, flagsTop, flagWidth, "storyquest.editor.flag.visible",
                () -> draft.visible, () -> draft.visible = !draft.visible);
        addFlagButton(contentX + flagWidth + 6, flagsTop, flagWidth, "storyquest.editor.flag.manual_start",
                () -> draft.manualStart, () -> draft.manualStart = !draft.manualStart);
        addFlagButton(contentX, flagsTop + 20, flagWidth, "storyquest.editor.flag.auto_start",
                () -> draft.autoStart, () -> draft.autoStart = !draft.autoStart);
        addFlagButton(contentX + flagWidth + 6, flagsTop + 20, flagWidth, "storyquest.editor.flag.auto_complete",
                () -> draft.autoComplete, () -> draft.autoComplete = !draft.autoComplete);

        int conditionsY = flagsTop + 46;
        if (conditionsY < contentBottom - 10) {
            addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.editor.conditions_button"),
                    button -> openConditions(), contentX, conditionsY, 150, 18));
            labels.add(new Label(contentX + 158, conditionsY + 5, conditionSummary()));
        }
    }

    private Component conditionSummary() {
        List<String> parts = new ArrayList<>();
        if (!draft.requiredQuests.isEmpty()) {
            parts.add(Component.translatable(draft.requireAll
                    ? "storyquest.editor.conditions.summary_after"
                    : "storyquest.editor.conditions.summary_any", draft.requiredQuests.size()).getString());
        }
        if (draft.hideUntilAvailable) {
            parts.add(Component.translatable("storyquest.editor.conditions.summary_hidden").getString());
        }
        if (draft.scope == Quest.Scope.SHARED) {
            parts.add(Component.translatable("storyquest.editor.conditions.summary_shared").getString());
        }
        if (!draft.branch.isBlank()) {
            parts.add(Component.translatable("storyquest.editor.conditions.summary_branch", draft.branch).getString());
        }
        if (parts.isEmpty()) {
            return Component.translatable("storyquest.editor.conditions.summary_none");
        }
        return Component.literal(String.join("  ·  ", parts));
    }

    private void openConditions() {
        if (minecraft != null) {
            minecraft.setScreen(new QuestConditionsScreen(this, draft));
        }
    }

    List<Quest> allQuests() {
        return parent.allQuests();
    }

    String originalId() {
        return originalId;
    }

    private void buildText() {
        int y = contentY;
        labels.add(new Label(contentX, y + 5, Component.translatable("storyquest.editor.field.narrator")));
        addEditBox(contentX + 96, y, contentWidth - 96, draft.narrator, value -> draft.narrator = value, 100);
        y += 24;

        int boxHeight = clampBox((contentBottom - y - 51) / 2, 32, 56);

        labels.add(new Label(contentX, y, Component.translatable("storyquest.editor.field.intro")));
        addMultiLine(contentX, y + 12, contentWidth, boxHeight, draft.intro, value -> draft.intro = value);
        y += 12 + boxHeight + 8;

        labels.add(new Label(contentX, y, Component.translatable("storyquest.editor.field.outro")));
        addMultiLine(contentX, y + 12, contentWidth, boxHeight, draft.outro, value -> draft.outro = value);
        y += 12 + boxHeight + 8;

        labels.add(new Label(contentX, y, Component.translatable("storyquest.editor.hint.pages")));
    }

    private void buildObjectives() {
        labels.add(new Label(contentX, contentY + 2,
                Component.translatable("storyquest.editor.objectives_header", draft.objectives.size())));

        objectiveRowsTop = contentY + 18;
        int addButtonHeight = 20;
        int available = contentBottom - objectiveRowsTop - addButtonHeight - 4;
        objectiveRows = Math.max(1, available / 24);
        int maxScroll = Math.max(0, draft.objectives.size() - objectiveRows);
        objectiveScroll = Math.max(0, Math.min(objectiveScroll, maxScroll));

        int shown = Math.min(objectiveRows, Math.max(0, draft.objectives.size() - objectiveScroll));
        for (int i = 0; i < shown; i++) {
            int index = objectiveScroll + i;
            int rowY = objectiveRowsTop + i * 24;
            addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.button.edit_short"),
                    button -> openObjective(index), contentX + contentWidth - 66, rowY + 1, 44, 18));
            addRenderableWidget(QuestButton.normal(Component.literal("X"), button -> removeObjective(index),
                    contentX + contentWidth - 20, rowY + 1, 20, 18));
        }

        addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.button.add_objective"),
                button -> openObjective(draft.objectives.size()),
                contentX, objectiveRowsTop + objectiveRows * 24 + 2, 120, 18));
    }

    private void buildScript() {
        int y = contentY;
        int boxHeight = clampBox((contentBottom - y - 51) / 2, 40, 70);

        labels.add(new Label(contentX, y, Component.translatable("storyquest.editor.field.start_commands")));
        addMultiLine(contentX, y + 12, contentWidth, boxHeight, draft.startCommands, value -> draft.startCommands = value);
        y += 12 + boxHeight + 8;

        labels.add(new Label(contentX, y, Component.translatable("storyquest.editor.field.complete_commands")));
        addMultiLine(contentX, y + 12, contentWidth, boxHeight, draft.completeCommands, value -> draft.completeCommands = value);
        y += 12 + boxHeight + 8;

        labels.add(new Label(contentX, y, Component.translatable("storyquest.editor.hint.commands")));
    }

    private static int clampBox(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ------------------------------------------------------------------ widget helpers

    private void addEditBox(int x, int y, int width, String value, Consumer<String> responder, int maxLength) {
        QuestEditBox box = new QuestEditBox(font, x, y, width, 18);
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        box.setResponder(responder);
        addRenderableWidget(box);
    }

    private void addMultiLine(int x, int y, int width, int height, String value, Consumer<String> responder) {
        QuestMultiLineBox box = new QuestMultiLineBox(font, x, y, width, height);
        box.setCharacterLimit(4000);
        box.setValue(value == null ? "" : value);
        box.setValueListener(responder);
        addRenderableWidget(box);
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

    private void switchTab(Tab value) {
        tab = value;
        rebuildWidgets();
    }

    private void openObjective(int index) {
        boolean isNew = index < 0 || index >= draft.objectives.size();
        Objective objective;
        if (isNew) {
            objective = new Objective();
            objective.text = Component.translatable("storyquest.editor.new_objective", draft.objectives.size() + 1).getString();
            if (Minecraft.getInstance().player != null) {
                var player = Minecraft.getInstance().player;
                objective.dimension = player.level().dimension().location().toString();
                objective.x = player.getX();
                objective.y = player.getY();
                objective.z = player.getZ();
            }
        } else {
            objective = draft.objectives.get(index).copy();
        }
        if (minecraft != null) {
            minecraft.setScreen(new ObjectiveEditorScreen(this, isNew ? draft.objectives.size() : index, objective, isNew));
        }
    }

    private void removeObjective(int index) {
        if (index >= 0 && index < draft.objectives.size()) {
            draft.objectives.remove(index);
            objectiveScroll = Math.max(0, Math.min(objectiveScroll, Math.max(0, draft.objectives.size() - objectiveRows)));
            rebuildWidgets();
        }
    }

    public void applyObjective(int index, Objective objective) {
        if (index >= 0 && index < draft.objectives.size()) {
            draft.objectives.set(index, objective);
        } else {
            draft.objectives.add(objective);
        }
    }

    private void save() {
        error = null;
        String id = draft.id == null ? "" : draft.id.trim().toLowerCase(Locale.ROOT);
        if (!Quest.isValidId(id)) {
            error = Component.translatable("storyquest.message.invalid_id");
            return;
        }
        boolean renamed = !id.equals(originalId);
        if (renamed && parent.contains(id)) {
            error = Component.translatable("storyquest.message.duplicate_id", id);
            return;
        }

        draft.id = id;
        draft.sanitize();

        if (renamed && parent.contains(originalId)) {
            // The old entry (and progress tied to it) is dropped, then the renamed quest is saved.
            PacketDistributor.sendToServer(new QuestPayloads.DeleteQuest(originalId));
            parent.removeLocal(originalId);
        }
        PacketDistributor.sendToServer(new QuestPayloads.SaveQuest(draft.save()));
        parent.upsert(draft);
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

        graphics.drawString(font, Component.translatable("storyquest.editor.form_title", draft.id),
                contentX, panelY + 9, theme.text(), false);
        if (error != null) {
            graphics.drawString(font, error, contentX + contentWidth - font.width(error),
                    panelY + 9, theme.negative(), false);
        }
        QuestUiRenderer.drawDivider(graphics, contentX, panelY + 44, contentWidth);

        for (Label label : labels) {
            graphics.drawString(font, label.text(), label.x(), label.y(), theme.textDim(), false);
        }
        if (tab == Tab.OBJECTIVES) {
            drawObjectiveRows(graphics);
        }

        for (var entry : tabButtons.entrySet()) {
            entry.getValue().active = entry.getKey() != tab;
            entry.getValue().setSelected(entry.getKey() == tab);
        }
        QuestUiRenderer.drawDivider(graphics, contentX, contentBottom + 4, contentWidth);

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawObjectiveRows(GuiGraphics graphics) {
        QuestTheme theme = QuestTheme.current();
        if (draft.objectives.isEmpty()) {
            graphics.drawString(font, Component.translatable("storyquest.editor.no_objectives"),
                    contentX, objectiveRowsTop + 4, theme.textMuted(), false);
            return;
        }
        int shown = Math.min(objectiveRows, draft.objectives.size() - objectiveScroll);
        for (int i = 0; i < shown; i++) {
            int index = objectiveScroll + i;
            int rowY = objectiveRowsTop + i * 24;
            Objective objective = draft.objectives.get(index);
            String type = Component.translatable(objective.type.translationKey()).getString();
            String line = (index + 1) + ". [" + type + "] " + objective.displayText();
            graphics.drawString(font, font.plainSubstrByWidth(line, contentWidth - 72),
                    contentX, rowY + 6, theme.textDim(), false);
            if (objective.type == Objective.Type.VISIT) {
                String coords = String.format(Locale.ROOT, "%.0f %.0f %.0f  r=%.0f",
                        objective.x, objective.y, objective.z, objective.radius);
                if (objective.markerVisible) {
                    MarkerIcon icon = objective.markerIcon == null ? MarkerIcon.CHECK : objective.markerIcon;
                    MarkerIcons.draw(graphics, icon, contentX, rowY + 13, 10, objective.markerColor, 0.9F);
                }
                graphics.drawString(font, coords, contentX + 14, rowY + 16, theme.textMuted(), false);
            }
        }
        if (draft.objectives.size() > objectiveRows) {
            String more = (objectiveScroll + 1) + "-" + (objectiveScroll + shown) + " / " + draft.objectives.size();
            graphics.drawString(font, more, contentX + contentWidth - font.width(more) - 70,
                    objectiveRowsTop + objectiveRows * 24 + 7, theme.textMuted(), false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.OBJECTIVES && draft.objectives.size() > objectiveRows) {
            int maxScroll = draft.objectives.size() - objectiveRows;
            int next = Math.max(0, Math.min(maxScroll, objectiveScroll + (scrollY > 0 ? -1 : 1)));
            if (next != objectiveScroll) {
                objectiveScroll = next;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
