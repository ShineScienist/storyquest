package com.shinecreator.storyquest.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-level progress for shared co-op quests, saved in the overworld data storage.
 */
public final class SharedQuestProgress extends SavedData {
    public static final String DATA_NAME = "storyquest_shared_progress";

    private final QuestProgressData progress;

    public SharedQuestProgress() {
        this(new CompoundTag());
    }

    private SharedQuestProgress(CompoundTag tag) {
        this.progress = new QuestProgressData(tag);
    }

    public static SharedQuestProgress get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SharedQuestProgress::new, SharedQuestProgress::load),
                DATA_NAME);
    }

    public static SharedQuestProgress load(CompoundTag tag, HolderLookup.Provider registries) {
        return new SharedQuestProgress(tag);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (String key : new java.util.ArrayList<>(tag.getAllKeys())) {
            tag.remove(key);
        }
        tag.merge(progress.snapshot());
        return tag;
    }

    public QuestProgressData progress() {
        return progress;
    }

    public boolean isActive(String questId) {
        return progress.isActive(questId);
    }

    public boolean isCompleted(String questId) {
        return progress.isCompleted(questId);
    }

    public boolean isObjectiveDone(String questId, int index) {
        return progress.isObjectiveDone(questId, index);
    }

    public boolean allObjectivesDone(Quest quest) {
        return progress.allObjectivesDone(quest);
    }

    public void startQuest(Quest quest) {
        progress.startQuest(quest);
        setDirty();
    }

    public void completeQuest(Quest quest) {
        progress.completeQuest(quest);
        setDirty();
    }

    public void markObjective(String questId, int index) {
        progress.markObjective(questId, index);
        setDirty();
    }

    public void forgetQuest(String questId) {
        progress.forgetQuest(questId);
        setDirty();
    }

    public void reset() {
        progress.reset();
        setDirty();
    }

    public CompoundTag snapshot() {
        return progress.snapshot();
    }
}
