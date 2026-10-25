package com.shinecreator.storyquest.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.shinecreator.storyquest.client.StoryQuestClientConfig;
import com.shinecreator.storyquest.quest.Quest;
import com.shinecreator.storyquest.quest.QuestConditions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Quest prerequisites, visibility and branch settings.
 * The screen edits a working copy and copies the fields back on Done.
 */
public class QuestConditionsScreen extends Screen {
    private static final int ROW_HEIGHT = 20;

    private record Label(int x, int y, Component text) {}
    private record Row(String id, String title, boolean missing) {}

    private final QuestFormScreen parent;
    private final Quest target;
    private final Quest draft;
    private final List<Quest> allQuests;
    private final List<Label> labels = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentBottom;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int rightX;
    private int rightWidth;
    private int scroll;
    private String search = "";
    private EditBox searchBox;
    private EditBox branchBox;
    private Button modeButton;
    private Button hideButton;
    private Button scopeButton;
    private Component error;

    public QuestConditionsScreen(QuestFormScreen parent, Quest quest) {
        super(Component.translatable("storyquest.editor.conditions_title"));
        this.parent = parent;
        this.target = quest;
        this.draft = quest.copy();
        this.allQuests = parent.allQuests();
        this.allQuests.removeIf(candidate -> candidate.id.equals(quest.id)
                || candidate.id.equals(parent.originalId()));
        this.allQuests.sort(Comparator
                .comparing((Quest candidate) -> candidate.chapter.toLowerCase(Locale.ROOT))
                .thenComparingInt(candidate -> candidate.order)
                .thenComparing(candidate -> candidate.title.toLowerCase(Locale.ROOT)));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(540, width - 16);
        panelHeight = Math.min(300, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        contentX = panelX + 14;
        contentWidth = panelWidth - 28;
        contentY = panelY + 36;

        int bottom = panelY + panelHeight - 26;
        contentBottom = bottom - 6;
        int gap = 12;
        listWidth = Math.max(150, Math.min(260, contentWidth * 55 / 100));
        rightX = contentX + listWidth + gap;
        rightWidth = Math.max(80, contentX + contentWidth - rightX);

        labels.clear();
        labels.add(new Label(contentX, contentY, Component.translatable("storyquest.editor.conditions.requirements")));
        labels.add(new Label(rightX, contentY, Component.translatable("storyquest.editor.conditions.mode")));
        labels.add(new Label(rightX, contentY + 32, Component.translatable("storyquest.editor.conditions.visibility")));
        labels.add(new Label(rightX, contentY + 64, Component.translatable("storyquest.editor.conditions.scope")));
        labels.add(new Label(rightX, contentY + 96, Component.translatable("storyquest.editor.conditions.branch")));

        searchBox = new EditBox(font, contentX, contentY + 13, listWidth, 18, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setValue(search);
        searchBox.setResponder(value -> {
            search = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            scroll = 0;
        });
        addRenderableWidget(searchBox);

        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> {
            draft.requireAll = !draft.requireAll;
            button.setMessage(modeLabel());
        }).bounds(rightX, contentY + 13, rightWidth, 18).build());

        hideButton = addRenderableWidget(Button.builder(hideLabel(), button -> {
            draft.hideUntilAvailable = !draft.hideUntilAvailable;
            button.setMessage(hideLabel());
        }).bounds(rightX, contentY + 44, rightWidth, 18).build());

        scopeButton = addRenderableWidget(Button.builder(scopeLabel(), button -> {
            draft.scope = draft.scope == Quest.Scope.SHARED ? Quest.Scope.SOLO : Quest.Scope.SHARED;
            button.setMessage(scopeLabel());
        }).bounds(rightX, contentY + 76, rightWidth, 18).build());

        branchBox = new EditBox(font, rightX, contentY + 108, rightWidth, 18, Component.empty());
        branchBox.setMaxLength(64);
        branchBox.setValue(draft.branch == null ? "" : draft.branch);
        branchBox.setResponder(value -> draft.branch = value == null ? "" : value);
        addRenderableWidget(branchBox);

        addRenderableWidget(Button.builder(Component.translatable("storyquest.editor.conditions.clear"),
                button -> draft.requiredQuests.clear())
                .bounds(contentX, bottom, 88, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("storyquest.button.done"), button -> done())
                .bounds(panelX + panelWidth - 14 - 70, bottom, 70, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("storyquest.button.cancel"), button -> cancel())
                .bounds(panelX + panelWidth - 14 - 70 - 76, bottom, 70, 18).build());

        listY = contentY + 38;
        listX = contentX;
        listHeight = Math.max(ROW_HEIGHT, contentBottom - listY);
    }

    private Component modeLabel() {
        return Component.translatable(draft.requireAll
                ? "storyquest.editor.conditions.mode_all"
                : "storyquest.editor.conditions.mode_any");
    }

    private Component hideLabel() {
        return Component.translatable(draft.hideUntilAvailable
                ? "storyquest.editor.conditions.hide_on"
                : "storyquest.editor.conditions.hide_off");
    }

