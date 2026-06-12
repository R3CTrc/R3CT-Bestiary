package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.inventory.MerchantResultSlot.class)
public abstract class TradeMixin {

    @Shadow @Final private Player player;
    @Shadow @Final private Merchant merchant;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void onTradeResult(ItemStack stack, CallbackInfo ci) {
        if (this.player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            if (this.merchant instanceof Entity merchantEntity) {
                MobProgressHandler.handleMobTrade(serverPlayer, merchantEntity.getType());
            }
        }
    }
}