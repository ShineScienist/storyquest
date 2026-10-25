package com.shinecreator.storyquest.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.shinecreator.storyquest.quest.Quest;
import com.shinecreator.storyquest.quest.QuestConditions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Client-side snapshot of all quests, the local player's progress and the
 * world-shared co-op progress. Updated by {@code ClientPayloadHandler}.
 */
public final class ClientQuestCache {
    private static final Map<String, Quest> QUESTS = new LinkedHashMap<>();
    private static final Set<String> ACTIVE = new LinkedHashSet<>();
    private static final Set<String> COMPLETED = new LinkedHashSet<>();
    private static final Map<String, Set<Integer>> OBJECTIVES = new LinkedHashMap<>();
    private static final Set<String> SHARED_ACTIVE = new LinkedHashSet<>();
    private static final Set<String> SHARED_COMPLETED = new LinkedHashSet<>();
    private static final Map<String, Set<Integer>> SHARED_OBJECTIVES = new LinkedHashMap<>();
    private static String tracked = "";
    private static int version;

    private ClientQuestCache() {}

    public static void update(CompoundTag tag) {
        QUESTS.clear();
        ACTIVE.clear();
        COMPLETED.clear();
        OBJECTIVES.clear();
        SHARED_ACTIVE.clear();
        SHARED_COMPLETED.clear();
        SHARED_OBJECTIVES.clear();
        tracked = "";

        ListTag questList = tag.getList("Quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < questList.size(); i++) {
            Quest quest = Quest.load(questList.getCompound(i));
            if (!quest.id.isBlank()) {
                QUESTS.put(quest.id, quest);
            }
        }

        CompoundTag progress = tag.getCompound("Progress");
        readProgress(progress, ACTIVE, COMPLETED, OBJECTIVES);
        tracked = progress.getString("Tracked");

        CompoundTag shared = tag.getCompound("Shared");
        readProgress(shared, SHARED_ACTIVE, SHARED_COMPLETED, SHARED_OBJECTIVES);
        version++;
    }

    private static void readProgress(CompoundTag progress, Set<String> active,
            Set<String> completed, Map<String, Set<Integer>> objectives) {
        readStrings(progress.getList("Active", Tag.TAG_STRING), active);
        readStrings(progress.getList("Completed", Tag.TAG_STRING), completed);

        CompoundTag objectiveTag = progress.getCompound("Objectives");
        for (String questId : objectiveTag.getAllKeys()) {
            ListTag done = objectiveTag.getList(questId, Tag.TAG_INT);
            Set<Integer> indexes = new LinkedHashSet<>();
            for (int i = 0; i < done.size(); i++) {
                indexes.add(done.getInt(i));
            }
            objectives.put(questId, indexes);
        }
    }

    public static void clear() {
        QUESTS.clear();
        ACTIVE.clear();
        COMPLETED.clear();
        OBJECTIVES.clear();
        SHARED_ACTIVE.clear();
        SHARED_COMPLETED.clear();
        SHARED_OBJECTIVES.clear();
        tracked = "";
        version++;
    }

    public static int version() {
        return version;
    }

    public static List<Quest> quests() {
        List<Quest> list = new ArrayList<>(QUESTS.values());
        list.sort(Comparator
                .comparing((Quest quest) -> quest.chapter.toLowerCase(Locale.ROOT))
                .thenComparingInt(quest -> quest.order)
                .thenComparing(quest -> quest.title.toLowerCase(Locale.ROOT)));
        return list;
    }

    public static Quest get(String questId) {
        return questId == null ? null : QUESTS.get(questId);
    }

    public static boolean hasQuests() {
        return !QUESTS.isEmpty();
    }

    public static boolean isActive(String questId) {
        Quest quest = get(questId);
        if (quest != null && quest.scope == Quest.Scope.SHARED) {
            return SHARED_ACTIVE.contains(questId);
        }
        return ACTIVE.contains(questId);
    }

    public static boolean isCompleted(String questId) {
        Quest quest = get(questId);
        if (quest != null && quest.scope == Quest.Scope.SHARED) {
            return SHARED_COMPLETED.contains(questId);
        }
        return COMPLETED.contains(questId);
    }

    public static boolean isObjectiveDone(String questId, int index) {
        Quest quest = get(questId);
        Set<Integer> done = quest != null && quest.scope == Quest.Scope.SHARED
                ? SHARED_OBJECTIVES.get(questId)
                : OBJECTIVES.get(questId);
        return done != null && done.contains(index);
    }

    public static String tracked() {
        return tracked;
    }

    public static Quest trackedQuest() {
        Quest quest = get(tracked);
        return quest != null && isActive(quest.id) ? quest : null;
    }

    public static boolean requirementsMet(Quest quest) {
        return QuestConditions.requirementsMet(quest, ClientQuestCache::requirementCompleted);
    }

    public static boolean branchLocked(Quest quest) {
        return QuestConditions.branchLocked(quest,
                id -> activeOrCompleted(quest, id), sameScopeQuests(quest));
    }

    public static Quest branchWinner(Quest quest) {
        return QuestConditions.branchWinner(quest,
                id -> activeOrCompleted(quest, id), sameScopeQuests(quest));
    }

    public static boolean canStart(Quest quest) {
        return QuestConditions.canStart(quest, ClientQuestCache::requirementCompleted,
                id -> activeOrCompleted(quest, id), sameScopeQuests(quest));
    }

    public static List<String> missingRequirements(Quest quest) {
        return QuestConditions.missingRequirements(quest, ClientQuestCache::requirementCompleted);
    }

    private static boolean requirementCompleted(String requiredId) {
        Quest required = get(requiredId);
        if (required != null && required.scope == Quest.Scope.SHARED) {
            return SHARED_COMPLETED.contains(requiredId);
        }
        return COMPLETED.contains(requiredId);
    }

    private static boolean activeOrCompleted(Quest target, String questId) {
        if (target.scope == Quest.Scope.SHARED) {
            return SHARED_ACTIVE.contains(questId) || SHARED_COMPLETED.contains(questId);
        }
        return ACTIVE.contains(questId) || COMPLETED.contains(questId);
    }

    private static List<Quest> sameScopeQuests(Quest target) {
        List<Quest> list = new ArrayList<>();
        for (Quest quest : QUESTS.values()) {
            if (quest.scope == target.scope) {
                list.add(quest);
            }
        }
        return list;
    }

    private static void readStrings(ListTag list, Set<String> target) {
        for (int i = 0; i < list.size(); i++) {
            String value = list.getString(i);
            if (!value.isBlank()) {
                target.add(value);
            }
        }
    }
}