    private Component scopeLabel() {
        return Component.translatable(draft.scope == Quest.Scope.SHARED
                ? "storyquest.editor.conditions.scope_shared"
                : "storyquest.editor.conditions.scope_solo");
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (String requiredId : draft.requiredQuests) {
            boolean known = false;
            for (Quest candidate : allQuests) {
                if (candidate.id.equals(requiredId)) {
                    known = true;
                    break;
                }
            }
            if (!known && matchesSearch(requiredId, requiredId)) {
                rows.add(new Row(requiredId,
                        Component.translatable("storyquest.editor.conditions.missing", requiredId).getString(), true));
            }
        }
        for (Quest candidate : allQuests) {
            if (matchesSearch(candidate.id, candidate.title)) {
                rows.add(new Row(candidate.id, candidate.title, false));
            }
        }
        return rows;
    }

    private boolean matchesSearch(String id, String title) {
        if (search.isBlank()) {
            return true;
        }
        return id.toLowerCase(Locale.ROOT).contains(search)
                || title.toLowerCase(Locale.ROOT).contains(search);
    }

    private Component conditionWarning() {
        for (String requiredId : draft.requiredQuests) {
            for (Quest candidate : allQuests) {
                if (candidate.id.equals(requiredId) && candidate.scope != draft.scope) {
                    return Component.translatable("storyquest.editor.conditions.mixed_scope", requiredId);
                }
            }
        }
        if (draft.autoStart && draft.branch != null && !draft.branch.isBlank()) {
            for (Quest candidate : allQuests) {
                if (candidate.autoStart && candidate.scope == draft.scope
                        && draft.branch.equals(candidate.branch)) {
                    return Component.translatable("storyquest.editor.conditions.auto_branch",
                            candidate.id);
                }
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int accent = StoryQuestClientConfig.accentColor();
        graphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, accent);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight,
                (StoryQuestClientConfig.panelColor() & 0x00FFFFFF) | 0xF0101218);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 24, 0x30FFFFFF);
        graphics.fill(panelX, panelY + 24, panelX + panelWidth, panelY + 25, accent);

        graphics.drawString(font, title, panelX + 10, panelY + 8, accent, true);
        Component warning = conditionWarning();
        if (warning != null) {
            graphics.drawString(font, font.plainSubstrByWidth(warning.getString(), panelWidth - 180),
                    panelX + 160, panelY + 8, 0xFFFFB86B, false);
        }
        if (error != null) {
            graphics.drawString(font, font.plainSubstrByWidth(error.getString(), panelWidth - 24),
                    panelX + 12, panelY + panelHeight - 40, 0xFFFF6B6B, false);
        }

        for (Label label : labels) {
            graphics.drawString(font, label.text(), label.x(), label.y(), 0xFFA8ADBC, false);
        }

        String selected = Component.translatable("storyquest.editor.conditions.selected",
                draft.requiredQuests.size()).getString();
        graphics.drawString(font, selected, contentX + listWidth - font.width(selected),
                contentY, 0xFF6A6F80, false);

        drawList(graphics, mouseX, mouseY);
        drawHint(graphics);
        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Row> rows = rows();
        int visibleRows = Math.max(1, listHeight / ROW_HEIGHT);
        int maxScroll = Math.max(0, rows.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        graphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0x40000000);
        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        for (int i = 0; i < visibleRows; i++) {
            int index = scroll + i;
            if (index >= rows.size()) {
                break;
            }
            Row row = rows.get(index);
            int y = listY + i * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            boolean checked = draft.requiredQuests.contains(row.id());
            if (hovered) {
                graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, 0x20FFFFFF);
            }
            String prefix = checked ? "[\u2714] " : "[  ] ";
            String text = prefix + row.title();
            graphics.drawString(font, font.plainSubstrByWidth(text, listWidth - 8),
                    listX + 4, y + 6, row.missing() ? 0xFFFF6B6B : 0xFFD8D8DE, false);
        }
        graphics.disableScissor();
    }

    private void drawHint(GuiGraphics graphics) {
        int y = contentY + 134;
        List<FormattedCharSequence> lines = font.split(
                Component.translatable("storyquest.editor.conditions.hint"), rightWidth);
        for (FormattedCharSequence line : lines) {
            if (y > contentBottom - 10) {
                break;
            }
            graphics.drawString(font, line, rightX, y, 0xFF6A6F80, false);
            y += 10;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (mouseX < listX || mouseX >= listX + listWidth || mouseY < listY || mouseY >= listY + listHeight) {
            return false;
        }
        List<Row> rows = rows();
        int index = scroll + (int) ((mouseY - listY) / ROW_HEIGHT);
        if (index < 0 || index >= rows.size()) {
            return false;
        }
        String id = rows.get(index).id();
        if (draft.requiredQuests.contains(id)) {
            draft.requiredQuests.removeIf(id::equals);
        } else {
            draft.requiredQuests.add(id);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            int visibleRows = Math.max(1, listHeight / ROW_HEIGHT);
            int maxScroll = Math.max(0, rows().size() - visibleRows);
            scroll = Math.max(0, Math.min(maxScroll, scroll + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void done() {
        draft.branch = branchBox == null ? "" : branchBox.getValue();
        draft.requiredQuests.removeIf(id -> id.equals(target.id) || id.equals(draft.id));
        draft.sanitize();

        List<Quest> all = new ArrayList<>(parent.allQuests());
        all.removeIf(candidate -> candidate.id.equals(target.id) || candidate.id.equals(draft.id));
        all.add(draft);
        List<String> cycle = QuestConditions.findCycle(draft, all);
        if (!cycle.isEmpty()) {
            error = Component.translatable("storyquest.editor.conditions.cycle", String.join(" -> ", cycle));
            return;
        }

        target.copyConditionsFrom(draft);
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
    public boolean isPauseScreen() {
        return false;
    }
}
