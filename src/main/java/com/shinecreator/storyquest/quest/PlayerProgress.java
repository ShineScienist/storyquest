package com.shinecreator.storyquest.quest;

import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Per-player quest progress, stored in NeoForge persistent entity data,
 * so it is saved together with the player and survives restarts.
 */
public final class PlayerProgress extends QuestProgressData {
    public static final String KEY = "storyquest_progress";
    private static final String TRACKED = "Tracked";

    private PlayerProgress(CompoundTag tag) {
        super(tag);
    }

    public static PlayerProgress of(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted;
        if (persistent.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        } else {
            persisted = new CompoundTag();
            persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        }
        if (!persisted.contains(KEY, Tag.TAG_COMPOUND)) {
            persisted.put(KEY, new CompoundTag());
        }
        return new PlayerProgress(persisted.getCompound(KEY));
    }

    public boolean isTracked(String questId) {
        return questId != null && questId.equals(tracked());
    }

    public String tracked() {
        return tag.getString(TRACKED);
    }

    @Override
    public void startQuest(Quest quest) {
        if (isCompleted(quest.id)) {
            return;
        }
        super.startQuest(quest);
        if (isActive(quest.id) && tracked().isBlank()) {
            setTracked(quest.id);
        }
    }

    @Override
    public void completeQuest(Quest quest) {
        boolean wasTracked = isTracked(quest.id);
        super.completeQuest(quest);
        if (wasTracked) {
            Set<String> active = active();
            setTracked(active.isEmpty() ? "" : active.iterator().next());
        }
    }

    public void setTracked(String questId) {
        String value = questId == null ? "" : questId;
        if (!value.isBlank() && !isActive(value)) {
            return;
        }
        setTrackedRaw(value);
    }

    /**
     * Tracking is personal even for shared quests, so this bypasses the
     * personal-active check used by {@link #setTracked(String)}.
     */
    public void setTrackedRaw(String questId) {
        tag.putString(TRACKED, questId == null ? "" : questId);
    }

    @Override
    public void forgetQuest(String questId) {
        boolean wasTracked = isTracked(questId);
        super.forgetQuest(questId);
        if (wasTracked) {
            Set<String> active = active();
            setTrackedRaw(active.isEmpty() ? "" : active.iterator().next());
        }
    }

    public void clearTrackedIf(String questId) {
        if (!isTracked(questId)) {
            return;
        }
        Set<String> active = active();
        setTrackedRaw(active.isEmpty() ? "" : active.iterator().next());
    }

    @Override
    public CompoundTag snapshot() {
        CompoundTag out = super.snapshot();
        out.put(TRACKED, StringTag.valueOf(tracked()));
        return out;
    }
}
