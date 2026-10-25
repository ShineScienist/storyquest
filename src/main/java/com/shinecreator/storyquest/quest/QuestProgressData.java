package com.shinecreator.storyquest.quest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Shared NBT layout for personal and world-shared quest progress.
 * The player-specific tracking field lives in {@link PlayerProgress}.
 */
public class QuestProgressData {
    protected static final String ACTIVE = "Active";
    protected static final String COMPLETED = "Completed";
    protected static final String OBJECTIVES = "Objectives";

    protected final CompoundTag tag;

    public QuestProgressData(CompoundTag tag) {
        this.tag = tag;
    }

    public Set<String> active() {
        return readStrings(ACTIVE);
    }

    public Set<String> completed() {
        return readStrings(COMPLETED);
    }

    public boolean isActive(String questId) {
        return active().contains(questId);
    }

    public boolean isCompleted(String questId) {
        return completed().contains(questId);
    }

    public Set<Integer> completedObjectives(String questId) {
        Set<Integer> result = new LinkedHashSet<>();
        CompoundTag objectives = tag.getCompound(OBJECTIVES);
        ListTag list = objectives.getList(questId, Tag.TAG_INT);
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getInt(i));
        }
        return result;
    }

    public boolean isObjectiveDone(String questId, int index) {
        return completedObjectives(questId).contains(index);
    }

    public boolean allObjectivesDone(Quest quest) {
        Set<Integer> done = completedObjectives(quest.id);
        for (int i = 0; i < quest.objectives.size(); i++) {
            if (!done.contains(i)) {
                return false;
            }
        }
        return true;
    }

    public void startQuest(Quest quest) {
        if (isCompleted(quest.id)) {
            return;
        }
        Set<String> active = active();
        if (active.add(quest.id)) {
            writeStrings(ACTIVE, active);
        }
    }

    public void completeQuest(Quest quest) {
        Set<String> active = active();
        if (active.remove(quest.id)) {
            writeStrings(ACTIVE, active);
        } else if (isCompleted(quest.id)) {
            return;
        }
        Set<String> completed = completed();
        completed.add(quest.id);
        writeStrings(COMPLETED, completed);
        clearObjectives(quest.id);
    }

    public void markObjective(String questId, int index) {
        Set<Integer> done = completedObjectives(questId);
        if (!done.add(index)) {
            return;
        }
        CompoundTag objectives = tag.contains(OBJECTIVES, Tag.TAG_COMPOUND)
                ? tag.getCompound(OBJECTIVES)
                : new CompoundTag();
        ListTag list = new ListTag();
        for (int value : done) {
            list.add(IntTag.valueOf(value));
        }
        objectives.put(questId, list);
        tag.put(OBJECTIVES, objectives);
    }

    public void clearObjectives(String questId) {
        if (!tag.contains(OBJECTIVES, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag objectives = tag.getCompound(OBJECTIVES);
        if (objectives.contains(questId)) {
            objectives.remove(questId);
            tag.put(OBJECTIVES, objectives);
        }
    }

    public void forgetQuest(String questId) {
        Set<String> active = active();
        if (active.remove(questId)) {
            writeStrings(ACTIVE, active);
        }
        Set<String> completed = completed();
        if (completed.remove(questId)) {
            writeStrings(COMPLETED, completed);
        }
        clearObjectives(questId);
    }

    public void reset() {
        List<String> keys = new java.util.ArrayList<>(tag.getAllKeys());
        for (String key : keys) {
            tag.remove(key);
        }
    }

    /**
     * Copy of the progress, safe to send over the network.
     */
    public CompoundTag snapshot() {
        CompoundTag out = new CompoundTag();
        out.put(ACTIVE, copyStringList(ACTIVE));
        out.put(COMPLETED, copyStringList(COMPLETED));

        CompoundTag objectivesCopy = new CompoundTag();
        CompoundTag objectives = tag.getCompound(OBJECTIVES);
        for (String key : objectives.getAllKeys()) {
            objectivesCopy.put(key, objectives.getList(key, Tag.TAG_INT).copy());
        }
        out.put(OBJECTIVES, objectivesCopy);
        return out;
    }

    protected Set<String> readStrings(String key) {
        Set<String> result = new LinkedHashSet<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String value = list.getString(i);
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    protected void writeStrings(String key, Set<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        tag.put(key, list);
    }

    protected ListTag copyStringList(String key) {
        ListTag copy = new ListTag();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            copy.add(StringTag.valueOf(list.getString(i)));
        }
        return copy;
    }
}
