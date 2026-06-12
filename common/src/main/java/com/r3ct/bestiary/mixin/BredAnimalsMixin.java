package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class BredAnimalsMixin {

    @Inject(method = "spawnChildFromBreeding", at = @At("RETURN"))
    private void onBreed(ServerLevel level, Animal mate, CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;
        ServerPlayer player = animal.getLoveCause();

        // Dodatkowe zabezpieczenie: jeśli pierwszy rodzic zgubił gracza, pytamy drugiego
        if (player == null && mate != null) {
            player = mate.getLoveCause();
        }

        if (player != null) {
            EntityType<?> targetType = animal.getType();

            // Specjalny przypadek: Koń + Osioł = Muł
            if ((animal.getType() == EntityType.HORSE && mate.getType() == EntityType.DONKEY) ||
                    (animal.getType() == EntityType.DONKEY && mate.getType() == EntityType.HORSE)) {
                targetType = EntityType.MULE;
            }

            MobProgressHandler.handleMobBreed(player, targetType);
        }
    }
}