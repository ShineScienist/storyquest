package com.shinecreator.storyquest.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.shinecreator.storyquest.client.gui.theme.QuestButton;
import com.shinecreator.storyquest.client.gui.theme.QuestTheme;
import com.shinecreator.storyquest.client.gui.theme.QuestUiRenderer;
import com.shinecreator.storyquest.network.QuestPayloads;
import com.shinecreator.storyquest.quest.Quest;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Operator-only quest list. Quests are stored in the world, no config files needed.
 */
public class QuestEditorScreen extends Screen {
    private static final int PAGE_SIZE = 8;
    private static final int ROW_HEIGHT = 24;

    private final List<Quest> quests = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int page;
    private String selectedId;
    private long lastClickTime;
    private String lastClickId;

    public QuestEditorScreen(CompoundTag data) {
        super(Component.translatable("storyquest.editor.title"));
        ListTag list = data.getList("Quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            quests.add(Quest.load(list.getCompound(i)));
        }
        sortQuests();
    }

    private void sortQuests() {
        quests.sort(Comparator
                .comparing((Quest quest) -> quest.chapter.toLowerCase(Locale.ROOT))
                .thenComparingInt(quest -> quest.order)
                .thenComparing(quest -> quest.title.toLowerCase(Locale.ROOT)));
    }

