package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.advancements.criterion.FishingRodHookedTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(FishingRodHookedTrigger.class)
public class FishingMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void r3ct_onFishingHooked(ServerPlayer player, ItemStack rod, FishingHook entity, Collection<ItemStack> loots, CallbackInfo ci) {
        MobProgressHandler.handlePlayerFishedItems(player, loots);
    }
}