package com.shinecreator.storyquest.network;

import com.shinecreator.storyquest.StoryQuest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * All network payloads of the mod.
 * <p>
 * Server-to-client payloads are registered in {@code ClientPayloadHandler},
 * client-to-server payloads are registered in the mod entry point.
 */
public final class QuestPayloads {
    private QuestPayloads() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(StoryQuest.MODID, path);
    }

    /** Server to client: full quest list (filtered) + the player's progress. */
    public record Sync(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(id("sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Sync::data, Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: open the quest editor with all quest definitions. */
    public record OpenEditor(CompoundTag data) implements CustomPacketPayload {
        public static final Type<OpenEditor> TYPE = new Type<>(id("open_editor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenEditor> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, OpenEditor::data, OpenEditor::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: show a dialogue screen. Pages are separated by {@code |}. */
    public record OpenDialogue(String title, String speaker, String text) implements CustomPacketPayload {
        public static final Type<OpenDialogue> TYPE = new Type<>(id("open_dialogue"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialogue> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, OpenDialogue::title,
                ByteBufCodecs.STRING_UTF8, OpenDialogue::speaker,
                ByteBufCodecs.STRING_UTF8, OpenDialogue::text,
                OpenDialogue::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: open the journal (used by {@code /storyquest journal}). */
    public record OpenJournal() implements CustomPacketPayload {
        public static final Type<OpenJournal> TYPE = new Type<>(id("open_journal"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenJournal> STREAM_CODEC = StreamCodec.unit(new OpenJournal());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: request a fresh snapshot. */
    public record RequestSync() implements CustomPacketPayload {
        public static final Type<RequestSync> TYPE = new Type<>(id("request_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestSync> STREAM_CODEC = StreamCodec.unit(new RequestSync());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: ask to open the editor. Requires permission level 2. */
    public record RequestEditor() implements CustomPacketPayload {
        public static final Type<RequestEditor> TYPE = new Type<>(id("request_editor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestEditor> STREAM_CODEC = StreamCodec.unit(new RequestEditor());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: create or update a quest. Requires permission level 2. */
    public record SaveQuest(CompoundTag quest) implements CustomPacketPayload {
        public static final Type<SaveQuest> TYPE = new Type<>(id("save_quest"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveQuest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, SaveQuest::quest, SaveQuest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: delete a quest. Requires permission level 2. */
    public record DeleteQuest(String questId) implements CustomPacketPayload {
        public static final Type<DeleteQuest> TYPE = new Type<>(id("delete_quest"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DeleteQuest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, DeleteQuest::questId, DeleteQuest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: start a quest that allows manual start. */
    public record StartQuest(String questId) implements CustomPacketPayload {
        public static final Type<StartQuest> TYPE = new Type<>(id("start_quest"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StartQuest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, StartQuest::questId, StartQuest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: pin or unpin the HUD tracker. An empty id unpins. */
    public record TrackQuest(String questId) implements CustomPacketPayload {
        public static final Type<TrackQuest> TYPE = new Type<>(id("track_quest"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TrackQuest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TrackQuest::questId, TrackQuest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
