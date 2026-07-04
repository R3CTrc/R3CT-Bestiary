package com.r3ct.bestiary.logic;

import com.r3ct.bestiary.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class ModTags {
    public static final TagKey<EntityType<?>> ATTACK_EXPLOSIVE = create("attack_type/explosive");
    public static final TagKey<EntityType<?>> ATTACK_MAGIC = create("attack_type/magic");
    public static final TagKey<EntityType<?>> ATTACK_SONIC = create("attack_type/sonic");

    public static final TagKey<EntityType<?>> C_BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("c:bosses"));

    private static TagKey<EntityType<?>> create(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(Constants.MOD_ID + ":" + name));
    }
}