package com.shinecreator.storyquest.quest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.shinecreator.storyquest.StoryQuest;
import com.shinecreator.storyquest.config.StoryQuestConfig;
import com.shinecreator.storyquest.network.QuestPayloads;
import com.shinecreator.storyquest.storage.QuestStorage;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * All server-side quest logic: starting, completing, tracking, visit checks and syncing.
 */
public final class QuestManager {
    private static final int TIMER_TICKS = 20;

    private static int tickCounter;
    private static int timerTickCounter;
    private static boolean refreshingAvailability;

    private QuestManager() {}

    // ------------------------------------------------------------------ ticking

    public static void tick(MinecraftServer server) {
        boolean timerTick = ++timerTickCounter >= TIMER_TICKS;
        if (timerTick) {
            timerTickCounter = 0;
        }

        int interval = Math.max(1, StoryQuestConfig.VISIT_CHECK_INTERVAL.get());
        boolean visitTick = ++tickCounter >= interval;
        if (visitTick) {
            tickCounter = 0;
        }
        if (!timerTick && !visitTick) {
            return;
        }

        QuestSavedData data = QuestSavedData.get(server);
        if (data.quests().isEmpty()) {
            return;
        }
        if (visitTick) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkVisits(player, data);
            }
        }
        if (timerTick) {
            tickCounters(server, data);
        }
    }

    /** Runs once per second: collect inventories and timers. */
    private static void tickCounters(MinecraftServer server, QuestSavedData data) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        for (ServerPlayer player : players) {
            boolean changed = updatePersonalCollect(player, data);
            changed |= tickPersonalTimers(player, data);
            if (changed) {
                syncProgress(player);
            }
        }

        if (updateSharedCounters(server, data, players)) {
            syncProgressAll(server);
        }
    }

    private static boolean updatePersonalCollect(ServerPlayer player, QuestSavedData data) {
        PlayerProgress progress = PlayerProgress.of(player);
        boolean changed = false;
        for (String questId : new ArrayList<>(progress.active())) {
            if (!progress.isActive(questId)) {
                continue;
            }
            Quest quest = data.get(questId);
            if (quest == null || quest.scope == Quest.Scope.SHARED) {
                continue;
            }
            changed |= updateCollect(player, quest, progress);
        }
        return changed;
    }

    /** Rescans the player's inventory right away, e.g. after an item pickup. */
    public static void refreshCollect(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return;
        }
        QuestSavedData data = QuestSavedData.get(player.getServer());
        if (data.quests().isEmpty()) {
            return;
        }
        if (updatePersonalCollect(player, data)) {
            syncProgress(player);
        }
    }

    private static boolean updateCollect(ServerPlayer player, Quest quest, QuestProgressData progress) {
        boolean changed = false;
        for (int index = 0; index < quest.objectives.size(); index++) {
            Objective objective = quest.objectives.get(index);
            if (objective.type != Objective.Type.COLLECT || progress.isObjectiveDone(quest.id, index)) {
                continue;
            }
            int have = countItem(player, objective.normalizeTarget());
            int shown = Math.min(objective.count, have);
            if (shown != progress.counter(quest.id, index)) {
                progress.setCounter(quest.id, index, shown);
                changed = true;
            }
            if (have >= objective.count) {
                completeObjective(player, quest, index, true);
                changed = true;
                if (!isQuestActive(player, quest)) {
                    break;
                }
            }
        }
        return changed;
    }

    private static boolean tickPersonalTimers(ServerPlayer player, QuestSavedData data) {
        PlayerProgress progress = PlayerProgress.of(player);
        boolean changed = false;
        for (String questId : new ArrayList<>(progress.active())) {
            if (!progress.isActive(questId)) {
                continue;
            }
            Quest quest = data.get(questId);
            if (quest == null || quest.scope == Quest.Scope.SHARED) {
                continue;
            }
            changed |= tickTimer(player, quest, progress);
        }
        return changed;
    }

    private static boolean updateSharedCounters(MinecraftServer server, QuestSavedData data,
            List<ServerPlayer> players) {
        SharedQuestProgress shared = SharedQuestProgress.get(server);
        ServerPlayer trigger = players.get(0);
        boolean changed = false;

        for (String questId : new ArrayList<>(shared.progress().active())) {
            if (!shared.isActive(questId)) {
                continue;
            }
            Quest quest = data.get(questId);
            if (quest == null || quest.scope != Quest.Scope.SHARED) {
                continue;
            }

            for (int index = 0; index < quest.objectives.size(); index++) {
                Objective objective = quest.objectives.get(index);
                if (shared.isObjectiveDone(questId, index)) {
                    continue;
                }
                if (objective.type == Objective.Type.COLLECT) {
                    int have = countItem(players, objective.normalizeTarget());
                    int shown = Math.min(objective.count, have);
                    if (shown != shared.counter(questId, index)) {
                        shared.setCounter(questId, index, shown);
                        changed = true;
                    }
                    if (have >= objective.count) {
                        completeObjective(trigger, quest, index, true);
                        changed = true;
                    }
                } else if (objective.type == Objective.Type.TIMER) {
                    int next = shared.counter(questId, index) + 1;
                    if (next >= objective.count) {
                        shared.setCounter(questId, index, objective.count);
                        completeObjective(trigger, quest, index, true);
                        changed = true;
                    } else {
                        shared.setCounter(questId, index, next);
                        changed = true;
                    }
                }
                if (!shared.isActive(questId)) {
                    break;
                }
            }
        }
        return changed;
    }

    private static boolean tickTimer(ServerPlayer player, Quest quest, QuestProgressData progress) {
        boolean changed = false;
        for (int index = 0; index < quest.objectives.size(); index++) {
            Objective objective = quest.objectives.get(index);
            if (objective.type != Objective.Type.TIMER || progress.isObjectiveDone(quest.id, index)) {
                continue;
            }
            int next = progress.counter(quest.id, index) + 1;
            if (next >= objective.count) {
                progress.setCounter(quest.id, index, objective.count);
                completeObjective(player, quest, index, true);
                changed = true;
                if (!isQuestActive(player, quest)) {
                    break;
                }
            } else {
                progress.setCounter(quest.id, index, next);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isQuestActive(ServerPlayer player, Quest quest) {
        return quest.scope == Quest.Scope.SHARED
                ? SharedQuestProgress.get(player.getServer()).isActive(quest.id)
                : PlayerProgress.of(player).isActive(quest.id);
    }

    // ------------------------------------------------------------------ objective actions

    /** Called by gameplay event hooks when the player performs a matching action. */
    public static void action(ServerPlayer player, Objective.Type type, String target) {
        action(player, type, target, 1);
    }

    public static void action(ServerPlayer player, Objective.Type type, String target, int amount) {
        if (player == null || type == null || amount <= 0) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        QuestSavedData data = QuestSavedData.get(server);
        if (data.quests().isEmpty()) {
            return;
        }

        String normalized = Objective.normalizeTarget(target);
        boolean personalChanged = false;
        PlayerProgress personal = PlayerProgress.of(player);
        for (String questId : new ArrayList<>(personal.active())) {
            if (!personal.isActive(questId)) {
                continue;
            }
            Quest quest = data.get(questId);
            if (quest == null || quest.scope == Quest.Scope.SHARED) {
                continue;
            }
            personalChanged |= applyAction(player, quest, personal, type, normalized, amount);
        }

        boolean sharedChanged = false;
        SharedQuestProgress shared = SharedQuestProgress.get(server);
        for (String questId : new ArrayList<>(shared.progress().active())) {
            if (!shared.isActive(questId)) {
                continue;
            }
            Quest quest = data.get(questId);
            if (quest == null || quest.scope != Quest.Scope.SHARED) {
                continue;
            }
            sharedChanged |= applyAction(player, quest, shared.progress(), type, normalized, amount);
        }

        if (sharedChanged) {
            syncProgressAll(server);
        } else if (personalChanged) {
            syncProgress(player);
        }
    }

    private static boolean applyAction(ServerPlayer player, Quest quest, QuestProgressData progress,
            Objective.Type type, String target, int amount) {
        boolean changed = false;
        for (int index = 0; index < quest.objectives.size(); index++) {
            Objective objective = quest.objectives.get(index);
            if (objective.type != type || progress.isObjectiveDone(quest.id, index)
                    || !objective.matchesTarget(target)) {
                continue;
            }
            int current = progress.counter(quest.id, index);
            if (current >= objective.count) {
                completeObjective(player, quest, index, true);
                changed = true;
            } else {
                int next = Math.min(objective.count, current + amount);
                progress.setCounter(quest.id, index, next);
                changed = true;
                if (next >= objective.count) {
                    completeObjective(player, quest, index, true);
                }
            }
            if (!isQuestActive(player, quest)) {
                break;
            }
        }
        return changed;
    }

    /** Counts matching items in the player's inventory, including the off-hand. */
    private static int countItem(ServerPlayer player, String targetId) {
        Item item = resolveItem(targetId);
        if (item == null) {
            return 0;
        }
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Counts matching items across all online players, for shared collect quests. */
    private static int countItem(List<ServerPlayer> players, String targetId) {
        int count = 0;
        for (ServerPlayer player : players) {
            count += countItem(player, targetId);
        }
        return count;
    }

    private static void consumeItem(ServerPlayer player, String targetId, int amount) {
        Item item = resolveItem(targetId);
        if (item == null || amount <= 0) {
            return;
        }
        Inventory inventory = player.getInventory();
        int remaining = amount;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        inventory.setChanged();
        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static void consumeItem(MinecraftServer server, String targetId, int amount) {
        int remaining = amount;
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (remaining <= 0) {
                break;
            }
            int available = countItem(online, targetId);
            int taken = Math.min(available, remaining);
            consumeItem(online, targetId, taken);
            remaining -= taken;
        }
    }

    private static Item resolveItem(String targetId) {
        ResourceLocation id = ResourceLocation.tryParse(Objective.normalizeTarget(targetId));
        if (id == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? null : item;
    }

    private static void checkVisits(ServerPlayer player, QuestSavedData data) {
        PlayerProgress personal = PlayerProgress.of(player);
        Set<String> active = personal.active();
        if (!active.isEmpty()) {
            String dimension = player.level().dimension().location().toString();
            for (String questId : new ArrayList<>(active)) {
                if (!personal.isActive(questId)) {
                    // The previous quest may have completed (and cleared) this one through its commands.
                    continue;
                }
                Quest quest = data.get(questId);
                if (quest == null || quest.scope == Quest.Scope.SHARED) {
                    continue;
                }
                if (checkVisitObjectives(player, quest, personal, dimension)) {
                    break;
                }
            }
        }

        SharedQuestProgress shared = SharedQuestProgress.get(player.getServer());
        Set<String> sharedActive = shared.progress().active();
        if (!sharedActive.isEmpty()) {
            String dimension = player.level().dimension().location().toString();
            for (String questId : new ArrayList<>(sharedActive)) {
                if (!shared.isActive(questId)) {
                    continue;
                }
                Quest quest = data.get(questId);
                if (quest == null || quest.scope != Quest.Scope.SHARED) {
                    continue;
                }
                if (checkVisitObjectives(player, quest, shared.progress(), dimension)) {
                    break;
                }
            }
        }
    }

    private static boolean checkVisitObjectives(ServerPlayer player, Quest quest,
            QuestProgressData progress, String dimension) {
        for (int index = 0; index < quest.objectives.size(); index++) {
            Objective objective = quest.objectives.get(index);
            if (objective.type != Objective.Type.VISIT || progress.isObjectiveDone(quest.id, index)) {
                continue;
            }
            if (objective.isInArea(dimension, player.getX(), player.getY(), player.getZ())) {
                completeObjective(player, quest, index, true);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ player actions

    public static boolean startQuest(ServerPlayer player, Quest quest) {
        return startQuest(player, quest, false);
    }

    public static boolean startQuest(ServerPlayer player, Quest quest, boolean force) {
        if (!force) {
            Component blocker = startBlocker(player, quest);
            if (blocker != null) {
                if (StoryQuestConfig.SHOW_MESSAGES.get()) {
                    player.displayClientMessage(blocker.copy().withStyle(ChatFormatting.RED), true);
                }
                return false;
            }
        }
        boolean started = startQuestInternal(player, quest);
        if (started) {
            if (quest.scope == Quest.Scope.SHARED) {
                syncAll(player.getServer());
            } else {
                sync(player);
            }
        }
        return started;
    }

    public static boolean startQuestForced(ServerPlayer player, Quest quest) {
        return startQuest(player, quest, true);
    }

    private static Component startBlocker(ServerPlayer player, Quest quest) {
        if (quest == null) {
            return Component.translatable("storyquest.message.cannot_start");
        }
        QuestSavedData data = QuestSavedData.get(player.getServer());
        List<Quest> scoped = sameScopeQuests(quest, data.quests());

        if (quest.scope == Quest.Scope.SHARED) {
            SharedQuestProgress shared = SharedQuestProgress.get(player.getServer());
            if (shared.isCompleted(quest.id) || shared.isActive(quest.id)) {
                return null;
            }
            if (!QuestConditions.requirementsMet(quest,
                    id -> requirementCompleted(player, id, data))) {
                return Component.translatable("storyquest.message.requirements_not_met");
            }
            if (QuestConditions.branchLocked(quest,
                    id -> shared.isActive(id) || shared.isCompleted(id), scoped)) {
                return Component.translatable("storyquest.message.branch_locked");
            }
            return null;
        }

        PlayerProgress progress = PlayerProgress.of(player);
        if (progress.isCompleted(quest.id) || progress.isActive(quest.id)) {
            return null;
        }
        if (!QuestConditions.requirementsMet(quest,
                id -> requirementCompleted(player, id, data))) {
            return Component.translatable("storyquest.message.requirements_not_met");
        }
        if (QuestConditions.branchLocked(quest,
                id -> progress.isActive(id) || progress.isCompleted(id), scoped)) {
            return Component.translatable("storyquest.message.branch_locked");
        }
        return null;
    }

    private static boolean startQuestInternal(ServerPlayer player, Quest quest) {
        if (quest == null) {
            return false;
        }
        MinecraftServer server = player.getServer();

        if (quest.scope == Quest.Scope.SHARED) {
            SharedQuestProgress shared = SharedQuestProgress.get(server);
            if (shared.isCompleted(quest.id) || shared.isActive(quest.id)) {
                return false;
            }
            shared.startQuest(quest);
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                runCommands(online, quest.startCommands);
                notifyStarted(online, quest);
                PlayerProgress progress = PlayerProgress.of(online);
                if (progress.tracked().isBlank()) {
                    progress.setTrackedRaw(quest.id);
                }
            }
            return true;
        }

        PlayerProgress progress = PlayerProgress.of(player);
        if (progress.isCompleted(quest.id) || progress.isActive(quest.id)) {
            return false;
        }
        progress.startQuest(quest);
        runCommands(player, quest.startCommands);
        notifyStarted(player, quest);
        return true;
    }

    private static void notifyStarted(ServerPlayer player, Quest quest) {
        if (StoryQuestConfig.SHOW_MESSAGES.get()) {
            player.displayClientMessage(Component.translatable("storyquest.message.quest_started",
                    Component.literal(quest.title)).withStyle(ChatFormatting.GOLD), false);
        }
        if (!quest.intro.isBlank()) {
            sendDialogue(player, quest.title, quest.narrator, quest.intro);
        }
        playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F);
    }

    public static boolean completeQuest(ServerPlayer player, Quest quest) {
        if (quest == null) {
            return false;
        }
        if (quest.scope == Quest.Scope.SHARED) {
            return completeSharedQuest(player, quest);
        }

        PlayerProgress progress = PlayerProgress.of(player);
        if (progress.isCompleted(quest.id)) {
            return false;
        }
        progress.completeQuest(quest);
        runCommands(player, quest.completeCommands);
        notifyCompleted(player, quest);
        autoStart(player);
        sync(player);
        return true;
    }

    private static boolean completeSharedQuest(ServerPlayer player, Quest quest) {
        MinecraftServer server = player.getServer();
        SharedQuestProgress shared = SharedQuestProgress.get(server);
        if (shared.isCompleted(quest.id)) {
            return false;
        }
        shared.completeQuest(quest);

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            runCommands(online, quest.completeCommands);
            notifyCompleted(online, quest);
            PlayerProgress.of(online).clearTrackedIf(quest.id);
        }
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            autoStart(online);
        }
        syncAll(server);
        return true;
    }

    private static void notifyCompleted(ServerPlayer player, Quest quest) {
        if (StoryQuestConfig.SHOW_MESSAGES.get()) {
            player.displayClientMessage(Component.translatable("storyquest.message.quest_completed",
                    Component.literal(quest.title)).withStyle(ChatFormatting.GREEN), false);
        }
        if (!quest.outro.isBlank()) {
            sendDialogue(player, quest.title, quest.narrator, quest.outro);
        }
        playSound(player, SoundEvents.PLAYER_LEVELUP, 1.0F);
    }

    /**
     * @return {@code true} if the objective was newly completed
     */
    public static boolean completeObjective(ServerPlayer player, Quest quest, int index, boolean announce) {
        if (quest == null || index < 0 || index >= quest.objectives.size()) {
            return false;
        }
        Objective objective = quest.objectives.get(index);

        if (quest.scope == Quest.Scope.SHARED) {
            MinecraftServer server = player.getServer();
            SharedQuestProgress shared = SharedQuestProgress.get(server);
            if (shared.isObjectiveDone(quest.id, index)) {
                return false;
            }
            if (objective.type == Objective.Type.COLLECT && objective.consume) {
                consumeItem(server, objective.normalizeTarget(), objective.count);
            }
            shared.markObjective(quest.id, index);
            if (announce && StoryQuestConfig.SHOW_MESSAGES.get()) {
                Component message = Component.translatable("storyquest.message.objective_completed",
                        Component.literal(objective.displayText())).withStyle(ChatFormatting.YELLOW);
                for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                    online.displayClientMessage(message, false);
                    playSound(online, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.6F);
                }
            }
            if (quest.autoComplete && shared.allObjectivesDone(quest)) {
                completeSharedQuest(player, quest);
            } else {
                syncProgressAll(server);
            }
            return true;
        }

        PlayerProgress progress = PlayerProgress.of(player);
        if (progress.isObjectiveDone(quest.id, index)) {
            return false;
        }
        if (objective.type == Objective.Type.COLLECT && objective.consume) {
            consumeItem(player, objective.normalizeTarget(), objective.count);
        }
        progress.markObjective(quest.id, index);

        if (announce && StoryQuestConfig.SHOW_MESSAGES.get()) {
            player.displayClientMessage(Component.translatable("storyquest.message.objective_completed",
                    Component.literal(objective.displayText())).withStyle(ChatFormatting.YELLOW), false);
        }
        playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.6F);

        if (quest.autoComplete && progress.allObjectivesDone(quest)) {
            completeQuest(player, quest);
        } else {
            syncProgress(player);
        }
        return true;
    }

    public static boolean startFromJournal(ServerPlayer player, String questId) {
        QuestSavedData data = QuestSavedData.get(player.getServer());
        Quest quest = data.get(questId);
        if (quest == null) {
            return false;
        }
        if (!quest.manualStart || !StoryQuestConfig.ALLOW_MANUAL_START.get()) {
            if (StoryQuestConfig.SHOW_MESSAGES.get()) {
                player.displayClientMessage(Component.translatable("storyquest.message.cannot_start")
                        .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        if (quest.scope == Quest.Scope.SHARED) {
            SharedQuestProgress shared = SharedQuestProgress.get(player.getServer());
            if (shared.isCompleted(quest.id)) {
                return false;
            }
            if (shared.isActive(quest.id)) {
                PlayerProgress.of(player).setTrackedRaw(quest.id);
                sync(player);
                return true;
            }
        } else {
            PlayerProgress progress = PlayerProgress.of(player);
            if (progress.isCompleted(quest.id)) {
                return false;
            }
            if (progress.isActive(quest.id)) {
                progress.setTracked(quest.id);
                sync(player);
                return true;
            }
        }
        return startQuest(player, quest);
    }

    public static boolean setTracked(ServerPlayer player, String questId) {
        String value = questId == null ? "" : questId;
        QuestSavedData data = QuestSavedData.get(player.getServer());
        if (value.isBlank()) {
            PlayerProgress.of(player).setTrackedRaw("");
            sync(player);
            return true;
        }

        Quest quest = data.get(value);
        if (quest == null) {
            return false;
        }
        if (quest.scope == Quest.Scope.SHARED) {
            if (!SharedQuestProgress.get(player.getServer()).isActive(value)) {
                return false;
            }
            PlayerProgress.of(player).setTrackedRaw(value);
        } else {
            PlayerProgress progress = PlayerProgress.of(player);
            if (!progress.isActive(value)) {
                return false;
            }
            progress.setTracked(value);
        }
        sync(player);
        return true;
    }

    public static void reset(ServerPlayer player) {
        PlayerProgress.of(player).reset();
        sync(player);
        if (StoryQuestConfig.SHOW_MESSAGES.get()) {
            player.displayClientMessage(Component.translatable("storyquest.message.reset")
                    .withStyle(ChatFormatting.GRAY), false);
        }
    }

    public static void resetShared(MinecraftServer server, String questId) {
        QuestSavedData data = QuestSavedData.get(server);
        SharedQuestProgress shared = SharedQuestProgress.get(server);
        boolean all = questId == null || questId.isBlank();
        if (all) {
            shared.reset();
        } else {
            shared.forgetQuest(questId);
        }

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            PlayerProgress progress = PlayerProgress.of(online);
            if (all) {
                Quest tracked = data.get(progress.tracked());
                if (tracked == null || tracked.scope == Quest.Scope.SHARED) {
                    progress.setTrackedRaw("");
                }
            } else {
                progress.clearTrackedIf(questId);
            }
        }
        syncAll(server);
    }

    public static void autoStart(ServerPlayer player) {
        if (refreshingAvailability) {
            return;
        }
        refreshingAvailability = true;
        try {
            MinecraftServer server = player.getServer();
            QuestSavedData data = QuestSavedData.get(server);
            boolean changed = false;
            boolean sharedChanged = false;
            boolean started;
            int guard = 0;
            do {
                started = false;
                for (Quest quest : data.sortedQuests()) {
                    if (!quest.autoStart || isActiveOrCompleted(player, quest)) {
                        continue;
                    }
                    if (!canStartFor(player, quest, data)) {
                        continue;
                    }
                    if (startQuestInternal(player, quest)) {
                        started = true;
                        changed = true;
                        if (quest.scope == Quest.Scope.SHARED) {
                            sharedChanged = true;
                        }
                    }
                }
            } while (started && ++guard < 100);
            if (sharedChanged) {
                syncAll(server);
            } else if (changed) {
                sync(player);
            }
        } finally {
            refreshingAvailability = false;
        }
    }

    private static boolean isActiveOrCompleted(ServerPlayer player, Quest quest) {
        if (quest.scope == Quest.Scope.SHARED) {
            SharedQuestProgress shared = SharedQuestProgress.get(player.getServer());
            return shared.isActive(quest.id) || shared.isCompleted(quest.id);
        }
        PlayerProgress progress = PlayerProgress.of(player);
        return progress.isActive(quest.id) || progress.isCompleted(quest.id);
    }

    private static boolean canStartFor(ServerPlayer player, Quest quest, QuestSavedData data) {
        List<Quest> scoped = sameScopeQuests(quest, data.quests());
        if (quest.scope == Quest.Scope.SHARED) {
            SharedQuestProgress shared = SharedQuestProgress.get(player.getServer());
            return QuestConditions.canStart(quest,
                    id -> requirementCompleted(player, id, data),
                    id -> shared.isActive(id) || shared.isCompleted(id), scoped);
        }
        PlayerProgress progress = PlayerProgress.of(player);
        return QuestConditions.canStart(quest,
                id -> requirementCompleted(player, id, data),
                id -> progress.isActive(id) || progress.isCompleted(id), scoped);
    }

    private static List<Quest> sameScopeQuests(Quest quest, java.util.Collection<Quest> all) {
        List<Quest> scoped = new ArrayList<>();
        for (Quest candidate : all) {
            if (candidate.scope == quest.scope) {
                scoped.add(candidate);
            }
        }
        return scoped;
    }

    private static boolean requirementCompleted(ServerPlayer player, String requiredId, QuestSavedData data) {
        Quest required = data.get(requiredId);
        if (required != null && required.scope == Quest.Scope.SHARED) {
            return SharedQuestProgress.get(player.getServer()).isCompleted(requiredId);
        }
        return PlayerProgress.of(player).isCompleted(requiredId);
    }

    // ------------------------------------------------------------------ editor / admin

    public static void openEditor(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new QuestPayloads.OpenEditor(QuestSavedData.get(player.getServer()).toTag()));
    }

    public static boolean saveQuest(ServerPlayer player, CompoundTag tag) {
        String requestedId = tag.getString("id");
        Quest quest = Quest.load(tag);
        if (quest.id.isBlank() || !quest.id.equals(requestedId)) {
            if (StoryQuestConfig.SHOW_MESSAGES.get()) {
                player.displayClientMessage(Component.translatable("storyquest.message.invalid_id")
                        .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        QuestSavedData data = QuestSavedData.get(player.getServer());
        List<Quest> all = new ArrayList<>(data.quests());
        all.removeIf(existing -> existing.id.equals(quest.id));
        all.add(quest);
        List<String> cycle = QuestConditions.findCycle(quest, all);
        if (!cycle.isEmpty()) {
            if (StoryQuestConfig.SHOW_MESSAGES.get()) {
                player.displayClientMessage(Component.translatable("storyquest.message.condition_cycle",
                        Component.literal(String.join(" -> ", cycle))).withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        backupQuests(player);
        data.put(quest);
        syncAll(player.getServer());

        for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
            autoStart(online);
        }
        if (StoryQuestConfig.SHOW_MESSAGES.get()) {
            player.displayClientMessage(Component.translatable("storyquest.message.saved",
                    Component.literal(quest.id)).withStyle(ChatFormatting.GRAY), true);
        }
        return true;
    }

    public static boolean deleteQuest(ServerPlayer player, String questId) {
        QuestSavedData data = QuestSavedData.get(player.getServer());
        Quest deleted = data.get(questId);
        if (deleted == null) {
            return false;
        }
        backupQuests(player);
        if (!data.remove(questId)) {
            return false;
        }
        if (deleted != null && deleted.scope == Quest.Scope.SHARED) {
            SharedQuestProgress.get(player.getServer()).forgetQuest(questId);
        }
        for (Quest other : new ArrayList<>(data.quests())) {
            if (other.requiredQuests.remove(questId)) {
                data.put(other);
            }
        }
        MinecraftServer server = player.getServer();
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            PlayerProgress progress = PlayerProgress.of(online);
            progress.forgetQuest(questId);
            progress.clearTrackedIf(questId);
            sync(online);
        }
        // Deleting a quest can remove a prerequisite or a branch blocker.
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            autoStart(online);
        }
        syncAll(server);
        if (StoryQuestConfig.SHOW_MESSAGES.get()) {
            player.displayClientMessage(Component.translatable("storyquest.message.deleted",
                    Component.literal(questId)).withStyle(ChatFormatting.GRAY), true);
        }
        return true;
    }

    private static void backupQuests(ServerPlayer player) {
        try {
            QuestStorage.backup(player.getServer());
        } catch (IOException exception) {
            StoryQuest.LOGGER.warn("Could not back up StoryQuest data before saving", exception);
        }
    }

    // ------------------------------------------------------------------ networking / utils

    public static void sync(ServerPlayer player) {
        QuestSavedData data = QuestSavedData.get(player.getServer());
        PlayerProgress progress = PlayerProgress.of(player);
        sanitizeTracked(player, progress, data);

        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (Quest quest : data.sortedQuests()) {
            if (!isVisibleTo(player, quest, data)) {
                continue;
            }
            list.add(quest.save());
        }
        root.put("Quests", list);
        root.put("Progress", progress.snapshot());
        root.put("Shared", SharedQuestProgress.get(player.getServer()).snapshot());
        PacketDistributor.sendToPlayer(player, new QuestPayloads.Sync(root));
    }

    /** Sends only progress and counters, keeping the quest list on the client. */
    public static void syncProgress(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new QuestPayloads.ProgressSync(
                PlayerProgress.of(player).snapshot(),
                SharedQuestProgress.get(player.getServer()).snapshot()));
    }

    public static void syncProgressAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncProgress(player);
        }
    }

    /**
     * Clears or replaces a tracker that points to a quest completed, deleted or
     * reset while this player was offline. Without this, the HUD could stay
     * empty after a shared quest completed in the background.
     */
    private static void sanitizeTracked(ServerPlayer player, PlayerProgress progress, QuestSavedData data) {
        String tracked = progress.tracked();
        if (tracked.isBlank()) {
            return;
        }
        Quest quest = data.get(tracked);
        boolean active = quest != null && (quest.scope == Quest.Scope.SHARED
                ? SharedQuestProgress.get(player.getServer()).isActive(tracked)
                : progress.isActive(tracked));
        if (active) {
            return;
        }

        String fallback = "";
        for (Quest candidate : data.sortedQuests()) {
            if (candidate.scope == Quest.Scope.SOLO && progress.isActive(candidate.id)) {
                fallback = candidate.id;
                break;
            }
        }
        if (fallback.isBlank()) {
            SharedQuestProgress shared = SharedQuestProgress.get(player.getServer());
            for (Quest candidate : data.sortedQuests()) {
                if (candidate.scope == Quest.Scope.SHARED && shared.isActive(candidate.id)) {
                    fallback = candidate.id;
                    break;
                }
            }
        }
        progress.setTrackedRaw(fallback);
    }

    private static boolean isVisibleTo(ServerPlayer player, Quest quest, QuestSavedData data) {
        if (quest.scope == Quest.Scope.SHARED) {
            SharedQuestProgress shared = SharedQuestProgress.get(player.getServer());
            if (shared.isActive(quest.id) || shared.isCompleted(quest.id)) {
                return true;
            }
            if (!quest.visible) {
                return false;
            }
            if (quest.hideUntilAvailable && !QuestConditions.requirementsMet(quest,
                    id -> requirementCompleted(player, id, data))) {
                return false;
            }
            return true;
        }

        PlayerProgress progress = PlayerProgress.of(player);
        if (progress.isActive(quest.id) || progress.isCompleted(quest.id)) {
            return true;
        }
        if (!quest.visible) {
            return false;
        }
        if (quest.hideUntilAvailable && !QuestConditions.requirementsMet(quest,
                id -> requirementCompleted(player, id, data))) {
            return false;
        }
        return true;
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    public static void sendDialogue(ServerPlayer player, String title, String speaker, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new QuestPayloads.OpenDialogue(
                title == null ? "" : title,
                speaker == null ? "" : speaker,
                text));
    }

    public static void runCommands(ServerPlayer player, String rawCommands) {
        if (rawCommands == null || rawCommands.isBlank()) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        for (String part : rawCommands.split("[;\\n]")) {
            String command = part.trim();
            if (command.isEmpty()) {
                continue;
            }
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            command = command.replace("%player%", player.getScoreboardName());
            try {
                CommandSourceStack source = player.createCommandSourceStack()
                        .withPermission(2)
                        .withSuppressedOutput();
                server.getCommands().performPrefixedCommand(source, command);
            } catch (Exception exception) {
                StoryQuest.LOGGER.warn("Failed to run quest command '{}' for {}",
                        command, player.getScoreboardName(), exception);
            }
        }
    }

    private static void playSound(ServerPlayer player, SoundEvent sound, float pitch) {
        if (StoryQuestConfig.PLAY_SOUNDS.get()) {
            player.playNotifySound(sound, SoundSource.PLAYERS, 0.7F, pitch);
        }
    }
}
