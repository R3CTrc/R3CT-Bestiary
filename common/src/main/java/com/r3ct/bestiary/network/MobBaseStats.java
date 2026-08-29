package com.r3ct.bestiary.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record MobBaseStats(
        float maxHealth,
        ItemStack mainHandItem,
        List<String> drops
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MobBaseStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, MobBaseStats::maxHealth,
            ItemStack.OPTIONAL_STREAM_CODEC, MobBaseStats::mainHandItem,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), MobBaseStats::drops,
            MobBaseStats::new
    );
}