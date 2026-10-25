package com.shinecreator.storyquest.client;

import java.util.ArrayList;
import java.util.List;

import com.shinecreator.storyquest.quest.Objective;
import com.shinecreator.storyquest.quest.Quest;

/**
 * Collects the visit objectives of the tracked quest that should be visible
 * as world markers and on the waypoint compass.
 */
public final class QuestWaypoints {
    public record Waypoint(Quest quest, int objectiveIndex, Objective objective) {
        public String label() {
            return objective.markerLabel();
        }

        public int color() {
            return objective.markerRgb();
        }

        public String dimension() {
            return objective.normalizeDimension();
        }
    }

    private QuestWaypoints() {}

    public static List<Waypoint> active() {
        Quest quest = ClientQuestCache.trackedQuest();
        if (quest == null) {
            return List.of();
        }
        List<Waypoint> result = new ArrayList<>();
        for (int i = 0; i < quest.objectives.size(); i++) {
            Objective objective = quest.objectives.get(i);
            if (objective.type != Objective.Type.VISIT || !objective.markerVisible) {
                continue;
            }
            if (ClientQuestCache.isObjectiveDone(quest.id, i)) {
                continue;
            }
            result.add(new Waypoint(quest, i, objective));
        }
        return result;
    }
}
