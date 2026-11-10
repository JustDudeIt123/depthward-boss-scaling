package net.sixik.bossscalingsystem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
@Mod("bossscalingsystem")
public final class Bossscalingsystem {
    public Bossscalingsystem() { MinecraftForge.EVENT_BUS.register(new ScalingEvents()); }
}
