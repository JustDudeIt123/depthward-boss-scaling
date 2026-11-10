package net.sixik.bossscalingsystem;
import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
public final class ReloadContainer {
    public static final ReloadContainer INSTANCE = new ReloadContainer();
    public final HashMap<ResourceLocation, MobConfig> MOB_CONFIG = new HashMap<>();
    private ReloadContainer() {}
}
