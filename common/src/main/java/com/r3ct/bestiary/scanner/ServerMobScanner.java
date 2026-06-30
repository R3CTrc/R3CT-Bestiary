package com.r3ct.bestiary.scanner;

import com.r3ct.bestiary.network.MobBaseStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;

public class ServerMobScanner {

    private static Map<String, MobBaseStats> CACHED_STATS = null;

    public static Map<String, MobBaseStats> getServerMobStats(ServerLevel level) {
        if (CACHED_STATS != null) {
            return CACHED_STATS;
        }

        CACHED_STATS = new HashMap<>();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            try {
                Entity entity = type.create(level, EntitySpawnReason.COMMAND);
                if (entity instanceof LivingEntity living) {

                    if (living instanceof Mob mob) {
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.COMMAND, null);
                    }

                    float hp = living.getMaxHealth();
                    int armor = living.getArmorValue();
                    double speed = living.getAttributeValue(Attributes.MOVEMENT_SPEED);

                    double attack = 0;
                    var attackAttr = living.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (attackAttr != null) attack = attackAttr.getValue();

                    double range = 16;
                    var rangeAttr = living.getAttribute(Attributes.FOLLOW_RANGE);
                    if (rangeAttr != null) range = rangeAttr.getValue();

                    float kb = 0;
                    var kbAttr = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                    if (kbAttr != null) kb = (float) kbAttr.getValue();

                    CACHED_STATS.put(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                            new MobBaseStats(hp, armor, speed, attack, range, kb, living.fireImmune(), living.getMainHandItem().copy()));

                    living.discard();
                }
            } catch (Exception ignored) {}
        }
        return CACHED_STATS;
    }
}