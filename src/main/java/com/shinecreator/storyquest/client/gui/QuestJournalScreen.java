package com.shinecreator.storyquest.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.shinecreator.storyquest.client.ClientQuestCache;
import com.shinecreator.storyquest.client.StoryQuestClientConfig;
import com.shinecreator.storyquest.network.QuestPayloads;
import com.shinecreator.storyquest.quest.Objective;
import com.shinecreator.storyquest.quest.Quest;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The quest journal: chapters on the left, quest details on the right.
 */
public class QuestJournalScreen extends Screen {
    private static final int ROW_HEIGHT = 18;

    private final List<Row> rows = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;
    private int scroll;
    private String selectedId;
    private int seenVersion = -1;
    private Button trackButton;
    private Button acceptButton;

    private record Row(String questId, String chapter, boolean header) {}

    public QuestJournalScreen() {
        super(Component.translatable("storyquest.journal.title"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(540, width - 20);
        panelHeight = Math.min(320, height - 20);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        listWidth = Math.max(120, Math.min(180, panelWidth / 3));

        seenVersion = ClientQuestCache.version();
        rebuildRows();

        int bottom = panelY + panelHeight - 26;
        addRenderableWidget(Button.builder(Component.translatable("storyquest.button.close"), button -> onClose())
                .bounds(panelX + panelWidth - 78, bottom, 70, 18).build());
        trackButton = addRenderableWidget(Button.builder(Component.translatable("storyquest.journal.track"), button -> toggleTrack())
                .bounds(panelX + panelWidth - 78 - 96, bottom, 90, 18).build());
        acceptButton = addRenderableWidget(Button.builder(Component.translatable("storyquest.journal.accept"), button -> accept())
                .bounds(panelX + panelWidth - 78 - 96 - 96, bottom, 90, 18).build());
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
        int listHeight = panelHeight - 78;
        return Math.max(0, rows.size() * ROW_HEIGHT - listHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (seenVersion != ClientQuestCache.version()) {
            seenVersion = ClientQuestCache.version();
            rebuildRows();
        }

        renderBackground(graphics, mouseX, mouseY, partialTick);
        int accent = StoryQuestClientConfig.accentColor();
        int panel = StoryQuestClientConfig.panelColor();
        int opaquePanel = (panel & 0x00FFFFFF) | 0xF0101218;

        graphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, accent);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, opaquePanel);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 24, 0x30FFFFFF);
        graphics.fill(panelX, panelY + 24, panelX + panelWidth, panelY + 25, accent);

        graphics.drawString(font, title, panelX + 10, panelY + 8, accent, true);
        String count = Integer.toString(rows.size() == 0 ? 0 : (int) rows.stream().filter(row -> !row.header()).count());
        graphics.drawString(font, count, panelX + panelWidth - 10 - font.width(count), panelY + 8, 0xFF8A8FA0, false);

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
        int listX = panelX + 8;
        int listY = panelY + 34;
        int listHeight = panelHeight - 78;

        graphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0x40000000);
        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        int y = listY - scroll;
        for (Row row : rows) {
            if (y + ROW_HEIGHT >= listY && y <= listY + listHeight) {
                if (row.header()) {
                    String label = row.chapter().isBlank()
                            ? Component.translatable("storyquest.journal.no_chapter").getString()
                            : row.chapter();
                    graphics.drawString(font, font.plainSubstrByWidth(label, listWidth - 8), listX + 4, y + 5, 0xFF8A8FA0, false);
                    graphics.fill(listX + 4, y + 16, listX + listWidth - 4, y + 17, 0x40FFFFFF);
                } else {
                    Quest quest = ClientQuestCache.get(row.questId());
                    if (quest != null) {
                        boolean selected = quest.id.equals(selectedId);
                        boolean hovered = mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
                        if (selected) {
                            graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, 0x50FFFFFF);
                            graphics.fill(listX, y, listX + 2, y + ROW_HEIGHT, StoryQuestClientConfig.accentColor());
                        } else if (hovered) {
                            graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, 0x20FFFFFF);
                        }
                        String symbol;
                        int color;
                        if (ClientQuestCache.isCompleted(quest.id)) {
                            symbol = "\u2714";
                            color = 0xFF7CD992;
                        } else if (ClientQuestCache.isActive(quest.id)) {
                            symbol = "\u25CF";
                            color = StoryQuestClientConfig.accentColor();
                        } else if (!ClientQuestCache.canStart(quest)) {
                            symbol = "\u2716";
                            color = 0xFFFF6B6B;
                        } else {
                            symbol = "\u25CB";
                            color = 0xFF9A9FB0;
                        }
                        graphics.drawString(font, symbol, listX + 6, y + 5, color, false);
                        graphics.drawString(font, font.plainSubstrByWidth(quest.title, listWidth - 24),
                                listX + 18, y + 5, selected ? 0xFFFFFFFF : 0xFFD8D8DE, false);
                    }
                }
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
    }

    private void drawDetails(GuiGraphics graphics) {
        int detailX = panelX + listWidth + 20;
        int detailWidth = panelX + panelWidth - 10 - detailX;
        Quest quest = ClientQuestCache.get(selectedId);
        if (quest == null) {
            graphics.drawString(font, Component.translatable("storyquest.journal.empty"), detailX, panelY + 44, 0xFF8A8FA0, false);
            graphics.drawString(font, Component.translatable("storyquest.journal.empty_hint"), detailX, panelY + 60, 0xFF6A6F80, false);
            return;
        }

        graphics.drawString(font, font.plainSubstrByWidth(quest.title, detailWidth), detailX, panelY + 38,
                StoryQuestClientConfig.accentColor(), true);

        boolean branchLocked = ClientQuestCache.branchLocked(quest);
        String state;
        int stateColor;
        if (ClientQuestCache.isCompleted(quest.id)) {
            state = Component.translatable("storyquest.journal.state.completed").getString();
            stateColor = 0xFF7CD992;
        } else if (ClientQuestCache.isActive(quest.id)) {
            state = Component.translatable("storyquest.journal.state.active").getString();
            stateColor = StoryQuestClientConfig.accentColor();
        } else if (branchLocked) {
            state = Component.translatable("storyquest.journal.state.branch_locked").getString();
            stateColor = 0xFFFF9B6B;
        } else if (!ClientQuestCache.requirementsMet(quest)) {
            state = Component.translatable("storyquest.journal.state.locked").getString();
            stateColor = 0xFFFF6B6B;
        } else {
            state = Component.translatable("storyquest.journal.state.available").getString();
            stateColor = 0xFF9A9FB0;
        }
        if (quest.scope == Quest.Scope.SHARED) {
            state += "  \u00B7  " + Component.translatable("storyquest.journal.shared").getString();
        }
        String chapter = quest.chapter.isBlank()
                ? Component.translatable("storyquest.journal.no_chapter").getString()
                : quest.chapter;
        graphics.drawString(font, font.plainSubstrByWidth(chapter + "  \u00B7  " + state, detailWidth),
                detailX, panelY + 53, stateColor, false);
        graphics.fill(detailX, panelY + 66, detailX + detailWidth, panelY + 67, 0x40FFFFFF);

        int y = panelY + 74;
        if (!quest.requiredQuests.isEmpty()) {
            String requirementsKey = quest.requireAll
                    ? "storyquest.journal.requirements"
                    : "storyquest.journal.requirements_any";
            graphics.drawString(font, Component.translatable(requirementsKey), detailX, y, 0xFF8A8FA0, false);
            y += 13;
            int shown = Math.min(quest.requiredQuests.size(), 4);
            for (int i = 0; i < shown; i++) {
                String requiredId = quest.requiredQuests.get(i);
                Quest required = ClientQuestCache.get(requiredId);
                String name = required == null
                        ? Component.translatable("storyquest.journal.requirement_unknown").getString()
                        : required.title;
                boolean done = ClientQuestCache.isCompleted(requiredId);
                String line = (done ? "\u2714 " : "\u2022 ") + name;
                graphics.drawString(font, font.plainSubstrByWidth(line, detailWidth), detailX, y,
                        done ? 0xFF7CD992 : 0xFFD8D8DE, false);
                y += 11;
            }
            if (quest.requiredQuests.size() > shown) {
                graphics.drawString(font, "+" + (quest.requiredQuests.size() - shown), detailX, y, 0xFF8A8FA0, false);
                y += 11;
            }
            y += 4;
        }

        if (branchLocked) {
            Quest winner = ClientQuestCache.branchWinner(quest);
            String line = Component.translatable("storyquest.journal.branch_chosen",
                    winner == null ? "???" : winner.title).getString();
            graphics.drawString(font, font.plainSubstrByWidth(line, detailWidth), detailX, y, 0xFFFF9B6B, false);
            y += 13;
        }

        if (!quest.objectives.isEmpty()) {
            graphics.drawString(font, Component.translatable("storyquest.journal.objectives"), detailX, y, 0xFF8A8FA0, false);
            y += 13;
            int shown = Math.min(quest.objectives.size(), 7);
            for (int i = 0; i < shown; i++) {
                Objective objective = quest.objectives.get(i);
                boolean done = ClientQuestCache.isObjectiveDone(quest.id, i);
                String line = (done ? "\u2714 " : "\u2022 ") + objective.displayText();
                graphics.drawString(font, font.plainSubstrByWidth(line, detailWidth), detailX, y,
                        done ? 0xFF7CD992 : 0xFFD8D8DE, false);
                y += 11;
            }
            if (quest.objectives.size() > shown) {
                graphics.drawString(font, "+" + (quest.objectives.size() - shown), detailX, y, 0xFF8A8FA0, false);
                y += 11;
            }
            y += 4;
        }

        if (!quest.description.isBlank()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(quest.description), detailWidth);
            int maxLines = Math.max(0, (panelY + panelHeight - 56 - y) / 10);
            for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
                graphics.drawString(font, lines.get(i), detailX, y, 0xFFB8BCC8, false);
                y += 10;
            }
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
        int listX = panelX + 8;
        int listY = panelY + 34;
        int listHeight = panelHeight - 78;
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
        int listX = panelX + 8;
        int listY = panelY + 34;
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + panelHeight - 78) {
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
