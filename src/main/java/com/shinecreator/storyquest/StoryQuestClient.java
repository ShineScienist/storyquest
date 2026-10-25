package com.shinecreator.storyquest;

import com.shinecreator.storyquest.client.ClientModEvents;
import com.shinecreator.storyquest.client.StoryQuestClientConfig;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only entry point: config screen, client config, keybinds and HUD layer.
 */
@Mod(value = StoryQuest.MODID, dist = Dist.CLIENT)
public class StoryQuestClient {
    public StoryQuestClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(ClientModEvents::registerKeys);
        modEventBus.addListener(ClientModEvents::registerGuiLayers);
        container.registerConfig(ModConfig.Type.CLIENT, StoryQuestClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
