package com.shinecreator.storyquest.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Pure quest condition helpers shared by the server, the journal and the editor.
 * The callers supply progress predicates, so the same logic works for personal
 * and shared progress.
 */
public final class QuestConditions {
    private QuestConditions() {}

    public static boolean requirementsMet(Quest quest, Predicate<String> completed) {
        if (quest == null || quest.requiredQuests.isEmpty()) {
            return true;
        }
        if (quest.requireAll) {
            for (String requiredId : quest.requiredQuests) {
                if (!completed.test(requiredId)) {
                    return false;
                }
            }
            return true;
        }
        for (String requiredId : quest.requiredQuests) {
            if (completed.test(requiredId)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> missingRequirements(Quest quest, Predicate<String> completed) {
        List<String> missing = new ArrayList<>();
        if (quest == null) {
            return missing;
        }
        for (String requiredId : quest.requiredQuests) {
            if (!completed.test(requiredId)) {
                missing.add(requiredId);
            }
        }
        return missing;
    }

    public static boolean branchLocked(Quest quest, Predicate<String> activeOrCompleted, Collection<Quest> all) {
        return branchWinner(quest, activeOrCompleted, all) != null;
    }

    public static Quest branchWinner(Quest quest, Predicate<String> activeOrCompleted, Collection<Quest> all) {
        if (quest == null || quest.branch == null || quest.branch.isBlank()) {
            return null;
        }
        for (Quest other : all) {
            if (other == null || other.id.equals(quest.id)) {
                continue;
            }
            if (!quest.branch.equals(other.branch)) {
                continue;
            }
            if (activeOrCompleted.test(other.id)) {
                return other;
            }
        }
        return null;
    }

    public static boolean canStart(Quest quest, Predicate<String> completed,
            Predicate<String> activeOrCompleted, Collection<Quest> all) {
        if (quest == null || activeOrCompleted.test(quest.id)) {
            return false;
        }
        return requirementsMet(quest, completed) && !branchLocked(quest, activeOrCompleted, all);
    }

    /**
     * Finds a prerequisite cycle reachable from this quest, including the quest itself.
     * A missing prerequisite is not a cycle; it is reported separately for the editor.
     *
     * @return the cycle path, or an empty list if there is no cycle
     */
    public static List<String> findCycle(Quest quest, Collection<Quest> all) {
        if (quest == null || quest.id == null || quest.id.isBlank()) {
            return List.of();
        }
        Map<String, Quest> byId = new HashMap<>();
        for (Quest candidate : all) {
            if (candidate != null && candidate.id != null && !candidate.id.isBlank()) {
                byId.put(candidate.id, candidate);
            }
        }
        byId.put(quest.id, quest);

        Map<String, Integer> state = new HashMap<>();
        List<String> stack = new ArrayList<>();
        List<String> cycle = visit(quest.id, byId, state, stack);
        return cycle == null ? List.of() : cycle;
    }

    private static List<String> visit(String id, Map<String, Quest> byId,
            Map<String, Integer> state, List<String> stack) {
        int current = state.getOrDefault(id, 0);
        if (current == 1) {
            int start = stack.indexOf(id);
            List<String> cycle = new ArrayList<>(stack.subList(Math.max(0, start), stack.size()));
            cycle.add(id);
            return cycle;
        }
        if (current == 2) {
            return null;
        }
        Quest quest = byId.get(id);
        if (quest == null) {
            return null;
        }

        state.put(id, 1);
        stack.add(id);
        for (String requiredId : quest.requiredQuests) {
            if (requiredId == null || requiredId.isBlank()) {
                continue;
            }
            if (!byId.containsKey(requiredId)) {
                continue;
            }
            List<String> cycle = visit(requiredId, byId, state, stack);
            if (cycle != null) {
                return cycle;
            }
        }
        stack.remove(stack.size() - 1);
        state.put(id, 2);
        return null;
    }

    public static List<String> missingQuestIds(Quest quest, Collection<Quest> all) {
        List<String> missing = new ArrayList<>();
        if (quest == null) {
            return missing;
        }
        for (String requiredId : quest.requiredQuests) {
            boolean found = false;
            for (Quest candidate : all) {
                if (candidate != null && requiredId.equals(candidate.id)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(requiredId);
            }
        }
        return missing;
    }
}
