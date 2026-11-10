package net.sixik.bossscalingsystem.compact;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.sixik.bossscalingsystem.*;
public final class KJSPlugin extends KubeJSPlugin {
    @Override public void registerBindings(BindingsEvent event) {
        if (event.getType().isServer()) {
            // Bindings are created once per server-script load, BEFORE scripts run.
            // onServerReload runs AFTER scripts and must not clear their registrations.
            ReloadContainer.INSTANCE.MOB_CONFIG.clear();
            event.add("BossScalingSystem", Methods.class);
        }
    }
    public interface Methods {
        static void addEntityConfig(ResourceLocation id, ItemStack[] sharedItems, float scale) {
            ReloadContainer.INSTANCE.MOB_CONFIG.put(id, new MobConfig(id).addItem(sharedItems).setScalePerPlayer(scale));
        }
    }
}
