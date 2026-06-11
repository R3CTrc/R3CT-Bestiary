package com.r3ct.bestiary.block;

import com.r3ct.bestiary.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":" + name));
    }

    private static BlockBehaviour.Properties trophyProps(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of()
                .setId(key)
                .noOcclusion()
                .isValidSpawn((state, getter, pos, entityType) -> false)
                .isViewBlocking((state, getter, pos) -> false)
                .strength(1.0f)
                .sound(SoundType.GLASS);
    }

    public static final Block TROPHY_BUILDING = new TrophyBlock(trophyProps(key("trophy_building")));
    public static final Block TROPHY_COMBAT = new TrophyBlock(trophyProps(key("trophy_combat")));
    public static final Block TROPHY_TOOLS = new TrophyBlock(trophyProps(key("trophy_tools")));
    public static final Block TROPHY_FOOD = new TrophyBlock(trophyProps(key("trophy_food")));
    public static final Block TROPHY_REDSTONE = new TrophyBlock(trophyProps(key("trophy_redstone")));
    public static final Block TROPHY_INGREDIENTS = new TrophyBlock(trophyProps(key("trophy_ingredients")));
    public static final Block TROPHY_NATURAL = new TrophyBlock(trophyProps(key("trophy_natural")));
    public static final Block TROPHY_COLORED = new TrophyBlock(trophyProps(key("trophy_colored")));
    public static final Block TROPHY_EGG = new TrophyBlock(trophyProps(key("trophy_egg")));
    public static final Block TROPHY_FUNCTIONAL = new TrophyBlock(trophyProps(key("trophy_functional")));
    public static final Block TROPHY_MOD = new TrophyBlock(trophyProps(key("trophy_mod")));

    public static final ResourceKey<BlockEntityType<?>> TROPHY_BE_KEY = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.parse(Constants.MOD_ID + ":trophy_building_be")
    );

    public static final BlockEntityType<TrophyBlockEntity> TROPHY_BE_TYPE = com.r3ct.bestiary.platform.Services.PLATFORM.createBlockEntityType(
            (pos, state) -> new TrophyBlockEntity(ModBlocks.TROPHY_BE_TYPE, pos, state),
            TROPHY_BUILDING, TROPHY_COMBAT, TROPHY_TOOLS, TROPHY_FOOD, TROPHY_REDSTONE,
            TROPHY_INGREDIENTS, TROPHY_NATURAL, TROPHY_COLORED, TROPHY_EGG, TROPHY_FUNCTIONAL, TROPHY_MOD
    );
}