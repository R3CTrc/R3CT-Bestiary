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

    public static final ResourceKey<Block> TROPHY_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.parse(Constants.MOD_ID + ":trophy")
    );

    public static final Block TROPHY = new TrophyBlock(BlockBehaviour.Properties.of()
            .setId(TROPHY_KEY)
            .noOcclusion()
            .isValidSpawn((state, getter, pos, entityType) -> false)
            .isViewBlocking((state, getter, pos) -> false)
            .strength(1.0f)
            .sound(SoundType.NETHERITE_BLOCK));

    public static final ResourceKey<BlockEntityType<?>> TROPHY_BE_KEY = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.parse(Constants.MOD_ID + ":trophy_be")
    );

    public static final BlockEntityType<TrophyBlockEntity> TROPHY_BE_TYPE = com.r3ct.bestiary.platform.Services.PLATFORM.createBlockEntityType(
            (pos, state) -> new TrophyBlockEntity(ModBlocks.TROPHY_BE_TYPE, pos, state),
            TROPHY
    );
}