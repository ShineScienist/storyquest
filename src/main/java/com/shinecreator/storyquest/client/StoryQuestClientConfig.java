package com.shinecreator.storyquest.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side look and feel: HUD placement, scale and colors.
 * Everything here is optional eye candy, quests work without it.
 */
public final class StoryQuestClientConfig {
    public enum HudAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_HUD = BUILDER
            .comment("Show the active quest tracker on the screen.")
            .define("showHud", true);

    public static final ModConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR = BUILDER
            .comment("Screen corner where the quest tracker is placed.")
            .defineEnum("hudAnchor", HudAnchor.TOP_RIGHT);

    public static final ModConfigSpec.IntValue HUD_OFFSET_X = BUILDER
            .comment("Horizontal distance from the chosen screen corner.")
            .defineInRange("hudOffsetX", 8, -2000, 2000);

    public static final ModConfigSpec.IntValue HUD_OFFSET_Y = BUILDER
            .comment("Vertical distance from the chosen screen corner.")
            .defineInRange("hudOffsetY", 8, -2000, 2000);

    public static final ModConfigSpec.DoubleValue HUD_SCALE = BUILDER
            .comment("Scale of the quest tracker.")
            .defineInRange("hudScale", 1.0D, 0.5D, 2.0D);

    public static final ModConfigSpec.IntValue HUD_MAX_OBJECTIVES = BUILDER
            .comment("Maximum number of objectives shown in the tracker.")
            .defineInRange("hudMaxObjectives", 5, 1, 20);

    public static final ModConfigSpec.DoubleValue HUD_BACKGROUND_OPACITY = BUILDER
            .comment("Opacity of the tracker background, 0.0 - 1.0.")
            .defineInRange("hudBackgroundOpacity", 0.55D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue WAYPOINTS_ENABLED = BUILDER
            .comment("Show quest markers in the world and on the waypoint compass.")
            .define("waypointsEnabled", true);

    public static final ModConfigSpec.BooleanValue WAYPOINT_COMPASS = BUILDER
            .comment("Show the compass bar with the direction and distance to the tracked objective.")
            .define("waypointCompass", true);

    public static final ModConfigSpec.IntValue WAYPOINT_COMPASS_OFFSET_X = BUILDER
            .comment("Horizontal offset of the waypoint compass from the top center of the screen.")
            .defineInRange("waypointCompassOffsetX", 0, -2000, 2000);

    public static final ModConfigSpec.IntValue WAYPOINT_COMPASS_OFFSET_Y = BUILDER
            .comment("Vertical offset of the waypoint compass from the top of the screen.")
            .defineInRange("waypointCompassOffsetY", 8, 0, 2000);

    public static final ModConfigSpec.BooleanValue WAYPOINT_BEAMS = BUILDER
            .comment("Show vertical light beams above objective locations. Each objective can turn its own beam off.")
            .define("waypointBeams", true);

    public static final ModConfigSpec.DoubleValue WAYPOINT_BEAM_HEIGHT = BUILDER
            .comment("Height of the light beam in blocks.")
            .defineInRange("waypointBeamHeight", 48.0D, 8.0D, 256.0D);

    public static final ModConfigSpec.IntValue WAYPOINT_MAX_DISTANCE = BUILDER
            .comment("Maximum distance in blocks at which objective markers are shown. 0 means no limit.")
            .defineInRange("waypointMaxDistance", 512, 0, 4096);

    public static final ModConfigSpec.DoubleValue WAYPOINT_ICON_SCALE = BUILDER
            .comment("Scale of the floating marker icons.")
            .defineInRange("waypointIconScale", 1.0D, 0.5D, 2.0D);

    public static final ModConfigSpec.ConfigValue<String> ACCENT_COLOR = BUILDER
            .comment("Accent color used by quest UI, as #RRGGBB.")
            .define("accentColor", "#FFC94A");

    public static final ModConfigSpec.ConfigValue<String> PANEL_COLOR = BUILDER
            .comment("Panel background color used by quest UI, as #RRGGBB.")
            .define("panelColor", "#12141C");

    public static final ModConfigSpec SPEC = BUILDER.build();

    private StoryQuestClientConfig() {}

    public static int accentColor() {
        return parseColor(ACCENT_COLOR.get(), 0xFFFFC94A);
    }

    public static int panelColor() {
        return parseColor(PANEL_COLOR.get(), 0xFF12141C);
    }

    private static int parseColor(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String hex = value.trim().replace("#", "");
        try {
            if (hex.length() == 6) {
                return 0xFF000000 | Integer.parseInt(hex, 16);
            }
            if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ignored) {
            // fall through to the default
        }
        return fallback;
    }
}
