package net.sixik.bossscalingsystem;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.*;
public final class ScalingEvents {
    public static final String STATE = "BossScalingSystem";
    private static final UUID HEALTH_MODIFIER = UUID.fromString("d7dd37a1-e5ee-47db-bc97-65cb0454cc2c");
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void spawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || event.loadedFromDisk() || !(event.getEntity() instanceof Mob mob)) return;
        var config = ReloadContainer.INSTANCE.MOB_CONFIG.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
        if (config == null || mob.getPersistentData().contains(STATE)) return;
        var players = event.getLevel().players().stream().filter(p -> eligible(p, mob)).toList();
        initialize(mob, config, players.stream().map(Entity::getUUID).toList());
    }
    public static boolean eligible(net.minecraft.world.entity.player.Player player, Entity mob) {
        return player.isAlive() && !player.isSpectator() && player.distanceToSqr(mob) <= 1600.0;
    }
    public static void initialize(Mob mob, MobConfig config, List<UUID> players) {
        if (mob.getPersistentData().contains(STATE)) return;
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        var state = new CompoundTag();
        var recipients = new ListTag();
        players.stream().distinct().forEach(id -> { var tag = new CompoundTag(); tag.putUUID("UUID", id); recipients.add(tag); });
        state.put("Players", recipients);
        var shared = new ListTag(); for (var item : config.items) shared.add(item.save(new CompoundTag()));
        state.put("Shared", shared);
        mob.getPersistentData().put(STATE, state);
        double extra = config.scalePerPlayer * Math.max(0, recipients.size() - 1);
        if (extra > 0) health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER, "Boss scaling players", extra, AttributeModifier.Operation.MULTIPLY_TOTAL));
        mob.setHealth(mob.getMaxHealth());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void drops(LivingDropsEvent event) {
        var mob = event.getEntity();
        if (mob.level().isClientSide || !mob.getPersistentData().contains(STATE)) return;
        var state = mob.getPersistentData().getCompound(STATE);
        var recipients = state.getList("Players", Tag.TAG_COMPOUND);
        // No players at spawn: keep vanilla drops instead of silently deleting them.
        if (recipients.isEmpty()) return;
        var shared = state.getList("Shared", Tag.TAG_COMPOUND);
        var original = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        for (var drop : original) {
            boolean isShared = false;
            for (var n : shared) if (ItemStack.isSameItemSameTags(drop.getItem(), ItemStack.of((CompoundTag)n))) { isShared = true; break; }
            // Another mod may already supply personal drops. Do not duplicate those again.
            if (isShared || PersonalLoot.owner(drop) != null) { event.getDrops().add(drop); continue; }
            for (var n : recipients) {
                var id = ((CompoundTag)n).getUUID("UUID");
                var copy = new ItemEntity(mob.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
                copy.setDeltaMovement(drop.getDeltaMovement()); copy.setDefaultPickUpDelay();
                copy.getPersistentData().putUUID(PersonalLoot.OWNER, id);
                event.getDrops().add(copy);
            }
        }
    }
}
