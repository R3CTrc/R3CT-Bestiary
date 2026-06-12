package com.r3ct.bestiary.scanner;

import com.r3ct.bestiary.config.BestiaryConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EntityTypeScanner {

    public static final Map<String, CategoryData> SCANNED_CATEGORIES = new LinkedHashMap<>();

    public static class CategoryData {
        public final String categoryId;
        public final String namespace;
        public final String type;
        public final List<String> entityIds;

        public CategoryData(String categoryId, String namespace, String type) {
            this.categoryId = categoryId;
            this.namespace = namespace;
            this.type = type;
            this.entityIds = new ArrayList<>();
        }

        public String getFormattedModName() {
            if (namespace.equals("minecraft")) return "Minecraft";

            String[] parts = namespace.split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
                }
            }
            return sb.toString().trim();
        }
    }

    public static void scanEntities() {
        SCANNED_CATEGORIES.clear();

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = entityType.getCategory();
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            String idString = id.toString();
            String namespace = id.getNamespace();
            String internalCategoryName = category.getName();

            boolean isAllowedMisc = (entityType == EntityType.VILLAGER ||
                    entityType == EntityType.IRON_GOLEM ||
                    entityType == EntityType.SNOW_GOLEM ||
                    entityType == EntityType.COPPER_GOLEM);

            // Blokujemy śmieciowe byty (strzały, ramki na przedmioty itp.)
            // Wyjątek: dozwolone żywe moby LUB jakikolwiek mob nadpisany przez Admina w configu!
            if (category == MobCategory.MISC && !isAllowedMisc && !BestiaryConfig.mobCategoryOverrides.containsKey(idString)) {
                continue;
            }

            // --- BLACKLISTY ---
            if (BestiaryConfig.blacklistedMods.contains(namespace)) continue;
            if (BestiaryConfig.blacklistedCategories.contains(internalCategoryName)) continue;
            if (BestiaryConfig.blacklistedMobs.contains(idString)) continue;

            String type;
            String mobOverride = BestiaryConfig.mobCategoryOverrides.get(idString);
            String modOverride = BestiaryConfig.modCategoryOverrides.get(namespace);

            // 1. Priorytet: Nadpisanie konkretnego moba (np. "minecraft:ender_dragon": "bosses")
            if (mobOverride != null && !mobOverride.isEmpty()) {
                type = mobOverride;
            }
            // 2. Priorytet: Nadpisanie całego moda (np. "cataclysm": "bosses")
            else if (modOverride != null && !modOverride.isEmpty()) {
                type = modOverride;
            }
            // 3. Domyślny podział: Jeśli gra uznaje go za Potwora (MONSTER)
            else if (category == MobCategory.MONSTER) {
                type = "monsters";
            }
            // 4. Fallback: Wszystko inne (Zwierzęta, Ryby, Nietoperze, Golemy) trafia do Stworzeń
            else {
                type = "creatures";
            }

            String categoryId = namespace + ":" + type;

            SCANNED_CATEGORIES.computeIfAbsent(categoryId, k -> new CategoryData(k, namespace, type)).entityIds.add(idString);
        }

        com.r3ct.bestiary.Constants.LOG.info("Bestiary Scanner: Successfully loaded {} mob categories.", SCANNED_CATEGORIES.size());
    }
}