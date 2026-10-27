package com.shinecreator.storyquest.client.gui;

import java.util.List;

import com.shinecreator.storyquest.client.ClientQuestCache;
import com.shinecreator.storyquest.client.StoryQuestClientConfig;
import com.shinecreator.storyquest.client.gui.theme.QuestTheme;
import com.shinecreator.storyquest.client.gui.theme.QuestUiRenderer;
import com.shinecreator.storyquest.quest.Objective;
import com.shinecreator.storyquest.quest.Quest;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The in-game quest tracker drawn in a configurable screen corner.
 */
public final class QuestHud {
    private QuestHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (!StoryQuestClientConfig.SHOW_HUD.get()) {
            return;
        }
        Quest quest = ClientQuestCache.trackedQuest();
        if (quest == null) {
            return;
        }

        List<Objective> objectives = quest.objectives;
        int maxObjectives = StoryQuestClientConfig.HUD_MAX_OBJECTIVES.get();
        int shown = Math.min(objectives.size(), maxObjectives);
        int overflow = objectives.size() - shown;

        int maxTextWidth = 190;
        String title = minecraft.font.plainSubstrByWidth(quest.title, maxTextWidth);
        int contentWidth = minecraft.font.width(title);

        String[] lines = new String[shown];
        boolean[] doneFlags = new boolean[shown];
        for (int i = 0; i < shown; i++) {
            Objective objective = objectives.get(i);
            boolean done = ClientQuestCache.isObjectiveDone(quest.id, i);
            String prefix = done ? "\u2714 " : "\u2022 ";
            String text = prefix + objective.displayText();
            lines[i] = minecraft.font.plainSubstrByWidth(text, maxTextWidth);
            doneFlags[i] = done;
            contentWidth = Math.max(contentWidth, minecraft.font.width(lines[i]));
        }
        if (overflow > 0) {
            contentWidth = Math.max(contentWidth, minecraft.font.width("+" + overflow));
        }

        int panelWidth = Math.max(96, contentWidth + 16);
        int panelHeight = 16 + shown * 10 + (overflow > 0 ? 10 : 0) + 4;

        double scale = StoryQuestClientConfig.HUD_SCALE.get();
        int scaledWidth = (int) Math.ceil(panelWidth * scale);
        int scaledHeight = (int) Math.ceil(panelHeight * scale);
        int offsetX = StoryQuestClientConfig.HUD_OFFSET_X.get();
        int offsetY = StoryQuestClientConfig.HUD_OFFSET_Y.get();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        boolean left = switch (StoryQuestClientConfig.HUD_ANCHOR.get()) {
            case TOP_LEFT, BOTTOM_LEFT -> true;
            default -> false;
        };
        boolean top = switch (StoryQuestClientConfig.HUD_ANCHOR.get()) {
            case TOP_LEFT, TOP_RIGHT -> true;
            default -> false;
        };
        int x = left ? offsetX : screenWidth - offsetX - scaledWidth;
        int y = top ? offsetY : screenHeight - offsetY - scaledHeight;

        int alphaValue = (int) Math.round(StoryQuestClientConfig.HUD_BACKGROUND_OPACITY.get() * 255.0D);
        float backgroundAlpha = alphaValue / 255.0F;
        QuestTheme theme = QuestTheme.current();

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale((float) scale, (float) scale, 1.0F);

        graphics.setColor(1.0F, 1.0F, 1.0F, backgroundAlpha);
        QuestUiRenderer.drawHudPanel(graphics, 0, 0, panelWidth, panelHeight);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.fill(0, 0, 3, panelHeight, QuestUiRenderer.alpha(theme.accent(), backgroundAlpha));

        int titleColor = theme.bookStyle() ? theme.hudText() : theme.accent();
        graphics.drawString(minecraft.font, title, 8, 5, titleColor, false);

        int lineY = 17;
        for (int i = 0; i < shown; i++) {
            int color = doneFlags[i] ? theme.hudPositive() : theme.hudText();
            graphics.drawString(minecraft.font, lines[i], 8, lineY, color, false);
            lineY += 10;
        }
        if (overflow > 0) {
            graphics.drawString(minecraft.font, "+" + overflow, 8, lineY, theme.hudMuted(), false);
        }

        pose.popPose();
    }
}
