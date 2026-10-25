package com.shinecreator.storyquest.network;

import com.shinecreator.storyquest.quest.QuestManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handles client-to-server payloads. Runs on the server thread.
 */
public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void handleRequestSync(QuestPayloads.RequestSync payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                QuestManager.sync(player);
            }
        });
    }

    public static void handleRequestEditor(QuestPayloads.RequestEditor payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.hasPermissions(2)) {
                    QuestManager.openEditor(player);
                } else {
                    player.displayClientMessage(Component.translatable("storyquest.message.no_permission")
                            .withStyle(ChatFormatting.RED), true);
                }
            }
        });
    }

    public static void handleSaveQuest(QuestPayloads.SaveQuest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.hasPermissions(2)) {
                QuestManager.saveQuest(player, payload.quest());
            }
        });
    }

    public static void handleDeleteQuest(QuestPayloads.DeleteQuest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.hasPermissions(2)) {
                QuestManager.deleteQuest(player, payload.questId());
            }
        });
    }

    public static void handleStartQuest(QuestPayloads.StartQuest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                QuestManager.startFromJournal(player, payload.questId());
            }
        });
    }

    public static void handleTrackQuest(QuestPayloads.TrackQuest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                QuestManager.setTracked(player, payload.questId());
            }
        });
    }
}
