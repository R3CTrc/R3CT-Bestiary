package com.r3ct.bestiary.block;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TrophyBlockEntity extends BlockEntity {

    private Component customName;

    public TrophyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public Component getCustomName() {
        return this.customName;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.customName != null) {
            var ops = this.level != null ? this.level.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE;
            ComponentSerialization.CODEC.encodeStart(ops, this.customName)
                    .result()
                    .ifPresent(jsonElement -> {
                        output.putString("CustomName", jsonElement.toString());
                    });
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getString("CustomName").ifPresent(jsonStr -> {
            try {
                var ops = this.level != null ? this.level.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE;
                var jsonElement = JsonParser.parseString(jsonStr);
                ComponentSerialization.CODEC.parse(ops, jsonElement)
                        .result()
                        .ifPresent(name -> this.customName = name);
            } catch (Exception e) {}
        });
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter input) {
        super.applyImplicitComponents(input);
        this.customName = input.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.customName != null) {
            components.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, this.customName);
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}