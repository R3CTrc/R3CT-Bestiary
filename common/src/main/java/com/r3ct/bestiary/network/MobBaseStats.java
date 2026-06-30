package com.r3ct.bestiary.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record MobBaseStats(
        float maxHealth,
        int armor,
        double speed,
        double attackDamage,
        double followRange,
        float knockbackResistance,
        boolean fireImmune,
        ItemStack mainHandItem
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MobBaseStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, MobBaseStats::maxHealth,
            ByteBufCodecs.VAR_INT, MobBaseStats::armor,
            ByteBufCodecs.DOUBLE, MobBaseStats::speed,
            ByteBufCodecs.DOUBLE, MobBaseStats::attackDamage,
            ByteBufCodecs.DOUBLE, MobBaseStats::followRange,
            ByteBufCodecs.FLOAT, MobBaseStats::knockbackResistance,
            ByteBufCodecs.BOOL, MobBaseStats::fireImmune,
            ItemStack.OPTIONAL_STREAM_CODEC, MobBaseStats::mainHandItem,
            MobBaseStats::new
    );
}