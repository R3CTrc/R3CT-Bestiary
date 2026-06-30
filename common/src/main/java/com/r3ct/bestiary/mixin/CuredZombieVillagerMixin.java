package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.advancements.criterion.CuredZombieVillagerTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CuredZombieVillagerTrigger.class)
public abstract class CuredZombieVillagerMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void r3ct_onZombieCured(ServerPlayer player, Zombie zombie, Villager villager, CallbackInfo ci) {
        MobProgressHandler.handleMobCure(player, EntityType.ZOMBIE_VILLAGER);
    }
}