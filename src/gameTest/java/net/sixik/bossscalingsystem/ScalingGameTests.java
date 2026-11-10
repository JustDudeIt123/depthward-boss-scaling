package net.sixik.bossscalingsystem;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
@GameTestHolder("bossscalingsystem") @PrefixGameTestTemplate(false)
public final class ScalingGameTests {
    private static Zombie mob(GameTestHelper t) { return new Zombie(EntityType.ZOMBIE,t.getLevel()); }
    private static MobConfig config(float scale) { return new MobConfig(new ResourceLocation("minecraft:zombie")).setScalePerPlayer(scale); }
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper t) { return FakePlayerFactory.get(t.getLevel(),new GameProfile(UUID.randomUUID(),"BossTest")); }
    @GameTest(template="empty") public static void healthAndIdempotence(GameTestHelper t) {
        var m=mob(t); ScalingEvents.initialize(m,config(1),List.of(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID()));
        t.assertTrue(m.getMaxHealth()==60 && m.getHealth()==60,"3 players should give 60 HP");
        ScalingEvents.initialize(m,config(1),List.of(UUID.randomUUID()));
        t.assertTrue(m.getMaxHealth()==60,"Repeated initialization must not scale twice");t.succeed();
    }
    @GameTest(template="empty") public static void healthVariants(GameTestHelper t) {
        for(int count=0;count<5;count++) { var m=mob(t);var ids=new ArrayList<UUID>();for(int i=0;i<count;i++)ids.add(UUID.randomUUID());
            ScalingEvents.initialize(m,config(0.5f),ids);t.assertTrue(m.getMaxHealth()==20*(1+0.5*Math.max(0,count-1)),"HP formula for "+count); }
        try {config(Float.NaN);t.fail("NaN accepted");}catch(IllegalArgumentException expected){}
        try {config(-1);t.fail("negative accepted");}catch(IllegalArgumentException expected){}
        t.succeed();
    }
    @GameTest(template="empty") public static void radiusIsSphere(GameTestHelper t) {
        var m=mob(t);m.setPos(0,100,0);var p=player(t);p.setPos(40,100,0);
        t.assertTrue(ScalingEvents.eligible(p,m),"40 inclusive");p.setPos(40.01,100,0);t.assertTrue(!ScalingEvents.eligible(p,m),"40.01 excluded");
        p.setPos(39,139,39);t.assertTrue(!ScalingEvents.eligible(p,m),"Cube corner excluded");
        p.setPos(0,100,0);p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);t.assertTrue(!ScalingEvents.eligible(p,m),"Spectators excluded");t.succeed();
    }
    @GameTest(template="empty") public static void persistentMobState(GameTestHelper t) {
        var m=mob(t);var ids=List.of(UUID.randomUUID(),UUID.randomUUID());ScalingEvents.initialize(m,config(1),ids);
        var tag=new CompoundTag();m.save(tag);var restored=EntityType.loadEntityRecursive(tag,t.getLevel(),e->e);
        t.assertTrue(restored instanceof Zombie && ((Zombie)restored).getMaxHealth()==40,"HP survives reload");
        t.assertTrue(restored.getPersistentData().getCompound(ScalingEvents.STATE).getList("Players",10).size()==2,"Recipients survive reload");t.succeed();
    }
    @GameTest(template="empty") public static void duplicateDropsAreIndependent(GameTestHelper t) {
        var m=mob(t);var a=UUID.randomUUID();var b=UUID.randomUUID();ScalingEvents.initialize(m,config(1),List.of(a,b));
        var stack=new ItemStack(Items.DIAMOND,3);stack.setHoverName(net.minecraft.network.chat.Component.literal("Reward"));stack.getOrCreateTag().putInt("Custom",42);
        var drops=new ArrayList<ItemEntity>();drops.add(new ItemEntity(t.getLevel(),0,100,0,stack));
        var event=new LivingDropsEvent(m,t.getLevel().damageSources().generic(),drops,0,false);new ScalingEvents().drops(event);
        t.assertTrue(drops.size()==2,"One identical copy per spawn recipient even without player killer");
        t.assertTrue(ItemStack.matches(drops.get(0).getItem(),drops.get(1).getItem()),"Names and NBT preserved");
        t.assertTrue(!PersonalLoot.owner(drops.get(0)).equals(PersonalLoot.owner(drops.get(1))),"Distinct owners");
        drops.get(0).getItem().shrink(1);t.assertTrue(drops.get(1).getItem().getCount()==3,"Stacks must not alias");t.succeed();
    }
    @GameTest(template="empty") public static void visibilityPickupAndPersistence(GameTestHelper t) {
        var a=player(t);var b=player(t);var drop=new ItemEntity(t.getLevel(),0,100,0,new ItemStack(Items.DIAMOND));drop.setNoPickUpDelay();drop.getPersistentData().putUUID(PersonalLoot.OWNER,a.getUUID());
        t.assertTrue(drop.broadcastToPlayer(a) && !drop.broadcastToPlayer(b),"Only owner receives entity tracking");
        drop.playerTouch(b);t.assertTrue(b.getInventory().countItem(Items.DIAMOND)==0 && !drop.isRemoved(),"Other player cannot pick up");
        var tag=new CompoundTag();drop.save(tag);var restored=(ItemEntity)EntityType.loadEntityRecursive(tag,t.getLevel(),e->e);
        t.assertTrue(PersonalLoot.owner(restored).equals(a.getUUID()) && !restored.broadcastToPlayer(b),"Ownership survives reload");
        drop.playerTouch(a);t.assertTrue(a.getInventory().countItem(Items.DIAMOND)==1,"Owner can pick up");t.succeed();
    }
    @GameTest(template="empty") public static void hoppersCannotSteal(GameTestHelper t) {
        var drop=new ItemEntity(t.getLevel(),0,100,0,new ItemStack(Items.DIAMOND));drop.getPersistentData().putUUID(PersonalLoot.OWNER,UUID.randomUUID());var inv=new SimpleContainer(5);
        t.assertTrue(!HopperBlockEntity.addItem(inv,drop) && inv.isEmpty(),"Hopper cannot collect personal drop");
        drop.getPersistentData().remove(PersonalLoot.OWNER);t.assertTrue(HopperBlockEntity.addItem(inv,drop),"Ordinary drops unaffected");t.succeed();
    }
    @GameTest(template="empty") public static void sharedAndZeroPlayers(GameTestHelper t) {
        var m=mob(t);var cfg=config(1).addItem(new ItemStack(Items.DIAMOND));ScalingEvents.initialize(m,cfg,List.of(UUID.randomUUID(),UUID.randomUUID()));
        var drops=new ArrayList<ItemEntity>();drops.add(new ItemEntity(t.getLevel(),0,100,0,new ItemStack(Items.DIAMOND)));
        new ScalingEvents().drops(new LivingDropsEvent(m,t.getLevel().damageSources().generic(),drops,0,false));t.assertTrue(drops.size()==1 && PersonalLoot.owner(drops.get(0))==null,"Shared exception stays shared");
        m=mob(t);ScalingEvents.initialize(m,config(1),List.of());new ScalingEvents().drops(new LivingDropsEvent(m,t.getLevel().damageSources().generic(),drops,0,false));t.assertTrue(drops.size()==1,"Zero players keeps vanilla drops");t.succeed();
    }
    @GameTest(template="empty") public static void kubejsAndSpawnEvent(GameTestHelper t) {
        var id=new ResourceLocation("minecraft:zombie");
        t.assertTrue(ReloadContainer.INSTANCE.MOB_CONFIG.containsKey(id),"KubeJS example must register mob at server script load");
        var m=mob(t);m.setPos(t.absolutePos(new net.minecraft.core.BlockPos(1,2,1)).getCenter());
        t.getLevel().addFreshEntity(m);
        t.assertTrue(m.getPersistentData().contains(ScalingEvents.STATE),"Actual join event must initialize configured mob");
        var disk=mob(t);new ScalingEvents().spawn(new net.minecraftforge.event.entity.EntityJoinLevelEvent(disk,t.getLevel(),true));
        t.assertTrue(!disk.getPersistentData().contains(ScalingEvents.STATE),"Loaded entities must not be rescaled");
        t.succeed();
    }
    @GameTest(template="empty") public static void mergeCannotExposePersonalLoot(GameTestHelper t) throws Exception {
        var a=new ItemEntity(t.getLevel(),0,100,0,new ItemStack(Items.DIAMOND));
        var b=new ItemEntity(t.getLevel(),0,100,0,new ItemStack(Items.DIAMOND));a.getPersistentData().putUUID(PersonalLoot.OWNER,UUID.randomUUID());
        var merge=ItemEntity.class.getDeclaredMethod("tryToMerge",ItemEntity.class);merge.setAccessible(true);merge.invoke(b,a);merge.invoke(a,b);
        t.assertTrue(a.getItem().getCount()==1 && b.getItem().getCount()==1 && !a.isRemoved() && !b.isRemoved(),"Both merge directions must be blocked");t.succeed();
    }
    @GameTest(template="empty") public static void actualSpawnCountsNearbyPlayers(GameTestHelper t) {
        var a=player(t);var b=player(t);var c=player(t);
        var center=t.absolutePos(new net.minecraft.core.BlockPos(1,3,1)).getCenter();
        a.setPos(center);b.setPos(center.add(40,0,0));c.setPos(center.add(39,39,39));
        var level=t.getLevel();level.addNewPlayer(a);level.addNewPlayer(b);level.addNewPlayer(c);
        try {
            var m=mob(t);m.setPos(center);level.addFreshEntity(m);
            t.assertTrue(m.getMaxHealth()==40,"Actual spawn must count two players inside sphere, ignoring cube corner");
            var recipients=m.getPersistentData().getCompound(ScalingEvents.STATE).getList("Players",10);
            t.assertTrue(recipients.size()==2,"Two UUIDs must be saved at real spawn");
            b.setPos(center.add(80,0,0));t.assertTrue(m.getMaxHealth()==40,"Moving away must not change HP");m.discard();
        } finally {level.removePlayerImmediately(a,Entity.RemovalReason.DISCARDED);level.removePlayerImmediately(b,Entity.RemovalReason.DISCARDED);level.removePlayerImmediately(c,Entity.RemovalReason.DISCARDED);}
        t.succeed();
    }
}
