package net.sixik.bossscalingsystem.mixin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.sixik.bossscalingsystem.PersonalLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method="playerTouch", at=@At("HEAD"), cancellable=true)
    private void boss$pickup(Player player, CallbackInfo ci) {
        if (!PersonalLoot.mayAccess((ItemEntity)(Object)this, player.getUUID())) ci.cancel();
    }
    @Inject(method="tryToMerge", at=@At("HEAD"), cancellable=true)
    private void boss$merge(ItemEntity other, CallbackInfo ci) {
        if (PersonalLoot.owner((ItemEntity)(Object)this) != null || PersonalLoot.owner(other) != null) ci.cancel();
    }
}
