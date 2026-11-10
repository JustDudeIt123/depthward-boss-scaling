package net.sixik.bossscalingsystem.mixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.sixik.bossscalingsystem.PersonalLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method="broadcastToPlayer", at=@At("HEAD"), cancellable=true)
    private void boss$visible(ServerPlayer player, CallbackInfoReturnable<Boolean> ci) {
        if (!PersonalLoot.mayAccess((Entity)(Object)this, player.getUUID())) ci.setReturnValue(false);
    }
}
