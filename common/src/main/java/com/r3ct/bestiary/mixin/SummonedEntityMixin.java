package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.advancements.criterion.SummonedEntityTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SummonedEntityTrigger.class)
public abstract class SummonedEntityMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void onEntitySummoned(ServerPlayer player, Entity entity, CallbackInfo ci) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String bestiaryCat = MobProgressHandler.getBestiaryCategory(entityId, entity.getType().getCategory());

        if (!bestiaryCat.equals("bosses")) {
            MobProgressHandler.handleMobBuild(player, entity.getType());
        }
    }
}