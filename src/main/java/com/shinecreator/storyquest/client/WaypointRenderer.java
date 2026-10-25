package com.shinecreator.storyquest.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shinecreator.storyquest.StoryQuest;
import com.shinecreator.storyquest.quest.MarkerIcon;
import com.shinecreator.storyquest.quest.Objective;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws floating marker icons and vertical light beams for visit objectives
 * of the tracked quest. Purely client-side: the server only stores the marker settings.
 */
@EventBusSubscriber(modid = StoryQuest.MODID, value = Dist.CLIENT)
public final class WaypointRenderer {
    private static final int MAX_MARKERS = 8;
    private static final double ICON_SCALE_PER_BLOCK = 0.022D;
    private static final float BEAM_WIDTH = 0.08F;

    private record Target(QuestWaypoints.Waypoint waypoint, double distance) {}

    private WaypointRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        if (!StoryQuestClientConfig.WAYPOINTS_ENABLED.get() || event.getPoseStack() == null) {
            return;
        }

        List<QuestWaypoints.Waypoint> waypoints = QuestWaypoints.active();
        if (waypoints.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        String dimension = minecraft.level.dimension().location().toString();
        int maxDistance = StoryQuestClientConfig.WAYPOINT_MAX_DISTANCE.get();

        List<Target> targets = new ArrayList<>();
        for (QuestWaypoints.Waypoint waypoint : waypoints) {
            if (!waypoint.dimension().equals(dimension)) {
                continue;
            }
            Objective objective = waypoint.objective();
            double dx = objective.x - cameraPos.x;
            double dy = objective.y + 1.5D - cameraPos.y;
            double dz = objective.z - cameraPos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (maxDistance > 0 && distance > maxDistance) {
                continue;
            }
            if (distance < 2.0D) {
                continue;
            }
            targets.add(new Target(waypoint, distance));
        }
        if (targets.isEmpty()) {
            return;
        }
        targets.sort(Comparator.comparingDouble(Target::distance));
        if (targets.size() > MAX_MARKERS) {
            targets = targets.subList(0, MAX_MARKERS);
        }

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long gameTime = minecraft.level.getGameTime();

        if (StoryQuestClientConfig.WAYPOINT_BEAMS.get()) {
            renderBeams(pose, buffers, cameraPos, targets, maxDistance);
        }
        renderIcons(pose, buffers, camera, cameraPos, targets, maxDistance,
                minecraft, partialTick, gameTime);
    }

    private static void renderBeams(PoseStack pose, MultiBufferSource.BufferSource buffers,
                                    Vec3 cameraPos, List<Target> targets, int maxDistance) {
        float height = (float) (double) StoryQuestClientConfig.WAYPOINT_BEAM_HEIGHT.get();
        VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());
        boolean empty = true;

        for (Target target : targets) {
            Objective objective = target.waypoint().objective();
            if (!objective.markerBeam) {
                continue;
            }
            float alpha = fade(target.distance(), maxDistance) * 0.30F;
            if (alpha <= 0.01F) {
                continue;
            }
            int rgb = target.waypoint().color();
            float red = ((rgb >> 16) & 0xFF) / 255.0F;
            float green = ((rgb >> 8) & 0xFF) / 255.0F;
            float blue = (rgb & 0xFF) / 255.0F;

            pose.pushPose();
            pose.translate(objective.x - cameraPos.x, objective.y + 0.05D - cameraPos.y,
                    objective.z - cameraPos.z);
            PoseStack.Pose entry = pose.last();

            addBeamQuad(consumer, entry, red, green, blue, alpha,
                    -BEAM_WIDTH, 0.0F, 0.0F, BEAM_WIDTH, 0.0F, 0.0F,
                    BEAM_WIDTH, height, 0.0F, -BEAM_WIDTH, height, 0.0F);
            addBeamQuad(consumer, entry, red, green, blue, alpha,
                    0.0F, 0.0F, -BEAM_WIDTH, 0.0F, 0.0F, BEAM_WIDTH,
                    0.0F, height, BEAM_WIDTH, 0.0F, height, -BEAM_WIDTH);
            pose.popPose();
            empty = false;
        }

