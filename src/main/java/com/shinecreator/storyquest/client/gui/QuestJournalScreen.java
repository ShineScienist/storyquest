package com.shinecreator.storyquest.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.shinecreator.storyquest.client.ClientQuestCache;
import com.shinecreator.storyquest.client.StoryQuestClientConfig;
import com.shinecreator.storyquest.client.gui.theme.QuestButton;
import com.shinecreator.storyquest.client.gui.theme.QuestTheme;
import com.shinecreator.storyquest.client.gui.theme.QuestUiRenderer;
import com.shinecreator.storyquest.network.QuestPayloads;
import com.shinecreator.storyquest.quest.Objective;
import com.shinecreator.storyquest.quest.Quest;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The quest journal: chapters on the left, quest details on the right.
 */
public class QuestJournalScreen extends Screen {
    private static final int ROW_HEIGHT = 18;
    private static final int HEADER_HEIGHT = 28;

    private final List<Row> rows = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int detailX;
    private int detailWidth;
    private int bottom;
    private int scroll;
    private String selectedId;
    private int seenVersion = -1;
    private QuestButton trackButton;
    private QuestButton acceptButton;

    private record Row(String questId, String chapter, boolean header) {}

    public QuestJournalScreen() {
        super(Component.translatable("storyquest.journal.title"));
    }

    @Override
    protected void init() {
        QuestTheme theme = QuestTheme.current();
        int padding = theme.padding();
        panelWidth = Math.min(560, width - 20);
        panelHeight = Math.min(336, height - 20);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        bottom = panelY + panelHeight - padding - 18;

        listWidth = Math.max(140, Math.min(190, panelWidth / 3));
        listX = panelX + padding;
        listY = panelY + 38;
        listHeight = Math.max(40, bottom - 8 - listY);

        int spineGap = theme.bookStyle() ? 24 : 16;
        detailX = listX + listWidth + spineGap;
        detailWidth = panelX + panelWidth - padding - detailX;

        seenVersion = ClientQuestCache.version();
        rebuildRows();

        acceptButton = addRenderableWidget(QuestButton.primary(
                Component.translatable("storyquest.journal.accept"), button -> accept(),
                panelX + panelWidth - padding - 70 - 6 - 78 - 6 - 78, bottom, 78, 18));
        trackButton = addRenderableWidget(QuestButton.normal(
                Component.translatable("storyquest.journal.track"), button -> toggleTrack(),
                panelX + panelWidth - padding - 70 - 6 - 78, bottom, 78, 18));
        addRenderableWidget(QuestButton.normal(
                Component.translatable("storyquest.button.close"), button -> onClose(),
                panelX + panelWidth - padding - 70, bottom, 70, 18));

        int themeX = panelX + padding + font.width(title) + 10;
        addRenderableWidget(QuestButton.normal(themeLabel(), button -> cycleTheme(),
                themeX, panelY + 5, 112, 18));
    }

    private Component themeLabel() {
        return Component.translatable("storyquest.journal.theme",
                Component.translatable(QuestTheme.current().translationKey()));
    }

    private void cycleTheme() {
        QuestTheme[] values = QuestTheme.values();
        QuestTheme next = values[(QuestTheme.current().ordinal() + 1) % values.length];
        StoryQuestClientConfig.THEME.set(next);
        StoryQuestClientConfig.THEME.save();
        rebuildWidgets();
    }

