package com.shinecreator.storyquest.client.gui;

import java.util.List;

import com.mojang.math.Axis;
import com.shinecreator.storyquest.client.ClientQuestCache;
import com.shinecreator.storyquest.client.QuestWaypoints;
import com.shinecreator.storyquest.client.StoryQuestClientConfig;
import com.shinecreator.storyquest.quest.MarkerIcon;
import com.shinecreator.storyquest.quest.Objective;
import com.shinecreator.storyquest.quest.Quest;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A compact compass bar at the top of the screen. It shows the nearest visit
 * objective of the tracked quest, a rotating arrow and the distance to it.
 */
public final class WaypointHud {
    private static final ResourceLocation ARROW =
            ResourceLocation.fromNamespaceAndPath("storyquest", "textures/marker/arrow.png");
    private static final int HEIGHT = 22;
    private static final int PADDING = 8;
    private static final int ARROW_SIZE = 12;
    private static final int ICON_SIZE = 12;
    private static final int GAP = 4;

    private WaypointHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        if (!StoryQuestClientConfig.WAYPOINTS_ENABLED.get() || !StoryQuestClientConfig.WAYPOINT_COMPASS.get()) {
            return;
        }
        Quest tracked = ClientQuestCache.trackedQuest();
        if (tracked == null) {
            return;
        }
        List<QuestWaypoints.Waypoint> waypoints = QuestWaypoints.active();
        if (waypoints.isEmpty()) {
            return;
        }

        String dimension = minecraft.level.dimension().location().toString();
        QuestWaypoints.Waypoint target = null;
        QuestWaypoints.Waypoint otherDimension = null;
        double nearest = Double.MAX_VALUE;
        for (QuestWaypoints.Waypoint waypoint : waypoints) {
            if (!waypoint.dimension().equals(dimension)) {
                if (otherDimension == null) {
                    otherDimension = waypoint;
                }
                continue;
            }
            Objective objective = waypoint.objective();
            double dx = objective.x - minecraft.player.getX();
            double dy = objective.y - minecraft.player.getY();
            double dz = objective.z - minecraft.player.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < nearest) {
                nearest = distance;
                target = waypoint;
            }
        }
        boolean crossDimension = target == null;
        if (target == null) {
            target = otherDimension;
        }
        if (target == null) {
            return;
        }

        Objective objective = target.objective();
        boolean inside = !crossDimension && objective.isInArea(dimension,
                minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());

        String label = minecraft.font.plainSubstrByWidth(target.label(), 150);
        Component distanceComponent;
        if (crossDimension) {
            distanceComponent = Component.translatable("storyquest.waypoint.other_dimension");
        } else if (inside) {
            distanceComponent = Component.translatable("storyquest.waypoint.here");
        } else {
            distanceComponent = Component.translatable("storyquest.waypoint.distance", Math.round(nearest));
        }
        String distanceText = distanceComponent.getString();

        int labelWidth = minecraft.font.width(label);
        int distanceWidth = minecraft.font.width(distanceText);
        int width = PADDING + ARROW_SIZE + GAP + ICON_SIZE + GAP + labelWidth + 12 + distanceWidth + PADDING;
        int x = (graphics.guiWidth() - width) / 2 + StoryQuestClientConfig.WAYPOINT_COMPASS_OFFSET_X.get();
        int y = StoryQuestClientConfig.WAYPOINT_COMPASS_OFFSET_Y.get();

        int rgb = target.color();
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        int accent = 0xFF000000 | rgb;
        int background = (StoryQuestClientConfig.panelColor() & 0x00FFFFFF) | 0xB4000000;

        graphics.fill(x, y, x + width, y + HEIGHT, background);
        graphics.fill(x, y, x + 3, y + HEIGHT, accent);

        if (!crossDimension) {
            Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
            float yaw = minecraft.gameRenderer.getMainCamera().getYRot();
            double dx = objective.x - cameraPos.x;
            double dz = objective.z - cameraPos.z;
            float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float relative = Mth.wrapDegrees(bearing - yaw);

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + PADDING + ARROW_SIZE / 2.0F, y + HEIGHT / 2.0F, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(relative));
            graphics.setColor(red, green, blue, 1.0F);
            graphics.blit(ARROW, -ARROW_SIZE / 2, -ARROW_SIZE / 2,
                    ARROW_SIZE, ARROW_SIZE, 0.0F, 0.0F, 32, 32, 32, 32);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }

        MarkerIcon icon = objective.markerIcon == null ? MarkerIcon.CHECK : objective.markerIcon;
        int iconX = x + PADDING + ARROW_SIZE + GAP;
        MarkerIcons.draw(graphics, icon, iconX, y + (HEIGHT - ICON_SIZE) / 2, ICON_SIZE, rgb, 1.0F);

        int textX = iconX + ICON_SIZE + GAP;
        graphics.drawString(minecraft.font, label, textX, y + 7, 0xFFE8E8EE, true);
        graphics.drawString(minecraft.font, distanceText,
                x + width - PADDING - distanceWidth, y + 7,
                inside ? 0xFF7CD992 : 0xFFB9BECC, false);
    }
}
