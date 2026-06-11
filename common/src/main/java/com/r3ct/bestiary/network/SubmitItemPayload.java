package com.r3ct.bestiary.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SubmitItemPayload(String itemId, int slotId) implements CustomPacketPayload {

    public static final Type<SubmitItemPayload> TYPE = new Type<>(Identifier.parse("r3ct_bestiary:submit_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitItemPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SubmitItemPayload::itemId,
            ByteBufCodecs.VAR_INT, SubmitItemPayload::slotId,
            SubmitItemPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}