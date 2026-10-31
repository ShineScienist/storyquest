package com.shinecreator.storyquest.quest;

import com.shinecreator.storyquest.StoryQuest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Turns gameplay events into counter progress for active quests.
 * All handlers ignore client-side events and players who are not on a server.
 */
@EventBusSubscriber(modid = StoryQuest.MODID)
public final class QuestObjectiveEvents {
    private QuestObjectiveEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) {
            return;
        }
        var id = EntityType.getKey(dead.getType());
        if (id != null) {
            QuestManager.action(player, Objective.Type.KILL, id.toString());
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) {
            return;
        }
        var id = BuiltInRegistries.ITEM.getKey(crafted.getItem());
        if (id != null) {
            QuestManager.action(player, Objective.Type.CRAFT, id.toString(), crafted.getCount());
        }
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            // Collect objectives read the whole inventory, so a pickup only
            // triggers an immediate rescan instead of adding to a counter.
            QuestManager.refreshCollect(player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        Block block = event.getState().getBlock();
        var id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            QuestManager.action(player, Objective.Type.MINE, id.toString());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        var id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            QuestManager.action(player, Objective.Type.INTERACT, id.toString());
        }
    }
}
