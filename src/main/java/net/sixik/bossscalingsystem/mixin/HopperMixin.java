package net.sixik.bossscalingsystem.mixin;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.sixik.bossscalingsystem.PersonalLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(HopperBlockEntity.class)
public abstract class HopperMixin {
    @Inject(method="addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at=@At("HEAD"), cancellable=true)
    private static void boss$hopper(Container container, ItemEntity item, CallbackInfoReturnable<Boolean> ci) {
        if (PersonalLoot.owner(item) != null) ci.setReturnValue(false);
    }
}
