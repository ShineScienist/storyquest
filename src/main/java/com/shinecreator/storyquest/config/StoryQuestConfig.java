package com.shinecreator.storyquest.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side settings that affect quest logic.
 */
public final class StoryQuestConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue VISIT_CHECK_INTERVAL = BUILDER
            .comment("How often visit objectives are checked, in ticks. 20 ticks = 1 second.")
            .defineInRange("visitCheckIntervalTicks", 10, 1, 200);

    public static final ModConfigSpec.BooleanValue SHOW_MESSAGES = BUILDER
            .comment("Send quest progress messages to the player's chat.")
            .define("showChatMessages", true);

    public static final ModConfigSpec.BooleanValue PLAY_SOUNDS = BUILDER
            .comment("Play a notification sound when quests or objectives are completed.")
            .define("playSounds", true);

    public static final ModConfigSpec.BooleanValue ALLOW_MANUAL_START = BUILDER
            .comment("Allow players to accept quests from the journal if the quest allows manual start.")
            .define("allowManualStartFromJournal", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private StoryQuestConfig() {}
}
