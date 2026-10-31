package com.shinecreator.storyquest.quest;

import java.util.Locale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * A single goal of a quest.
 * <p>
 * {@link Type#MANUAL} objectives are completed by a command or by the editor.
 * {@link Type#VISIT} objectives are completed automatically when the player enters a spherical area.
 * The remaining types count player actions (kills, items, blocks) or elapsed time.
 */
public final class Objective {
    public enum Type {
        MANUAL,
        VISIT,
        KILL,
        COLLECT,
        CRAFT,
        MINE,
        INTERACT,
        TIMER;

        public String translationKey() {
            return "storyquest.objective.type." + name().toLowerCase(Locale.ROOT);
        }

        /** Types that keep a numeric counter instead of a simple done flag. */
        public boolean isCounter() {
            return switch (this) {
                case MANUAL, VISIT -> false;
                default -> true;
            };
        }

        /** Types that need an entity, item or block id in {@link #target}. */
        public boolean needsTarget() {
            return switch (this) {
                case KILL, COLLECT, CRAFT, MINE, INTERACT -> true;
                default -> false;
            };
        }
    }

    public Type type = Type.MANUAL;
    public String text = "";
    /** Entity, item or block id for counter objectives. */
    public String target = "";
    /** Required amount for counter objectives; for timers this is seconds. */
    public int count = 1;
    /** Collect objectives only: remove the items when the objective completes. */
    public boolean consume;
    public String dimension = "minecraft:overworld";
    public double x;
    public double y;
    public double z;
    public double radius = 5.0D;

    /** Marker settings, only used by {@link Type#VISIT} objectives. */
    public boolean markerVisible = true;
    public boolean markerBeam = true;
    public int markerColor = 0xFFC94A;
    public MarkerIcon markerIcon = MarkerIcon.CHECK;
    public String markerText = "";

    public Objective copy() {
        return load(save());
    }

    public String displayText() {
        return text == null || text.isBlank() ? "Objective" : text;
    }

    /** Text shown on the waypoint compass; an empty label falls back to the objective text. */
    public String markerLabel() {
        return markerText == null || markerText.isBlank() ? displayText() : markerText.trim();
    }

    public int markerRgb() {
        return markerColor & 0x00FFFFFF;
    }

    public void copyMarkerFrom(Objective other) {
        markerVisible = other.markerVisible;
        markerBeam = other.markerBeam;
        markerColor = other.markerColor;
        markerIcon = other.markerIcon == null ? MarkerIcon.CHECK : other.markerIcon;
        markerText = other.markerText == null ? "" : other.markerText;
    }

    public boolean isInArea(String dimensionId, double px, double py, double pz) {
        if (type != Type.VISIT) {
            return false;
        }
        ResourceLocation expected = ResourceLocation.tryParse(normalizeDimension());
        if (expected == null) {
            return false;
        }
        ResourceLocation actual = ResourceLocation.tryParse(dimensionId);
        if (!expected.equals(actual)) {
            return false;
        }
        double r = radius > 0.0D ? radius : 5.0D;
        double dx = px - x;
        double dy = py - y;
        double dz = pz - z;
        return dx * dx + dy * dy + dz * dz <= r * r;
    }

    public boolean isCounted() {
        return type != null && type.isCounter();
    }

    public boolean matchesTarget(String id) {
        if (type == null || !type.needsTarget()) {
            return true;
        }
        return normalizeTarget().equals(normalizeTarget(id));
    }

    /**
     * Normalizes an id typed by hand: trims, lower-cases and adds the
     * {@code minecraft:} namespace when it is missing.
     */
    public String normalizeTarget() {
        return normalizeTarget(target);
    }

    public static String normalizeTarget(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        return trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
    }

    public String normalizeDimension() {
        if (dimension == null || dimension.isBlank()) {
            return "minecraft:overworld";
        }
        String trimmed = dimension.trim().toLowerCase(Locale.ROOT);
        return trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putString("text", text == null ? "" : text);
        tag.putString("target", target == null ? "" : target);
        tag.putInt("count", count);
        tag.putBoolean("consume", consume);
        tag.putString("dimension", normalizeDimension());
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putDouble("radius", radius);
        tag.putBoolean("markerVisible", markerVisible);
        tag.putBoolean("markerBeam", markerBeam);
        tag.putInt("markerColor", markerColor);
        tag.putString("markerIcon", markerIcon == null ? MarkerIcon.CHECK.name() : markerIcon.name());
        tag.putString("markerText", markerText == null ? "" : markerText);
        return tag;
    }

    public static Objective load(CompoundTag tag) {
        Objective objective = new Objective();
        try {
            objective.type = Type.valueOf(tag.getString("type").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            objective.type = Type.MANUAL;
        }
        objective.text = tag.contains("text", Tag.TAG_STRING) ? tag.getString("text") : "";
        objective.target = tag.contains("target", Tag.TAG_STRING) ? tag.getString("target") : "";
        objective.count = tag.contains("count", Tag.TAG_INT) ? tag.getInt("count") : 1;
        objective.consume = tag.getBoolean("consume");
        objective.dimension = tag.contains("dimension", Tag.TAG_STRING)
                ? tag.getString("dimension")
                : "minecraft:overworld";
        objective.x = tag.getDouble("x");
        objective.y = tag.getDouble("y");
        objective.z = tag.getDouble("z");
        objective.radius = tag.contains("radius") ? tag.getDouble("radius") : 5.0D;
        objective.markerVisible = !tag.contains("markerVisible", Tag.TAG_BYTE) || tag.getBoolean("markerVisible");
        objective.markerBeam = !tag.contains("markerBeam", Tag.TAG_BYTE) || tag.getBoolean("markerBeam");
        objective.markerColor = tag.contains("markerColor", Tag.TAG_INT) ? tag.getInt("markerColor") : 0xFFC94A;
        objective.markerIcon = MarkerIcon.byName(tag.contains("markerIcon", Tag.TAG_STRING) ? tag.getString("markerIcon") : "");
        objective.markerText = tag.contains("markerText", Tag.TAG_STRING) ? tag.getString("markerText") : "";
        return objective;
    }
}
