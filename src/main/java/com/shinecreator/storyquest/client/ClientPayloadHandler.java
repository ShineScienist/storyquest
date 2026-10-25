package com.shinecreator.storyquest.client;

import com.shinecreator.storyquest.client.gui.DialogueScreen;
import com.shinecreator.storyquest.client.gui.QuestEditorScreen;
import com.shinecreator.storyquest.client.gui.QuestJournalScreen;
import com.shinecreator.storyquest.network.QuestPayloads;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side handlers for server-to-client payloads. Loaded only on the physical client.
 */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleSync(QuestPayloads.Sync payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestCache.update(payload.data()));
    }

    public static void handleOpenEditor(QuestPayloads.OpenEditor payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new QuestEditorScreen(payload.data())));
    }

    public static void handleOpenDialogue(QuestPayloads.OpenDialogue payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new DialogueScreen(payload.title(), payload.speaker(), payload.text())));
    }

    public static void handleOpenJournal(QuestPayloads.OpenJournal payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(Minecraft.getInstance().screen instanceof QuestJournalScreen)) {
                Minecraft.getInstance().setScreen(new QuestJournalScreen());
            }
        });
    }
}
