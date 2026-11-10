// kubejs/server_scripts/boss_scaling.js
// Second argument: items that remain SHARED (not duplicated). [] personalizes all drops.
BossScalingSystem.addEntityConfig('minecraft:zombie', [], 1.0)
// HP: 1 player = 100%, 2 = 200%, 3 = 300%. scale=0.5 adds 50% per extra player.
