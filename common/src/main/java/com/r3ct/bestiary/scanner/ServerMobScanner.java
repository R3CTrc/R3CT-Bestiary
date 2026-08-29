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

                    Identifier lootTableId = null;

                    if (living instanceof Mob mob) {
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.COMMAND, null);
                        lootTableId = mob.getLootTable().get().identifier();
                    } else {
                        lootTableId = type.getDefaultLootTable().get().identifier();
                    }

                    float hp = living.getMaxHealth();

                    List<String> drops = scanLootTable(level, lootTableId, new HashSet<>(), new HashSet<>());

                    CACHED_STATS.put(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                            new MobBaseStats(hp, living.getMainHandItem().copy(), drops));

                    living.discard();
                }
            } catch (Exception ignored) {}
        }
        return CACHED_STATS;
    }

    private static List<String> scanLootTable(ServerLevel level, Identifier location, Set<String> items, Set<Identifier> visited) {
        if (location == null || !visited.add(location)) return new ArrayList<>(items);

        try {
            Identifier resourcePath = Identifier.parse(location.getNamespace() + ":loot_table/" + location.getPath() + ".json");
            var resourceOpt = level.getServer().getResourceManager().getResource(resourcePath);

            if (resourceOpt.isPresent()) {
                try (Reader reader = new InputStreamReader(resourceOpt.get().open())) {
                    JsonElement json = JsonParser.parseReader(reader);
                    extractItemsFromJson(json, items, level, visited);
                }
            }
        } catch (Exception ignored) {}

        return new ArrayList<>(items);
    }

    private static void extractItemsFromJson(JsonElement element, Set<String> items, ServerLevel level, Set<Identifier> visited) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("name")) {
                String name = obj.get("name").getAsString();

                if (obj.has("type") && obj.get("type").getAsString().equals("minecraft:loot_table")) {
                    scanLootTable(level, Identifier.parse(name), items, visited);
                } else {
                    if (BuiltInRegistries.ITEM.containsKey(Identifier.parse(name)) && !name.equals("minecraft:air")) {
                        items.add(name);
                    }
                }
            }

            if (obj.has("item")) {
                String item = obj.get("item").getAsString();
                if (BuiltInRegistries.ITEM.containsKey(Identifier.parse(item)) && !item.equals("minecraft:air")) {
                    items.add(item);
                }
            }

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                extractItemsFromJson(entry.getValue(), items, level, visited);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                extractItemsFromJson(el, items, level, visited);
            }
        }
    }
}