    private void rebuildRows() {
        rows.clear();
        String lastChapter = null;
        for (Quest quest : ClientQuestCache.quests()) {
            String chapter = quest.chapter == null ? "" : quest.chapter.trim();
            if (!chapter.equals(lastChapter)) {
                rows.add(new Row("", chapter, true));
                lastChapter = chapter;
            }
            rows.add(new Row(quest.id, "", false));
        }
        if (selectedId != null && ClientQuestCache.get(selectedId) == null) {
            selectedId = null;
        }
        if (selectedId == null) {
            String tracked = ClientQuestCache.tracked();
            if (!tracked.isBlank() && ClientQuestCache.get(tracked) != null) {
                selectedId = tracked;
            } else {
                for (Row row : rows) {
                    if (!row.header()) {
                        selectedId = row.questId();
                        break;
                    }
                }
            }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * ROW_HEIGHT - (listHeight - 4));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (seenVersion != ClientQuestCache.version()) {
            seenVersion = ClientQuestCache.version();
            rebuildRows();
        }

        renderBackground(graphics, mouseX, mouseY, partialTick);
        QuestTheme theme = QuestTheme.current();
        int padding = theme.padding();
        QuestUiRenderer.drawPanel(graphics, panelX, panelY, panelWidth, panelHeight);

        int innerRight = panelX + panelWidth - padding;
        graphics.drawString(font, title, panelX + padding, panelY + 9, theme.text(), false);

        int count = (int) rows.stream().filter(row -> !row.header()).count();
        String countText = Component.translatable("storyquest.journal.quest_count", count).getString();
        int decorationWidth = theme.decoration().width();
        graphics.drawString(font, countText, innerRight - decorationWidth - 8 - font.width(countText),
                panelY + 10, theme.textMuted(), false);

        QuestUiRenderer.drawDecoration(graphics, innerRight - decorationWidth, panelY + 6);

        QuestUiRenderer.drawDivider(graphics, panelX + padding, panelY + HEADER_HEIGHT, panelWidth - padding * 2);

        drawQuestList(graphics, mouseX, mouseY);
        drawDetails(graphics);

        Quest selected = ClientQuestCache.get(selectedId);
        boolean active = selected != null && ClientQuestCache.isActive(selected.id);
        boolean tracked = selected != null && selected.id.equals(ClientQuestCache.tracked());
        trackButton.active = active;
        trackButton.setMessage(Component.translatable(tracked ? "storyquest.journal.untrack" : "storyquest.journal.track"));
        acceptButton.visible = selected != null && !active && !ClientQuestCache.isCompleted(selected.id)
                && selected.manualStart && ClientQuestCache.canStart(selected);

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawQuestList(GuiGraphics graphics, int mouseX, int mouseY) {
        QuestTheme theme = QuestTheme.current();
        QuestUiRenderer.drawInset(graphics, listX - 4, listY - 4, listWidth + 8, listHeight + 8);

        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        int y = listY - scroll;
        for (Row row : rows) {
            if (y + ROW_HEIGHT >= listY && y <= listY + listHeight) {
                if (row.header()) {
                    String label = row.chapter().isBlank()
                            ? Component.translatable("storyquest.journal.no_chapter").getString()
                            : row.chapter();
                    graphics.drawString(font, font.plainSubstrByWidth(label, listWidth - 16), listX + 8, y + 4,
                            theme.textMuted(), false);
                    graphics.fill(listX + 8, y + 15, listX + listWidth - 8, y + 16,
                            QuestUiRenderer.alpha(theme.textMuted(), 0.45F));
                } else {
                    Quest quest = ClientQuestCache.get(row.questId());
                    if (quest != null) {
                        boolean selected = quest.id.equals(selectedId);
                        boolean hovered = mouseX >= listX && mouseX < listX + listWidth
                                && mouseY >= y && mouseY < y + ROW_HEIGHT;
                        if (selected) {
                            graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, theme.selection());
                            graphics.fill(listX, y, listX + 3, y + ROW_HEIGHT, theme.accent());
                        } else if (hovered) {
                            graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT,
                                    QuestUiRenderer.alpha(theme.accent(), 0.14F));
                        }
                        String icon;
                        if (ClientQuestCache.isCompleted(quest.id)) {
                            icon = "check";
                        } else if (ClientQuestCache.isActive(quest.id)) {
                            icon = "star";
                        } else if (!ClientQuestCache.canStart(quest)) {
                            icon = "lock";
                        } else {
                            icon = "scroll";
                        }
                        QuestUiRenderer.drawIcon(graphics, icon, listX + 7, y + 3, 12);
                        graphics.drawString(font, font.plainSubstrByWidth(quest.title, listWidth - 30),
                                listX + 23, y + 5, theme.text(), false);
                    }
                }
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();

        if (maxScroll() > 0) {
            QuestUiRenderer.drawScrollbar(graphics, listX + listWidth - 7, listY + 2, listHeight - 4,
                    scroll, maxScroll(), listHeight - 4);
        }

        if (theme.bookStyle()) {
            QuestUiRenderer.drawSpine(graphics, listX + listWidth + 8, listY, listHeight);
        }
    }

    private void drawDetails(GuiGraphics graphics) {
        QuestTheme theme = QuestTheme.current();
        Quest quest = ClientQuestCache.get(selectedId);
        if (quest == null) {
            graphics.drawString(font, Component.translatable("storyquest.journal.empty"), detailX, panelY + 44,
                    theme.textMuted(), false);
            graphics.drawString(font, Component.translatable("storyquest.journal.empty_hint"), detailX, panelY + 60,
                    theme.textMuted(), false);
            return;
        }

        graphics.drawString(font, font.plainSubstrByWidth(quest.title, detailWidth), detailX, panelY + 38,
                theme.text(), false);

        boolean branchLocked = ClientQuestCache.branchLocked(quest);
        String state;
        int stateColor;
        if (ClientQuestCache.isCompleted(quest.id)) {
            state = Component.translatable("storyquest.journal.state.completed").getString();
            stateColor = theme.positive();
        } else if (ClientQuestCache.isActive(quest.id)) {
            state = Component.translatable("storyquest.journal.state.active").getString();
            stateColor = theme.accent();
        } else if (branchLocked) {
            state = Component.translatable("storyquest.journal.state.branch_locked").getString();
            stateColor = theme.warning();
        } else if (!ClientQuestCache.requirementsMet(quest)) {
            state = Component.translatable("storyquest.journal.state.locked").getString();
            stateColor = theme.negative();
        } else {
            state = Component.translatable("storyquest.journal.state.available").getString();
            stateColor = theme.textMuted();
        }
        if (quest.scope == Quest.Scope.SHARED) {
            state += "  \u00B7  " + Component.translatable("storyquest.journal.shared").getString();
        }
        String chapter = quest.chapter.isBlank()
                ? Component.translatable("storyquest.journal.no_chapter").getString()
                : quest.chapter;
        graphics.drawString(font, font.plainSubstrByWidth(chapter + "  \u00B7  " + state, detailWidth),
                detailX, panelY + 53, stateColor, false);
        QuestUiRenderer.drawDivider(graphics, detailX, panelY + 66, detailWidth);

        int y = panelY + 74;
        if (!quest.requiredQuests.isEmpty()) {
            String requirementsKey = quest.requireAll
                    ? "storyquest.journal.requirements"
                    : "storyquest.journal.requirements_any";
            graphics.drawString(font, Component.translatable(requirementsKey), detailX, y, theme.textMuted(), false);
            y += 13;
            int shown = Math.min(quest.requiredQuests.size(), 4);
            for (int i = 0; i < shown; i++) {
                String requiredId = quest.requiredQuests.get(i);
                Quest required = ClientQuestCache.get(requiredId);
                String name = required == null
                        ? Component.translatable("storyquest.journal.requirement_unknown").getString()
                        : required.title;
                boolean done = ClientQuestCache.isCompleted(requiredId);
                y = drawSymbolLine(graphics, name, detailX, y, done, theme);
            }
            if (quest.requiredQuests.size() > shown) {
                graphics.drawString(font, "+" + (quest.requiredQuests.size() - shown), detailX, y,
                        theme.textMuted(), false);
                y += 11;
            }
            y += 4;
        }

        if (branchLocked) {
            Quest winner = ClientQuestCache.branchWinner(quest);
            String line = Component.translatable("storyquest.journal.branch_chosen",
                    winner == null ? "???" : winner.title).getString();
            graphics.drawString(font, font.plainSubstrByWidth(line, detailWidth), detailX, y,
                    theme.warning(), false);
            y += 13;
        }

        if (!quest.objectives.isEmpty()) {
            graphics.drawString(font, Component.translatable("storyquest.journal.objectives"), detailX, y,
                    theme.textMuted(), false);
            y += 13;
            int shown = Math.min(quest.objectives.size(), 7);
            for (int i = 0; i < shown; i++) {
                Objective objective = quest.objectives.get(i);
                boolean done = ClientQuestCache.isObjectiveDone(quest.id, i);
                y = drawSymbolLine(graphics, objective.displayText(), detailX, y, done, theme);
            }
            if (quest.objectives.size() > shown) {
                graphics.drawString(font, "+" + (quest.objectives.size() - shown), detailX, y,
                        theme.textMuted(), false);
                y += 11;
            }
            y += 4;
        }

        if (!quest.description.isBlank()) {
            List<net.minecraft.util.FormattedCharSequence> lines =
                    font.split(Component.literal(quest.description), detailWidth);
            int maxLines = Math.max(0, (bottom - 8 - y) / 10);
            for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
                graphics.drawString(font, lines.get(i), detailX, y, theme.textDim(), false);
                y += 10;
            }
        }
    }

