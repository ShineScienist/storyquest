package com.shinecreator.storyquest.client;

import com.shinecreator.storyquest.StoryQuest;
import com.shinecreator.storyquest.client.gui.QuestHud;
import com.shinecreator.storyquest.client.gui.WaypointHud;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Mod-bus registrations that only exist on the physical client.
 */
public final class ClientModEvents {
    private ClientModEvents() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientEvents.OPEN_JOURNAL);
        event.register(ClientEvents.OPEN_EDITOR);
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(StoryQuest.MODID, "quest_tracker"),
                QuestHud::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(StoryQuest.MODID, "waypoint_compass"),
                WaypointHud::render);
    }
}
