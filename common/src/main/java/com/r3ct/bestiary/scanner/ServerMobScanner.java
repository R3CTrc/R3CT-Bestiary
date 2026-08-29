package com.r3ct.bestiary.scanner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.r3ct.bestiary.network.MobBaseStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

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

                    // Zbieramy TYLKO to, co nas teraz interesuje
                    float hp = living.getMaxHealth();
                    List<String> drops = scanLootTablesForDrops(level, type);

                    CACHED_STATS.put(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                            new MobBaseStats(hp, living.getMainHandItem().copy(), drops));

                    living.discard();
                }
            } catch (Exception ignored) {}
        }
        return CACHED_STATS;
    }

    // NASZ NOWY, SZYBKI SKANER JSON
    private static List<String> scanLootTablesForDrops(ServerLevel level, EntityType<?> type) {
        Set<String> drops = new HashSet<>();
        try {
            Identifier location = type.getDefaultLootTable().get().identifier();
            Identifier resourcePath = Identifier.parse(location.getNamespace() + ":loot_table/" + location.getPath() + ".json");

            var resourceOpt = level.getServer().getResourceManager().getResource(resourcePath);
            if (resourceOpt.isPresent()) {
                try (Reader reader = new InputStreamReader(resourceOpt.get().open())) {
                    JsonElement json = JsonParser.parseReader(reader);
                    extractItemsFromJson(json, drops);
                }
            }
        } catch (Exception ignored) {}

        return new ArrayList<>(drops);
    }

    private static void extractItemsFromJson(JsonElement element, Set<String> items) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("type") && obj.get("type").getAsString().equals("minecraft:item")) {
                if (obj.has("name")) {
                    items.add(obj.get("name").getAsString());
                }
            }
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                extractItemsFromJson(entry.getValue(), items);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                extractItemsFromJson(el, items);
            }
        }
    }
}