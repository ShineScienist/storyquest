package com.shinecreator.storyquest.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.shinecreator.storyquest.network.QuestPayloads;
import com.shinecreator.storyquest.quest.PlayerProgress;
import com.shinecreator.storyquest.quest.Quest;
import com.shinecreator.storyquest.quest.QuestJson;
import com.shinecreator.storyquest.quest.QuestManager;
import com.shinecreator.storyquest.quest.QuestSavedData;
import com.shinecreator.storyquest.quest.SharedQuestProgress;
import com.shinecreator.storyquest.storage.QuestStorage;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@code /storyquest ...} — the scripting hook for maps and command blocks.
 */
public final class StoryQuestCommand {
    private static final SuggestionProvider<CommandSourceStack> QUEST_IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    QuestSavedData.get(context.getSource().getServer()).ids(), builder);

    private StoryQuestCommand() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("storyquest")
                .then(Commands.literal("journal")
                        .executes(StoryQuestCommand::journal))
                .then(Commands.literal("editor")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .executes(StoryQuestCommand::editor))
                .then(Commands.literal("list")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .executes(StoryQuestCommand::list))
                .then(Commands.literal("start")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUEST_IDS)
                                .executes(context -> start(context, List.of(self(context))))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> start(context, EntityArgument.getPlayers(context, "targets"))))))
                .then(Commands.literal("force-start")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUEST_IDS)
                                .executes(context -> forceStart(context, List.of(self(context))))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> forceStart(context, EntityArgument.getPlayers(context, "targets"))))))
                .then(Commands.literal("complete")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUEST_IDS)
                                .executes(context -> complete(context, List.of(self(context))))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> complete(context, EntityArgument.getPlayers(context, "targets"))))))
                .then(Commands.literal("objective")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUEST_IDS)
                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                .executes(context -> objective(context, List.of(self(context)),
                                                        IntegerArgumentType.getInteger(context, "index")))
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(context -> objective(context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                IntegerArgumentType.getInteger(context, "index"))))))))
                .then(Commands.literal("track")
                        .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUEST_IDS)
                                .executes(context -> track(context, List.of(self(context)),
                                        StringArgumentType.getString(context, "quest")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .requires(commandSource -> commandSource.hasPermission(2))
                                        .executes(context -> track(context, EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "quest"))))))
                .then(Commands.literal("untrack")
                        .executes(context -> untrack(context, List.of(self(context))))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(commandSource -> commandSource.hasPermission(2))
                                .executes(context -> untrack(context, EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("reset")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .executes(context -> reset(context, List.of(self(context))))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> reset(context, EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("reset-shared")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .executes(context -> resetShared(context, ""))
                        .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUEST_IDS)
                                .executes(context -> resetShared(context,
                                        StringArgumentType.getString(context, "quest")))))
                .then(Commands.literal("export")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .executes(context -> export(context, ""))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> export(context,
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("import")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> importQuests(context,
                                        StringArgumentType.getString(context, "name"), false, false))
                                .then(Commands.literal("merge")
                                        .executes(context -> importQuests(context,
                                                StringArgumentType.getString(context, "name"), false, false)))
                                .then(Commands.literal("replace")
                                        .executes(context -> importQuests(context,
                                                StringArgumentType.getString(context, "name"), true, false))
                                        .then(Commands.literal("confirm")
                                                .executes(context -> importQuests(context,
                                                        StringArgumentType.getString(context, "name"),
                                                        true, true))))))
                .then(Commands.literal("dialogue")
                        .requires(commandSource -> commandSource.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("speaker", StringArgumentType.string())
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(StoryQuestCommand::dialogue))))));
    }

    // ------------------------------------------------------------------ subcommands

    private static int journal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = self(context);
        QuestManager.sync(player);
        PacketDistributor.sendToPlayer(player, new QuestPayloads.OpenJournal());
        return 1;
    }

    private static int editor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        QuestManager.openEditor(self(context));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        List<Quest> quests = QuestSavedData.get(context.getSource().getServer()).sortedQuests();
        if (quests.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.no_quests"));
            return 0;
        }
        for (Quest quest : quests) {
            Component chapter = quest.chapter.isBlank()
                    ? Component.translatable("storyquest.journal.no_chapter")
                    : Component.literal(quest.chapter);
            context.getSource().sendSuccess(() -> Component.literal("- ")
                    .append(Component.literal(quest.id).withStyle(net.minecraft.ChatFormatting.YELLOW))
                    .append(Component.literal(" | " + quest.title + " | ").append(chapter)), false);
        }
        return quests.size();
    }

    private static int start(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        Quest quest = getQuest(context);
        if (quest == null) {
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            if (QuestManager.startQuest(target, quest)) {
                count++;
            }
        }
        return feedback(context, count, "storyquest.command.started", quest.title, count);
    }

    private static int forceStart(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        Quest quest = getQuest(context);
        if (quest == null) {
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            if (QuestManager.startQuestForced(target, quest)) {
                count++;
            }
        }
        return feedback(context, count, "storyquest.command.started", quest.title, count);
    }

    private static int complete(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        Quest quest = getQuest(context);
        if (quest == null) {
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            if (QuestManager.completeQuest(target, quest)) {
                count++;
            }
        }
        return feedback(context, count, "storyquest.command.completed", quest.title, count);
    }

    private static int objective(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, int oneBasedIndex) {
        Quest quest = getQuest(context);
        if (quest == null) {
            return 0;
        }
        int index = oneBasedIndex - 1;
        if (index < 0 || index >= quest.objectives.size()) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.no_objective", oneBasedIndex));
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            if (QuestManager.completeObjective(target, quest, index, true)) {
                count++;
            }
        }
        return feedback(context, count, "storyquest.command.objective_done",
                quest.objectives.get(index).displayText(), count);
    }

    private static int track(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, String questId) {
        Quest quest = QuestSavedData.get(context.getSource().getServer()).get(questId);
        if (quest == null) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.unknown_quest", questId));
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            if (QuestManager.setTracked(target, questId)) {
                count++;
            }
        }
        return feedback(context, count, "storyquest.command.tracked", quest.title, count);
    }

    private static int untrack(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        int count = 0;
        for (ServerPlayer target : targets) {
            if (QuestManager.setTracked(target, "")) {
                count++;
            }
        }
        return feedback(context, count, "storyquest.command.untracked", "", count);
    }

    private static int reset(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            QuestManager.reset(target);
        }
        return feedback(context, targets.size(), "storyquest.command.reset", "", targets.size());
    }

    private static int resetShared(CommandContext<CommandSourceStack> context, String questId) {
        QuestManager.resetShared(context.getSource().getServer(), questId);
        context.getSource().sendSuccess(() -> Component.translatable("storyquest.command.shared_reset",
                Component.literal(questId == null || questId.isBlank() ? "*" : questId)), true);
        return 1;
    }

    private static int export(CommandContext<CommandSourceStack> context, String requestedName) {
        String name = requestedName == null || requestedName.isBlank()
                ? QuestStorage.defaultExportName()
                : requestedName;
        try {
            Path path = QuestStorage.resolveJson(name);
            List<Quest> quests = QuestSavedData.get(context.getSource().getServer()).sortedQuests();
            QuestStorage.write(path, QuestJson.toJson(quests));
            context.getSource().sendSuccess(() -> Component.translatable("storyquest.command.exported",
                    quests.size(), Component.literal(path.toAbsolutePath().toString())), true);
            return quests.size();
        } catch (IllegalArgumentException exception) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.invalid_file",
                    exception.getMessage()));
            return 0;
        } catch (IOException exception) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.export_failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int importQuests(CommandContext<CommandSourceStack> context, String name,
            boolean replace, boolean confirmed) {
        if (replace && !confirmed) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.import_confirm"));
            return 0;
        }

        Path path;
        try {
            path = QuestStorage.resolveJson(name);
        } catch (IllegalArgumentException exception) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.invalid_file",
                    exception.getMessage()));
            return 0;
        }
        if (!Files.exists(path)) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.import_missing",
                    Component.literal(path.toAbsolutePath().toString())));
            return 0;
        }

        QuestJson.Result result;
        try {
            result = QuestJson.parse(QuestStorage.read(path));
        } catch (IOException exception) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.import_failed",
                    exception.getMessage()));
            return 0;
        }

        QuestSavedData data = QuestSavedData.get(context.getSource().getServer());
        List<Quest> existing = replace ? List.of() : new ArrayList<>(data.quests());
        QuestJson.validateAgainst(result.quests, existing, result.errors, result.warnings);
        if (!result.ok()) {
            sendIssues(context.getSource(), result.errors);
            return 0;
        }

        try {
            Path backup = QuestStorage.backup(context.getSource().getServer());
            if (backup != null) {
                context.getSource().sendSuccess(() -> Component.translatable("storyquest.command.backup_created",
                        Component.literal(backup.toAbsolutePath().toString())), false);
            }
        } catch (IOException exception) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.backup_failed",
                    exception.getMessage()));
            return 0;
        }

        List<String> importedIds = new ArrayList<>();
        for (Quest quest : result.quests) {
            importedIds.add(quest.id);
        }
        List<String> removedIds = new ArrayList<>();
        if (replace) {
            for (Quest old : data.quests()) {
                if (!importedIds.contains(old.id)) {
                    removedIds.add(old.id);
                }
            }
            data.clear();
        }
        for (Quest quest : result.quests) {
            data.put(quest);
        }

        for (String removedId : removedIds) {
            for (ServerPlayer online : context.getSource().getServer().getPlayerList().getPlayers()) {
                PlayerProgress.of(online).forgetQuest(removedId);
            }
            SharedQuestProgress.get(context.getSource().getServer()).forgetQuest(removedId);
        }

        QuestManager.syncAll(context.getSource().getServer());
        for (ServerPlayer online : context.getSource().getServer().getPlayerList().getPlayers()) {
            QuestManager.autoStart(online);
        }

        for (String warning : result.warnings) {
            context.getSource().sendSuccess(() -> Component.translatable("storyquest.command.import_warning",
                    Component.literal(warning)), false);
        }
        String mode = replace ? "replace" : "merge";
        context.getSource().sendSuccess(() -> Component.translatable("storyquest.command.imported",
                result.quests.size(), Component.literal(mode)), true);
        return result.quests.size();
    }

    private static void sendIssues(CommandSourceStack source, List<String> issues) {
        int shown = Math.min(5, issues.size());
        for (int i = 0; i < shown; i++) {
            source.sendFailure(Component.literal(issues.get(i)));
        }
        if (issues.size() > shown) {
            source.sendFailure(Component.translatable("storyquest.command.more_issues",
                    issues.size() - shown));
        }
    }

    private static int dialogue(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        String speaker = StringArgumentType.getString(context, "speaker");
        String text = StringArgumentType.getString(context, "text");
        for (ServerPlayer target : targets) {
            QuestManager.sendDialogue(target, "", speaker, text);
        }
        return feedback(context, targets.size(), "storyquest.command.dialogue", "", targets.size());
    }

    // ------------------------------------------------------------------ helpers

    private static Quest getQuest(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "quest");
        Quest quest = QuestSavedData.get(context.getSource().getServer()).get(id);
        if (quest == null) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.unknown_quest", id));
        }
        return quest;
    }

    private static ServerPlayer self(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return context.getSource().getPlayerOrException();
    }

    private static int feedback(CommandContext<CommandSourceStack> context, int count, String key, String subject, int players) {
        if (count <= 0) {
            context.getSource().sendFailure(Component.translatable("storyquest.command.no_targets"));
            return 0;
        }
        Component message = Component.translatable(key,
                Component.literal(subject).withStyle(net.minecraft.ChatFormatting.YELLOW), players);
        context.getSource().sendSuccess(() -> message, true);
        return count;
    }
}
