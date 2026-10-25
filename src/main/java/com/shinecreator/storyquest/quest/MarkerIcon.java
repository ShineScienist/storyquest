package com.shinecreator.storyquest.quest;

import java.util.Locale;

import com.shinecreator.storyquest.StoryQuest;

import net.minecraft.resources.ResourceLocation;

/**
 * Built-in shapes for objective markers. The textures are plain white glyphs,
 * so they can be tinted with the color configured per objective.
 */
public enum MarkerIcon {
    CHECK,
    PIN,
    STAR;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "storyquest.marker.icon." + id();
    }

    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(StoryQuest.MODID, "textures/marker/" + id() + ".png");
    }

    public MarkerIcon next() {
        MarkerIcon[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static MarkerIcon byName(String name) {
        if (name != null) {
            for (MarkerIcon icon : values()) {
                if (icon.name().equalsIgnoreCase(name)) {
                    return icon;
                }
            }
        }
        return CHECK;
    }
}