    @Override
    protected void init() {
        QuestTheme theme = QuestTheme.current();
        int padding = theme.padding();
        panelWidth = Math.min(460, width - 16);
        panelHeight = Math.min(310, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        page = Math.max(0, Math.min(page, pageCount() - 1));
        if (selectedId == null || find(selectedId) == null) {
            selectedId = quests.isEmpty() ? null : quests.get(0).id;
        }

        int bottom = panelY + panelHeight - padding - 18;
        int left = panelX + padding;
        addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.button.new"),
                button -> newQuest(), left, bottom, 64, 18));
        addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.button.edit"),
                button -> editSelected(), left + 70, bottom, 76, 18));
        addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.button.delete"),
                button -> deleteSelected(), left + 152, bottom, 68, 18));
        addRenderableWidget(QuestButton.normal(Component.translatable("storyquest.button.close"),
                button -> onClose(), panelX + panelWidth - padding - 64, bottom, 64, 18));

        addRenderableWidget(QuestButton.normal(Component.literal("\u25C0"), button -> changePage(-1),
                panelX + panelWidth - padding - 46, panelY + 28, 20, 16));
        addRenderableWidget(QuestButton.normal(Component.literal("\u25B6"), button -> changePage(1),
                panelX + panelWidth - padding - 24, panelY + 28, 20, 16));
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(pageCount() - 1, page + delta));
    }

    private int pageCount() {
        return Math.max(1, (quests.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void newQuest() {
        int index = 1;
        while (find("quest_" + index) != null) {
            index++;
        }
        Quest quest = new Quest();
        quest.id = "quest_" + index;
        quest.title = Component.translatable("storyquest.editor.new_quest").getString();
        quest.order = quests.size();
        openForm(quest);
    }

    private void editSelected() {
        Quest quest = find(selectedId);
        if (quest != null) {
            openForm(quest);
        }
    }

    private void openForm(Quest quest) {
        if (minecraft != null) {
            minecraft.setScreen(new QuestFormScreen(this, quest.copy()));
        }
    }

    private void deleteSelected() {
        Quest quest = find(selectedId);
        if (quest == null) {
            return;
        }
        PacketDistributor.sendToServer(new QuestPayloads.DeleteQuest(quest.id));
        quests.remove(quest);
        selectedId = quests.isEmpty() ? null : quests.get(0).id;
        page = Math.max(0, Math.min(page, pageCount() - 1));
        rebuildWidgets();
    }

    public void upsert(Quest quest) {
        Quest existing = find(quest.id);
        if (existing == null) {
            quests.add(quest.copy());
        } else {
            int index = quests.indexOf(existing);
            quests.set(index, quest.copy());
        }
        selectedId = quest.id;
        sortQuests();
        int index = quests.indexOf(find(quest.id));
        if (index >= 0) {
            page = index / PAGE_SIZE;
        } else {
            page = Math.max(0, Math.min(page, pageCount() - 1));
        }
    }

    /**
     * Removes a quest from the local list only. Used when a quest is renamed:
     * the old entry is dropped before the renamed one is added.
     */
    public void removeLocal(String id) {
        Quest existing = find(id);
        if (existing == null) {
            return;
        }
        quests.remove(existing);
        if (id.equals(selectedId)) {
            selectedId = quests.isEmpty() ? null : quests.get(0).id;
        }
        page = Math.max(0, Math.min(page, pageCount() - 1));
    }

    public boolean contains(String id) {
        return find(id) != null;
    }

    public List<Quest> allQuests() {
        return new ArrayList<>(quests);
    }

    private Quest find(String id) {
        if (id == null) {
            return null;
        }
        for (Quest quest : quests) {
            if (quest.id.equals(id)) {
                return quest;
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        QuestTheme theme = QuestTheme.current();
        int padding = theme.padding();
        QuestUiRenderer.drawPanel(graphics, panelX, panelY, panelWidth, panelHeight);

        graphics.drawString(font, title, panelX + padding, panelY + 9, theme.text(), false);
        String pages = (page + 1) + " / " + pageCount() + "   (" + quests.size() + ")";
        graphics.drawString(font, pages, panelX + panelWidth - padding - 56 - font.width(pages),
                panelY + 9, theme.textMuted(), false);
        QuestUiRenderer.drawDivider(graphics, panelX + padding, panelY + 24, panelWidth - padding * 2);

        int listX = panelX + padding;
        int listY = panelY + 50;
        int listWidth = panelWidth - padding * 2;
        int start = page * PAGE_SIZE;
        int end = Math.min(quests.size(), start + PAGE_SIZE);

        QuestUiRenderer.drawInset(graphics, listX - 4, listY - 6, listWidth + 8, PAGE_SIZE * ROW_HEIGHT + 10);

        if (quests.isEmpty()) {
            graphics.drawString(font, Component.translatable("storyquest.editor.empty"), listX + 4, listY + 8,
                    theme.textMuted(), false);
            graphics.drawString(font, Component.translatable("storyquest.editor.empty_hint"), listX + 4, listY + 22,
                    theme.textMuted(), false);
        }

        for (int i = start; i < end; i++) {
            Quest quest = quests.get(i);
            int y = listY + (i - start) * ROW_HEIGHT;
            boolean selected = quest.id.equals(selectedId);
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
            if (selected) {
                graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT - 2, theme.selection());
                graphics.fill(listX, y, listX + 3, y + ROW_HEIGHT - 2, theme.accent());
            } else if (hovered) {
                graphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT - 2,
                        QuestUiRenderer.alpha(theme.accent(), 0.14F));
            }
            graphics.drawString(font, font.plainSubstrByWidth(quest.id, 150), listX + 8, y + 4,
                    theme.textMuted(), false);
            graphics.drawString(font, font.plainSubstrByWidth(quest.title, listWidth - 210), listX + 150, y + 4,
                    theme.text(), false);
            String meta = quest.chapter.isBlank() ? "" : quest.chapter;
            meta += "  " + Component.translatable("storyquest.editor.objectives_short", quest.objectives.size()).getString();
            String metaText = font.plainSubstrByWidth(meta, 120);
            graphics.drawString(font, metaText, listX + listWidth - 8 - font.width(metaText), y + 13,
                    theme.textMuted(), false);
        }

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
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
        int listX = panelX + QuestTheme.current().padding();
        int listY = panelY + 50;
        int listWidth = panelWidth - QuestTheme.current().padding() * 2;
        if (mouseX < listX || mouseX >= listX + listWidth || mouseY < listY) {
            return false;
        }
        int index = page * PAGE_SIZE + (int) ((mouseY - listY) / ROW_HEIGHT);
        if (index < 0 || index >= quests.size()) {
            return false;
        }
        Quest quest = quests.get(index);
        long now = Util.getMillis();
        if (quest.id.equals(lastClickId) && now - lastClickTime < 300L) {
            openForm(quest);
        } else {
            selectedId = quest.id;
            lastClickId = quest.id;
            lastClickTime = now;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        changePage(scrollY > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
