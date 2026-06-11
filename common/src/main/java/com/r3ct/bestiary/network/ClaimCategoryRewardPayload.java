package com.r3ct.bestiary.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClaimCategoryRewardPayload(String tabId) implements CustomPacketPayload {
    public static final Type<ClaimCategoryRewardPayload> TYPE = new Type<>(Identifier.parse("r3ct_bestiary:claim_reward"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimCategoryRewardPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClaimCategoryRewardPayload::tabId,
            ClaimCategoryRewardPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}