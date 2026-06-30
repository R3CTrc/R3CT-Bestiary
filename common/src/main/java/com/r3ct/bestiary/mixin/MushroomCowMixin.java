package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin {

    @Unique private boolean r3ct_hadBowl = false;

    // Sprawdzamy, czy gracz trzyma pustą miskę
    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void r3ct_checkBowl(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.r3ct_hadBowl = player.getItemInHand(hand).is(Items.BOWL);
    }

    // Jeśli gra zaakceptowała akcję (gracz otrzymał zupę)
    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void r3ct_onStew(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer && this.r3ct_hadBowl) {

            MushroomCow mooshroom = (MushroomCow) (Object) this;
            long cooldownMs = (long) BestiaryConfig.interactionCooldownMinutes * 60 * 1000L;

            if (MobProgressHandler.tryApplyCooldown(mooshroom, cooldownMs)) {
                MobProgressHandler.handleMobInteract(serverPlayer, EntityType.MOOSHROOM);
            }
        }
    }
}