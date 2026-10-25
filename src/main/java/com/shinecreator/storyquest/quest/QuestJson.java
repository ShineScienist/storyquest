package com.shinecreator.storyquest.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.shinecreator.storyquest.StoryQuest;

import net.neoforged.fml.ModList;

/**
 * JSON exchange format for quest definitions. Player progress is not included.
 */
public final class QuestJson {
    public static final int FORMAT_VERSION = 1;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private QuestJson() {}

    public static final class Result {
        public final List<Quest> quests = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();

        public boolean ok() {
            return errors.isEmpty();
        }
    }

    public static String toJson(Collection<Quest> quests) {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", FORMAT_VERSION);
        root.addProperty("modVersion", modVersion());
        JsonArray array = new JsonArray();
        for (Quest quest : quests) {
            array.add(writeQuest(quest));
        }
        root.add("quests", array);
        return GSON.toJson(root);
    }

    public static Result parse(String json) {
        Result result = new Result();
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                result.errors.add("Root must be a JSON object.");
                return result;
            }
            root = parsed.getAsJsonObject();
        } catch (JsonSyntaxException exception) {
            result.errors.add("Invalid JSON: " + exception.getMessage());
            return result;
        }

        int version = intValue(root, "formatVersion", FORMAT_VERSION, result.errors);
        if (version != FORMAT_VERSION) {
            result.errors.add("Unsupported formatVersion: " + version + " (expected " + FORMAT_VERSION + ").");
            return result;
        }
        if (!root.has("quests") || !root.get("quests").isJsonArray()) {
            result.errors.add("Missing quests array.");
            return result;
        }

        JsonArray array = root.getAsJsonArray("quests");
        Map<String, Quest> seen = new LinkedHashMap<>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                result.errors.add("Quest #" + (i + 1) + " is not an object.");
                continue;
            }
            Quest quest = readQuest(element.getAsJsonObject(), i + 1, result.errors, result.warnings);
            if (quest == null) {
                continue;
            }
            if (seen.containsKey(quest.id)) {
                result.errors.add("Duplicate quest id: " + quest.id);
                continue;
            }
            seen.put(quest.id, quest);
            result.quests.add(quest);
        }
        return result;
    }

    /**
     * Checks cycles, missing references and likely authoring mistakes against
     * the quest set that will exist after import.
     */
    public static void validateAgainst(List<Quest> imported, Collection<Quest> existing,
            List<String> errors, List<String> warnings) {
        Map<String, Quest> combined = new LinkedHashMap<>();
        if (existing != null) {
            for (Quest quest : existing) {
                combined.put(quest.id, quest);
            }
        }
        for (Quest quest : imported) {
            combined.put(quest.id, quest);
        }

        for (Quest quest : imported) {
            List<String> cycle = QuestConditions.findCycle(quest, combined.values());
            if (!cycle.isEmpty()) {
                errors.add("Condition cycle: " + String.join(" -> ", cycle));
            }
            for (String requiredId : quest.requiredQuests) {
                Quest required = combined.get(requiredId);
                if (required == null) {
                    warnings.add("Missing quest requirement: " + requiredId + " (in " + quest.id + ").");
                } else if (required.scope != quest.scope) {
                    warnings.add("Mixed personal/shared chain: " + quest.id + " requires " + requiredId + ".");
                }
            }
        }

        Map<String, Integer> autoStartBranches = new LinkedHashMap<>();
        for (Quest quest : combined.values()) {
            if (quest.autoStart && !quest.branch.isBlank()) {
                String key = quest.scope.name() + ":" + quest.branch;
                autoStartBranches.merge(key, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> entry : autoStartBranches.entrySet()) {
            if (entry.getValue() > 1) {
                warnings.add("More than one autoStart quest in branch " + entry.getKey() + ".");
            }
        }
    }

    private static JsonObject writeQuest(Quest quest) {
        JsonObject object = new JsonObject();
        object.addProperty("id", quest.id);
        object.addProperty("title", quest.title);
        object.addProperty("chapter", quest.chapter);
        object.addProperty("order", quest.order);
        object.addProperty("description", quest.description);
        object.addProperty("narrator", quest.narrator);
        object.addProperty("intro", quest.intro);
        object.addProperty("outro", quest.outro);
        object.addProperty("startCommands", quest.startCommands);
        object.addProperty("completeCommands", quest.completeCommands);
        object.addProperty("visible", quest.visible);
        object.addProperty("manualStart", quest.manualStart);
        object.addProperty("autoStart", quest.autoStart);
        object.addProperty("autoComplete", quest.autoComplete);
        object.addProperty("requireAll", quest.requireAll);
        object.addProperty("hideUntilAvailable", quest.hideUntilAvailable);
        object.addProperty("branch", quest.branch);
        object.addProperty("scope", quest.scope.name().toLowerCase(Locale.ROOT));

        JsonArray required = new JsonArray();
        for (String id : quest.requiredQuests) {
            required.add(id);
        }
        object.add("requiredQuests", required);

        JsonArray objectives = new JsonArray();
        for (Objective objective : quest.objectives) {
            objectives.add(writeObjective(objective));
        }
        object.add("objectives", objectives);
        return object;
    }

    private static JsonObject writeObjective(Objective objective) {
        JsonObject object = new JsonObject();
        object.addProperty("type", objective.type.name().toLowerCase(Locale.ROOT));
        object.addProperty("text", objective.text);
        object.addProperty("dimension", objective.normalizeDimension());
        object.addProperty("x", objective.x);
        object.addProperty("y", objective.y);
        object.addProperty("z", objective.z);
        object.addProperty("radius", objective.radius);
        object.addProperty("markerVisible", objective.markerVisible);
        object.addProperty("markerBeam", objective.markerBeam);
        object.addProperty("markerColor", String.format(Locale.ROOT, "#%06X",
                objective.markerColor & 0x00FFFFFF));
        object.addProperty("markerIcon", (objective.markerIcon == null
                ? MarkerIcon.CHECK : objective.markerIcon).id());
        object.addProperty("markerText", objective.markerText);
        return object;
    }

    private static Quest readQuest(JsonObject object, int index, List<String> errors,
            List<String> warnings) {
        Quest quest = new Quest();
        try {
            String rawId = stringValue(object, "id", "");
            String id = Quest.sanitizeId(rawId);
            if (id.isBlank() || !id.equals(rawId)) {
                errors.add("Quest #" + index + ": invalid id '" + rawId + "'.");
                return null;
            }
            quest.id = id;
            quest.title = stringValue(object, "title", id);
            quest.chapter = stringValue(object, "chapter", "");
            quest.order = intValue(object, "order", 0, errors);
            quest.description = stringValue(object, "description", "");
            quest.narrator = stringValue(object, "narrator", "");
            quest.intro = stringValue(object, "intro", "");
            quest.outro = stringValue(object, "outro", "");
            quest.startCommands = stringValue(object, "startCommands", "");
            quest.completeCommands = stringValue(object, "completeCommands", "");
            quest.visible = boolValue(object, "visible", true);
            quest.manualStart = boolValue(object, "manualStart", false);
            quest.autoStart = boolValue(object, "autoStart", false);
            quest.autoComplete = boolValue(object, "autoComplete", true);
            quest.requireAll = boolValue(object, "requireAll", true);
            quest.hideUntilAvailable = boolValue(object, "hideUntilAvailable", false);
            quest.branch = stringValue(object, "branch", "");

            String scope = stringValue(object, "scope", "solo").toLowerCase(Locale.ROOT);
            if (scope.equals("solo")) {
                quest.scope = Quest.Scope.SOLO;
            } else if (scope.equals("shared")) {
                quest.scope = Quest.Scope.SHARED;
            } else {
                errors.add("Quest " + id + ": invalid scope '" + scope + "'.");
            }

            if (object.has("requiredQuests")) {
                if (!object.get("requiredQuests").isJsonArray()) {
                    errors.add("Quest " + id + ": requiredQuests must be an array.");
                } else {
                    JsonArray required = object.getAsJsonArray("requiredQuests");
                    for (JsonElement element : required) {
                        if (!element.isJsonPrimitive()) {
                            errors.add("Quest " + id + ": requiredQuests must contain strings.");
                            continue;
                        }
                        String requiredId = element.getAsString().trim().toLowerCase(Locale.ROOT);
                        if (!requiredId.matches("[a-z0-9_]{1,64}")) {
                            errors.add("Quest " + id + ": invalid required quest id '" + requiredId + "'.");
                        } else {
                            quest.requiredQuests.add(requiredId);
                        }
                    }
                }
            }
            if (quest.requiredQuests.contains(quest.id)) {
                // Quest.sanitize() removes self references, so reject the file before
                // that cleanup can hide the authoring mistake.
                errors.add("Quest " + id + ": cannot require itself.");
            }

            if (object.has("objectives")) {
                if (!object.get("objectives").isJsonArray()) {
                    errors.add("Quest " + id + ": objectives must be an array.");
                } else {
                    JsonArray objectives = object.getAsJsonArray("objectives");
                    for (int i = 0; i < objectives.size(); i++) {
                        JsonElement element = objectives.get(i);
                        if (!element.isJsonObject()) {
                            errors.add("Quest " + id + ": objective #" + (i + 1) + " is not an object.");
                            continue;
                        }
                        Objective objective = readObjective(element.getAsJsonObject(), id, i + 1,
                                errors);
                        if (objective != null) {
                            quest.objectives.add(objective);
                        }
                    }
                }
            }

            quest.sanitize();
            return quest;
        } catch (RuntimeException exception) {
            errors.add("Quest #" + index + ": " + exception.getMessage());
            return null;
        }
    }

    private static Objective readObjective(JsonObject object, String questId, int index,
            List<String> errors) {
        Objective objective = new Objective();
        String prefix = "Quest " + questId + " objective #" + index + ": ";

        String type = stringValue(object, "type", "manual").toLowerCase(Locale.ROOT);
        if (type.equals("manual")) {
            objective.type = Objective.Type.MANUAL;
        } else if (type.equals("visit")) {
            objective.type = Objective.Type.VISIT;
        } else {
            errors.add(prefix + "invalid type '" + type + "'.");
        }

        objective.text = stringValue(object, "text", "");
        objective.dimension = stringValue(object, "dimension", "minecraft:overworld");
        if (net.minecraft.resources.ResourceLocation.tryParse(objective.normalizeDimension()) == null) {
            errors.add(prefix + "invalid dimension '" + objective.dimension + "'.");
        }

        objective.x = doubleValue(object, "x", 0.0D, prefix, errors);
        objective.y = doubleValue(object, "y", 0.0D, prefix, errors);
        objective.z = doubleValue(object, "z", 0.0D, prefix, errors);
        objective.radius = doubleValue(object, "radius", 5.0D, prefix, errors);
        if (!Double.isFinite(objective.radius) || objective.radius < 0.5D || objective.radius > 512.0D) {
            errors.add(prefix + "radius must be between 0.5 and 512.");
        }

        objective.markerVisible = boolValue(object, "markerVisible", true);
        objective.markerBeam = boolValue(object, "markerBeam", true);
        objective.markerColor = readColor(object, prefix, errors);
        objective.markerText = stringValue(object, "markerText", "");

        String icon = stringValue(object, "markerIcon", "check");
        MarkerIcon resolved = null;
        for (MarkerIcon candidate : MarkerIcon.values()) {
            if (candidate.name().equalsIgnoreCase(icon)) {
                resolved = candidate;
                break;
            }
        }
        if (resolved == null) {
            errors.add(prefix + "invalid markerIcon '" + icon + "'.");
            resolved = MarkerIcon.CHECK;
        }
        objective.markerIcon = resolved;
        return objective;
    }

    private static int readColor(JsonObject object, String prefix, List<String> errors) {
        if (!object.has("markerColor")) {
            return 0xFFC94A;
        }
        JsonElement element = object.get("markerColor");
        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt() & 0x00FFFFFF;
            }
            String raw = element.getAsString().trim();
            if (raw.startsWith("#")) {
                raw = raw.substring(1);
            }
            if (raw.length() == 8) {
                raw = raw.substring(2);
            }
            if (!raw.matches("[0-9a-fA-F]{6}")) {
                errors.add(prefix + "markerColor must be #RRGGBB.");
                return 0xFFC94A;
            }
            return Integer.parseInt(raw, 16) & 0x00FFFFFF;
        } catch (RuntimeException exception) {
            errors.add(prefix + "invalid markerColor.");
            return 0xFFC94A;
        }
    }

    private static String stringValue(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    private static boolean boolValue(JsonObject object, String key, boolean fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsBoolean();
    }

    private static int intValue(JsonObject object, String key, int fallback, List<String> errors) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException exception) {
            errors.add("Field '" + key + "' must be an integer.");
            return fallback;
        }
    }

    private static double doubleValue(JsonObject object, String key, double fallback,
            String prefix, List<String> errors) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            double value = object.get(key).getAsDouble();
            if (!Double.isFinite(value)) {
                errors.add(prefix + key + " must be finite.");
                return fallback;
            }
            return value;
        } catch (RuntimeException exception) {
            errors.add(prefix + key + " must be a number.");
            return fallback;
        }
    }

    private static String modVersion() {
        return ModList.get().getModContainerById(StoryQuest.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