    private int drawSymbolLine(GuiGraphics graphics, String text, int x, int y, boolean done, QuestTheme theme) {
        if (done) {
            QuestUiRenderer.drawIcon(graphics, "check", x, y - 1, 9);
        } else {
            graphics.fill(x + 2, y + 4, x + 4, y + 6, theme.textMuted());
        }
        graphics.drawString(font, font.plainSubstrByWidth(text, detailWidth - 12), x + 12, y,
                done ? theme.positive() : theme.textDim(), false);
        return y + 11;
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
        int index = (int) ((mouseY - listY + scroll) / ROW_HEIGHT);
        if (index >= 0 && index < rows.size()) {
            Row row = rows.get(index);
            if (!row.header()) {
                selectedId = row.questId();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(scrollY * ROW_HEIGHT)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void toggleTrack() {
        Quest quest = ClientQuestCache.get(selectedId);
        if (quest == null || !ClientQuestCache.isActive(quest.id)) {
            return;
        }
        String target = quest.id.equals(ClientQuestCache.tracked()) ? "" : quest.id;
        PacketDistributor.sendToServer(new QuestPayloads.TrackQuest(target));
    }

    private void accept() {
        Quest quest = ClientQuestCache.get(selectedId);
        if (quest == null) {
            return;
        }
        PacketDistributor.sendToServer(new QuestPayloads.StartQuest(quest.id));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
