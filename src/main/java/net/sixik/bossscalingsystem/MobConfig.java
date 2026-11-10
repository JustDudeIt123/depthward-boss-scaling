package net.sixik.bossscalingsystem;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
/** items are shared (not duplicated) drops, retained for compatibility with 2.0.1. */
public final class MobConfig {
    public final ResourceLocation modID;
    public final List<ItemStack> items = new ArrayList<>();
    public float scalePerPlayer = 2.0f;
    public MobConfig(ResourceLocation id) { modID = id; }
    public MobConfig addItem(ItemStack... stacks) { for (var s : stacks) items.add(s.copy()); return this; }
    public MobConfig setScalePerPlayer(float scale) {
        if (!Float.isFinite(scale) || scale < 0) throw new IllegalArgumentException("scale must be finite and >= 0");
        scalePerPlayer = scale; return this;
    }
}
