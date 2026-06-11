package com.r3ct.bestiary.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.List;

public record SyncDataPayload(List<String> unlockedItems, List<String> rewardedCategories) implements CustomPacketPayload {
    public static final Type<SyncDataPayload> TYPE = new Type<>(Identifier.parse("r3ct_bestiary:sync_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncDataPayload::unlockedItems,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncDataPayload::rewardedCategories,
            SyncDataPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}