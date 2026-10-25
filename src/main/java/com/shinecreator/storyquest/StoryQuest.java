package com.shinecreator.storyquest;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.shinecreator.storyquest.client.ClientPayloadHandler;
import com.shinecreator.storyquest.command.StoryQuestCommand;
import com.shinecreator.storyquest.config.StoryQuestConfig;
import com.shinecreator.storyquest.network.QuestPayloads;
import com.shinecreator.storyquest.network.ServerPayloadHandler;
import com.shinecreator.storyquest.quest.QuestManager;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * StoryQuest — a story-driven quest system for Minecraft maps.
 * <p>
 * Quests are created in game (key {@code K}), shown in the journal (key {@code J})
 * and controlled from commands/command blocks.
 */
@Mod(StoryQuest.MODID)
public class StoryQuest {
    public static final String MODID = "storyquest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StoryQuest(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(StoryQuest::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.COMMON, StoryQuestConfig.SPEC);
        NeoForge.EVENT_BUS.register(this);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(QuestPayloads.RequestSync.TYPE, QuestPayloads.RequestSync.STREAM_CODEC,
                ServerPayloadHandler::handleRequestSync);
        registrar.playToServer(QuestPayloads.RequestEditor.TYPE, QuestPayloads.RequestEditor.STREAM_CODEC,
                ServerPayloadHandler::handleRequestEditor);
        registrar.playToServer(QuestPayloads.SaveQuest.TYPE, QuestPayloads.SaveQuest.STREAM_CODEC,
                ServerPayloadHandler::handleSaveQuest);
        registrar.playToServer(QuestPayloads.DeleteQuest.TYPE, QuestPayloads.DeleteQuest.STREAM_CODEC,
                ServerPayloadHandler::handleDeleteQuest);
        registrar.playToServer(QuestPayloads.StartQuest.TYPE, QuestPayloads.StartQuest.STREAM_CODEC,
                ServerPayloadHandler::handleStartQuest);
        registrar.playToServer(QuestPayloads.TrackQuest.TYPE, QuestPayloads.TrackQuest.STREAM_CODEC,
                ServerPayloadHandler::handleTrackQuest);

        // The server needs the codecs to encode these payloads. Client handlers are only referenced on the client,
        // so a dedicated server never tries to load client-only GUI classes.
        if (FMLEnvironment.dist.isClient()) {
            registerClientHandlers(registrar);
        } else {
            registrar.playToClient(QuestPayloads.Sync.TYPE, QuestPayloads.Sync.STREAM_CODEC,
                    StoryQuest::ignorePayload);
            registrar.playToClient(QuestPayloads.OpenEditor.TYPE, QuestPayloads.OpenEditor.STREAM_CODEC,
                    StoryQuest::ignorePayload);
            registrar.playToClient(QuestPayloads.OpenDialogue.TYPE, QuestPayloads.OpenDialogue.STREAM_CODEC,
                    StoryQuest::ignorePayload);
            registrar.playToClient(QuestPayloads.OpenJournal.TYPE, QuestPayloads.OpenJournal.STREAM_CODEC,
                    StoryQuest::ignorePayload);
        }
    }

    private static void registerClientHandlers(PayloadRegistrar registrar) {
        registrar.playToClient(QuestPayloads.Sync.TYPE, QuestPayloads.Sync.STREAM_CODEC,
                ClientPayloadHandler::handleSync);
        registrar.playToClient(QuestPayloads.OpenEditor.TYPE, QuestPayloads.OpenEditor.STREAM_CODEC,
                ClientPayloadHandler::handleOpenEditor);
        registrar.playToClient(QuestPayloads.OpenDialogue.TYPE, QuestPayloads.OpenDialogue.STREAM_CODEC,
                ClientPayloadHandler::handleOpenDialogue);
        registrar.playToClient(QuestPayloads.OpenJournal.TYPE, QuestPayloads.OpenJournal.STREAM_CODEC,
                ClientPayloadHandler::handleOpenJournal);
    }

    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void ignorePayload(
            T payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        // Server-bound side of a client-bound payload; never invoked.
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        StoryQuestCommand.register(event);
    }

    @SubscribeEvent
    public void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        LOGGER.info("StoryQuest loaded: {} quest(s) in this world",
                com.shinecreator.storyquest.quest.QuestSavedData.get(event.getServer()).quests().size());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        QuestManager.tick(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestManager.autoStart(player);
            QuestManager.sync(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestManager.sync(player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestManager.sync(player);
        }
    }
}
