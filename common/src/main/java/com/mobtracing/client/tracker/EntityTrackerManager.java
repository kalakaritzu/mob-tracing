package com.mobtracing.client.tracker;

import com.mobtracing.client.config.ModConfig;
import com.mobtracing.client.data.EntityAIData;
import com.mobtracing.client.data.GoalAnalyzer;
import com.mobtracing.client.data.PathDataExtractor;
import com.mobtracing.client.mixin.MobEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Collects and caches AI snapshots for all nearby Mob entities each tick.
// ConcurrentHashMap so the render thread can read while the tick thread writes.
// In dedicated multiplayer, GoalSelectors are empty (AI runs server-side) — goal labels
// will show "Idle" and paths will be absent. Works fully in singleplayer / LAN.
public final class EntityTrackerManager {

    private EntityTrackerManager() {}

    private static final Map<Integer, EntityAIData> trackedData = new ConcurrentHashMap<>();
    private static int tickTimer = 0;

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;
        if (!ModConfig.get().enabled) return;

        tickTimer++;
        if (tickTimer % Math.max(1, ModConfig.get().updateIntervalTicks) != 0) return;

        Vec3 playerPos = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
        double maxDist = ModConfig.get().maxRenderDistance;
        double maxDistSq = maxDist * maxDist;

        Set<Integer> liveIds = new HashSet<>();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Mob mob)) continue;
            if (entity.distanceToSqr(playerPos) > maxDistSq) continue;

            liveIds.add(entity.getId());

            try {
                trackedData.put(entity.getId(), buildSnapshot(mob));
            } catch (Exception ignored) {
                // Individual extraction failures must not interrupt the loop
            }
        }

        // Evict entities that are no longer nearby or loaded
        trackedData.keySet().retainAll(liveIds);
    }

    public static Collection<EntityAIData> getTrackedData() {
        return Collections.unmodifiableCollection(trackedData.values());
    }

    public static void clear() {
        trackedData.clear();
        tickTimer = 0;
    }

    private static EntityAIData buildSnapshot(Mob entity) {
        EntityAIData.EntityCategory category = categorize(entity);

        // In singleplayer/LAN we can grab the server-side mob for real AI data.
        // In dedicated multiplayer getSingleplayerServer() returns null, so we fall back to
        // the client entity which shows "Idle" with no path — expected behavior.
        Minecraft client = Minecraft.getInstance();
        Mob aiSource = getServerMob(entity.getId(), client);
        if (aiSource == null) aiSource = entity;

        List<Vec3> pathNodes    = PathDataExtractor.extractPathNodes(aiSource.getNavigation());
        int        nodeIndex    = PathDataExtractor.getCurrentNodeIndex(aiSource.getNavigation());
        String     primaryGoal  = GoalAnalyzer.getPrimaryGoal(aiSource);
        List<String> allGoals   = GoalAnalyzer.getAllRunningGoals(aiSource);
        String[]   goalsArray   = allGoals.toArray(String[]::new);

        LivingEntity target    = aiSource.getTarget();
        Vec3         targetPos = null;
        if (target != null) {
            targetPos = new Vec3(target.getX(), target.getY(), target.getZ())
                    .add(0, target.getBbHeight() * 0.5, 0);
        }

        float followRange = 16.0f;
        try {
            if (aiSource.getAttribute(Attributes.FOLLOW_RANGE) != null) {
                followRange = (float) aiSource.getAttributeValue(Attributes.FOLLOW_RANGE);
            }
        } catch (Exception ignored) {}

        Vec3 lookDir = aiSource.getLookAngle();

        boolean isBeingTempted = isTemptGoalRunning(aiSource);

        return new EntityAIData(
                entity.getId(),
                new Vec3(entity.getX(), entity.getY(), entity.getZ()),
                entity.getBbHeight(),
                category,
                pathNodes,
                nodeIndex,
                primaryGoal,
                goalsArray,
                targetPos,
                followRange,
                lookDir,
                isBeingTempted
        );
    }

    // Returns the server-side Mob from the integrated server, or null if unavailable
    private static Mob getServerMob(int entityId, Minecraft client) {
        if (client.getSingleplayerServer() == null || client.level == null) return null;
        try {
            ServerLevel serverLevel = client.getSingleplayerServer().getLevel(client.level.dimension());
            if (serverLevel == null) return null;
            Entity e = serverLevel.getEntity(entityId);
            if (e instanceof Mob mob) return mob;
        } catch (Exception ignored) {}
        return null;
    }

    // Checks by instanceof so it works regardless of name mappings
    private static boolean isTemptGoalRunning(Mob mob) {
        try {
            MobEntityAccessor accessor = (MobEntityAccessor) mob;
            GoalSelector selector = accessor.getGoalSelector();
            if (selector == null) return false;
            for (WrappedGoal pg : selector.getAvailableGoals()) {
                if (pg.isRunning() && pg.getGoal() instanceof TemptGoal) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static EntityAIData.EntityCategory categorize(Mob entity) {
        if (entity instanceof Villager || entity instanceof WanderingTrader) {
            return EntityAIData.EntityCategory.VILLAGER;
        }
        if (entity instanceof Monster) {
            return EntityAIData.EntityCategory.HOSTILE;
        }
        return EntityAIData.EntityCategory.PASSIVE;
    }
}
