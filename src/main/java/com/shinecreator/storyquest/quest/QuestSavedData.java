package com.shinecreator.storyquest.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Quest definitions, saved in the overworld data storage ({@code data/storyquest_quests.dat}).
 * Copying the world therefore copies all quests.
 */
public final class QuestSavedData extends SavedData {
    public static final String DATA_NAME = "storyquest_quests";

    private final Map<String, Quest> quests = new LinkedHashMap<>();

    public static QuestSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(QuestSavedData::new, QuestSavedData::load),
                DATA_NAME);
    }

    public static QuestSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        QuestSavedData data = new QuestSavedData();
        ListTag list = tag.getList("Quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Quest quest = Quest.load(list.getCompound(i));
            if (!quest.id.isBlank()) {
                data.quests.put(quest.id, quest);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.merge(toTag());
        return tag;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Quest quest : quests.values()) {
            list.add(quest.save());
        }
        tag.put("Quests", list);
        return tag;
    }

    public Collection<Quest> quests() {
        return quests.values();
    }

    public List<Quest> sortedQuests() {
        List<Quest> list = new ArrayList<>(quests.values());
        list.sort(Comparator
                .comparing((Quest quest) -> quest.chapter.toLowerCase(Locale.ROOT))
                .thenComparingInt(quest -> quest.order)
                .thenComparing(quest -> quest.title.toLowerCase(Locale.ROOT)));
        return list;
    }

    public Quest get(String id) {
        return id == null ? null : quests.get(id);
    }

    public void put(Quest quest) {
        quest.sanitize();
        quests.put(quest.id, quest);
        setDirty();
    }

    public boolean remove(String id) {
        boolean removed = quests.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void clear() {
        quests.clear();
        setDirty();
    }

    public List<String> ids() {
        return new ArrayList<>(quests.keySet());
    }

    public Quest createQuest() {
        int index = 1;
        while (quests.containsKey("quest_" + index)) {
            index++;
        }
        Quest quest = new Quest();
        quest.id = "quest_" + index;
        quest.title = "New quest";
        quest.chapter = "";
        quest.order = quests.size();
        return quest;
    }
}