        if (!empty) {
            buffers.endBatch(RenderType.debugQuads());
        }
    }

    private static void addBeamQuad(VertexConsumer consumer, PoseStack.Pose entry,
                                    float red, float green, float blue, float alpha,
                                    float x0, float y0, float z0,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3) {
        consumer.addVertex(entry, x0, y0, z0).setColor(red, green, blue, alpha);
        consumer.addVertex(entry, x1, y1, z1).setColor(red, green, blue, alpha);
        consumer.addVertex(entry, x2, y2, z2).setColor(red, green, blue, alpha);
        consumer.addVertex(entry, x3, y3, z3).setColor(red, green, blue, alpha);
    }

    private static void renderIcons(PoseStack pose, MultiBufferSource.BufferSource buffers,
                                    Camera camera, Vec3 cameraPos, List<Target> targets,
                                    int maxDistance, Minecraft minecraft,
                                    float partialTick, long gameTime) {
        double iconScale = StoryQuestClientConfig.WAYPOINT_ICON_SCALE.get();

        for (Target target : targets) {
            Objective objective = target.waypoint().objective();
            if (!objective.markerVisible) {
                continue;
            }
            float alpha = fade(target.distance(), maxDistance);
            if (alpha <= 0.01F) {
                continue;
            }

            int rgb = target.waypoint().color();
            float red = ((rgb >> 16) & 0xFF) / 255.0F;
            float green = ((rgb >> 8) & 0xFF) / 255.0F;
            float blue = (rgb & 0xFF) / 255.0F;

            float size = (float) (target.distance() * ICON_SCALE_PER_BLOCK * iconScale);
            size = Mth.clamp(size, 0.15F, 8.0F);
            double bob = Math.sin((gameTime + partialTick) * 0.08D + objective.x * 0.7D) * 0.12D;

            pose.pushPose();
            pose.translate(objective.x - cameraPos.x,
                    objective.y + 2.2D + bob - cameraPos.y,
                    objective.z - cameraPos.z);
            pose.mulPose(camera.rotation());
            PoseStack.Pose entry = pose.last();

            MarkerIcon icon = objective.markerIcon == null ? MarkerIcon.CHECK : objective.markerIcon;
            minecraft.getTextureManager().getTexture(icon.texture());
            RenderType renderType = RenderType.textSeeThrough(icon.texture());
            VertexConsumer consumer = buffers.getBuffer(renderType);

            consumer.addVertex(entry, -size, -size, 0.0F)
                    .setColor(red, green, blue, alpha).setUv(0.0F, 1.0F)
                    .setLight(LightTexture.FULL_BRIGHT);
            consumer.addVertex(entry, size, -size, 0.0F)
                    .setColor(red, green, blue, alpha).setUv(1.0F, 1.0F)
                    .setLight(LightTexture.FULL_BRIGHT);
            consumer.addVertex(entry, size, size, 0.0F)
                    .setColor(red, green, blue, alpha).setUv(1.0F, 0.0F)
                    .setLight(LightTexture.FULL_BRIGHT);
            consumer.addVertex(entry, -size, size, 0.0F)
                    .setColor(red, green, blue, alpha).setUv(0.0F, 0.0F)
                    .setLight(LightTexture.FULL_BRIGHT);

            buffers.endBatch(renderType);
            pose.popPose();
        }
    }

    private static float fade(double distance, int maxDistance) {
        float alpha = 1.0F;
        if (maxDistance > 0) {
            alpha *= Mth.clamp((float) ((maxDistance - distance) / 64.0D), 0.0F, 1.0F);
        }
        alpha *= Mth.clamp((float) ((distance - 2.0D) / 4.0D), 0.0F, 1.0F);
        return alpha;
    }
}
