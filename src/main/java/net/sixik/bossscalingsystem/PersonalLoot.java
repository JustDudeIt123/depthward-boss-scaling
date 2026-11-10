package net.sixik.bossscalingsystem;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
public final class PersonalLoot {
    public static final String OWNER = "BossScalingSystemOwner";
    private PersonalLoot() {}
    public static UUID owner(Entity e) { var n = e.getPersistentData(); return n.hasUUID(OWNER) ? n.getUUID(OWNER) : null; }
    public static boolean mayAccess(Entity e, UUID player) { UUID owner = owner(e); return owner == null || owner.equals(player); }
}
