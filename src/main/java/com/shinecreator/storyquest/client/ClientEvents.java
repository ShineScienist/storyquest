package com.shinecreator.storyquest.client;

import com.shinecreator.storyquest.StoryQuest;
import com.shinecreator.storyquest.client.gui.QuestJournalScreen;
import com.shinecreator.storyquest.network.QuestPayloads;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Client input and session lifecycle.
 */
@EventBusSubscriber(modid = StoryQuest.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    public static final KeyMapping OPEN_JOURNAL = new KeyMapping(
            "key.storyquest.journal", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J, "key.categories.storyquest");

    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.storyquest.editor", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.storyquest");

    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (OPEN_JOURNAL.consumeClick()) {
            PacketDistributor.sendToServer(new QuestPayloads.RequestSync());
            minecraft.setScreen(new QuestJournalScreen());
        }
        while (OPEN_EDITOR.consumeClick()) {
            PacketDistributor.sendToServer(new QuestPayloads.RequestEditor());
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientQuestCache.clear();
    }
}
