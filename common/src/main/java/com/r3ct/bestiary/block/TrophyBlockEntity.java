package com.r3ct.bestiary.block;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class TrophyBlockEntity extends BlockEntity {

    private Component customName;
    private String displayEntityId = "";
    private String ownerName = "";

    private List<String> entityList = new ArrayList<>();

    private Entity displayEntityCache = null;

    public TrophyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
        this.setChanged();
        syncToClient();
    }

    public Component getCustomName() {
        return this.customName;
    }

    public void setDisplayEntityId(String entityId) {
        this.displayEntityId = entityId;
        this.displayEntityCache = null; // Resetujemy cache
        this.setChanged();
        syncToClient();
    }

    public String getDisplayEntityId() {
        return this.displayEntityId;
    }

    // Dodaj to gdzieś pod getDisplayEntityId()
    public List<String> getEntityList() {
        return this.entityList;
    }

    public Entity getOrCreateDisplayEntity() {
        if (this.level == null) return null;
        if (this.displayEntityCache == null && this.displayEntityId != null && !this.displayEntityId.isEmpty()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(this.displayEntityId)).map(net.minecraft.core.Holder::value).orElse(null);
            if (type != null) {
                // Tworzymy fałszywą encję w trybie odczytu (LOAD) - nie istnieje ona fizycznie na serwerze!
                this.displayEntityCache = type.create(this.level, EntitySpawnReason.LOAD);
            }
        }
        return this.displayEntityCache;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        this.setChanged();
        syncToClient();
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    private void syncToClient() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void cycleDisplayEntity() {
        if (this.entityList == null || this.entityList.isEmpty()) return;

        int currentIndex = this.entityList.indexOf(this.displayEntityId);
        // Jeśli nie znaleziono lub jest ostatni, wracamy do zera, w przeciwnym razie bierzemy następnego
        int nextIndex = (currentIndex == -1 || currentIndex >= this.entityList.size() - 1) ? 0 : currentIndex + 1;

        setDisplayEntityId(this.entityList.get(nextIndex));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.customName != null) {
            var ops = this.level != null ? this.level.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE;
            ComponentSerialization.CODEC.encodeStart(ops, this.customName)
                    .result()
                    .ifPresent(jsonElement -> output.putString("CustomName", jsonElement.toString()));
        }

        if (this.displayEntityId != null && !this.displayEntityId.isEmpty()) {
            output.putString("DisplayEntity", this.displayEntityId);
        }
        if (this.ownerName != null && !this.ownerName.isEmpty()) {
            output.putString("OwnerName", this.ownerName);
        }
        if (!this.entityList.isEmpty()) {
            output.putString("EntityListString", String.join(",", this.entityList));
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

        input.getString("DisplayEntity").ifPresent(id -> this.displayEntityId = id);
        input.getString("OwnerName").ifPresent(name -> this.ownerName = name);
        input.getString("EntityListString").ifPresent(str -> {
            this.entityList.clear();
            if (!str.isEmpty()) {
                this.entityList.addAll(List.of(str.split(",")));
            }
        });
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        this.customName = input.get(DataComponents.CUSTOM_NAME);
        CustomData customData = input.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            String entity = tag.getString("DisplayEntity").orElse("");
            if (!entity.isEmpty()) this.displayEntityId = entity;

            String owner = tag.getString("OwnerName").orElse("");
            if (!owner.isEmpty()) this.ownerName = owner;

            if (tag.contains("EntityList")) {
                Tag rawTag = tag.get("EntityList");
                if (rawTag instanceof ListTag list) {
                    this.entityList.clear();
                    for (int i = 0; i < list.size(); i++) {
                        list.getString(i).ifPresent(str -> this.entityList.add(str));
                    }
                }
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.customName != null) {
            components.set(DataComponents.CUSTOM_NAME, this.customName);
        }

        CompoundTag tag = new CompoundTag();
        boolean hasData = false;

        if (this.displayEntityId != null && !this.displayEntityId.isEmpty()) {
            tag.putString("DisplayEntity", this.displayEntityId);
            hasData = true;
        }
        if (this.ownerName != null && !this.ownerName.isEmpty()) {
            tag.putString("OwnerName", this.ownerName);
            hasData = true;
        }
        if (!this.entityList.isEmpty()) {
            ListTag list = new ListTag();
            for (String id : this.entityList) {
                list.add(StringTag.valueOf(id));
            }
            tag.put("EntityList", list);
            hasData = true;
        }

        if (hasData) {
            components.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}