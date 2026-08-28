package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource damageSource, CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            if (damageSource.getEntity() instanceof ServerPlayer player) {
                MobProgressHandler.handleMobKill(player, this.getType());
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void r3ct_bestiary$onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || amount <= 0.0F) return;
        LivingEntity victim = (LivingEntity) (Object) this;
        Entity attackerEntity = source.getEntity();
        if (attackerEntity instanceof ServerPlayer player && victim instanceof Mob mob) {
            MobProgressHandler.handleMobInteract(player, mob.getType());
        }
    }
}