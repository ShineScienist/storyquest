package com.shinecreator.storyquest.quest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * A quest definition. Quests are stored in the world save and edited in game,
 * so this class is the single source of truth for both the server and the client editor.
 */
public final class Quest {
    public enum Scope {
        SOLO,
        SHARED
    }

    public String id = "";
    public String title = "";
    public String chapter = "";
    public String description = "";
    public String narrator = "";
    /** Pages of text shown when the quest starts. Use {@code |} to split pages. */
    public String intro = "";
    /** Pages of text shown when the quest is completed. Use {@code |} to split pages. */
    public String outro = "";
    /** Commands executed when the quest starts, separated by {@code ;}. */
    public String startCommands = "";
    /** Commands executed when the quest is completed, separated by {@code ;}. */
    public String completeCommands = "";
    public int order;
    public boolean visible = true;
    public boolean manualStart;
    public boolean autoStart;
    public boolean autoComplete = true;

    /** Quest ids that must be completed before this quest becomes available. */
    public final List<String> requiredQuests = new ArrayList<>();
    /** {@code true}: all required quests; {@code false}: any of them. */
    public boolean requireAll = true;
    /** Hide the quest from the journal until its requirements are met. */
    public boolean hideUntilAvailable;
    /** Quests sharing a non-empty branch are mutually exclusive. */
    public String branch = "";
    /** Personal progress or one world-shared progress for co-op. */
    public Scope scope = Scope.SOLO;

    public final List<Objective> objectives = new ArrayList<>();

    public Quest copy() {
        return load(save());
    }

    public void sanitize() {
        id = sanitizeId(id);
        title = title == null ? "" : title.trim();
        if (title.isEmpty()) {
            title = id;
        }
        chapter = chapter == null ? "" : chapter.trim();
        description = description == null ? "" : description.trim();
        narrator = narrator == null ? "" : narrator.trim();
        intro = intro == null ? "" : intro.trim();
        outro = outro == null ? "" : outro.trim();
        startCommands = startCommands == null ? "" : startCommands.trim();
        completeCommands = completeCommands == null ? "" : completeCommands.trim();

        LinkedHashSet<String> normalizedRequired = new LinkedHashSet<>();
        for (String requiredId : requiredQuests) {
            if (requiredId == null) {
                continue;
            }
            String normalized = requiredId.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty() && !normalized.equals(id)) {
                normalizedRequired.add(normalized);
            }
        }
        requiredQuests.clear();
        requiredQuests.addAll(normalizedRequired);

        branch = branch == null ? "" : branch.trim().toLowerCase(Locale.ROOT);
        if (branch.length() > 64) {
            branch = branch.substring(0, 64);
        }
        if (scope == null) {
            scope = Scope.SOLO;
        }

        objectives.removeIf(java.util.Objects::isNull);
        for (int i = 0; i < objectives.size(); i++) {
            Objective objective = objectives.get(i);
            if (objective.text == null || objective.text.isBlank()) {
                objective.text = "Objective " + (i + 1);
            }
            objective.radius = Math.max(0.5D, Math.min(512.0D, objective.radius));
            objective.markerColor &= 0x00FFFFFF;
            objective.markerText = objective.markerText == null ? "" : objective.markerText.trim();
            if (objective.markerText.length() > 64) {
                objective.markerText = objective.markerText.substring(0, 64);
            }
            if (objective.markerIcon == null) {
                objective.markerIcon = MarkerIcon.CHECK;
            }
        }
    }

    public String chapterDisplay() {
        return chapter.isBlank() ? "storyquest.journal.no_chapter" : chapter;
    }

    public void copyConditionsFrom(Quest other) {
        requiredQuests.clear();
        requiredQuests.addAll(other.requiredQuests);
        requireAll = other.requireAll;
        hideUntilAvailable = other.hideUntilAvailable;
        branch = other.branch;
        scope = other.scope;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id == null ? "" : id);
        tag.putString("title", title == null ? "" : title);
        tag.putString("chapter", chapter == null ? "" : chapter);
        tag.putString("description", description == null ? "" : description);
        tag.putString("narrator", narrator == null ? "" : narrator);
        tag.putString("intro", intro == null ? "" : intro);
        tag.putString("outro", outro == null ? "" : outro);
        tag.putString("startCommands", startCommands == null ? "" : startCommands);
        tag.putString("completeCommands", completeCommands == null ? "" : completeCommands);
        tag.putInt("order", order);
        tag.putBoolean("visible", visible);
        tag.putBoolean("manualStart", manualStart);
        tag.putBoolean("autoStart", autoStart);
        tag.putBoolean("autoComplete", autoComplete);
        tag.putBoolean("requireAll", requireAll);
        tag.putBoolean("hideUntilAvailable", hideUntilAvailable);
        tag.putString("branch", branch == null ? "" : branch);
        tag.putString("scope", scope == null ? Scope.SOLO.name().toLowerCase(Locale.ROOT)
                : scope.name().toLowerCase(Locale.ROOT));

        ListTag requiredTag = new ListTag();
        for (String requiredId : requiredQuests) {
            requiredTag.add(StringTag.valueOf(requiredId));
        }
        tag.put("requiredQuests", requiredTag);

        ListTag objectivesTag = new ListTag();
        for (Objective objective : objectives) {
            objectivesTag.add(objective.save());
        }
        tag.put("objectives", objectivesTag);
        return tag;
    }

    public static Quest load(CompoundTag tag) {
        Quest quest = new Quest();
        quest.id = tag.getString("id");
        quest.title = tag.getString("title");
        quest.chapter = tag.getString("chapter");
        quest.description = tag.getString("description");
        quest.narrator = tag.getString("narrator");
        quest.intro = tag.getString("intro");
        quest.outro = tag.getString("outro");
        quest.startCommands = tag.getString("startCommands");
        quest.completeCommands = tag.getString("completeCommands");
        quest.order = tag.getInt("order");
        quest.visible = !tag.contains("visible", Tag.TAG_BYTE) || tag.getBoolean("visible");
        quest.manualStart = tag.getBoolean("manualStart");
        quest.autoStart = tag.getBoolean("autoStart");
        quest.autoComplete = !tag.contains("autoComplete", Tag.TAG_BYTE) || tag.getBoolean("autoComplete");
        quest.requireAll = !tag.contains("requireAll", Tag.TAG_BYTE) || tag.getBoolean("requireAll");
        quest.hideUntilAvailable = tag.getBoolean("hideUntilAvailable");
        quest.branch = tag.getString("branch");
        try {
            quest.scope = Scope.valueOf(tag.getString("scope").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            quest.scope = Scope.SOLO;
        }

        ListTag requiredTag = tag.getList("requiredQuests", Tag.TAG_STRING);
        for (int i = 0; i < requiredTag.size(); i++) {
            quest.requiredQuests.add(requiredTag.getString(i));
        }

        ListTag objectivesTag = tag.getList("objectives", Tag.TAG_COMPOUND);
        for (int i = 0; i < objectivesTag.size(); i++) {
            quest.objectives.add(Objective.load(objectivesTag.getCompound(i)));
        }
        quest.sanitize();
        return quest;
    }

    public static String sanitizeId(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_");
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64);
        }
        return cleaned;
    }

    /**
     * Quest ids are typed in chat commands, so they are limited to characters
     * that Brigadier's {@code word()} argument accepts.
     */
    public static boolean isValidId(String id) {
        if (id == null || id.isBlank() || id.length() > 64) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean allowed = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }
}
