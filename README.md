# Solid Ground (Minecraft Forge 1.20.1)
Solid Ground is a gameplay-altering mod that introduces mining exhaustion mechanic and depth-dependent block hardness to Minecraft.
- Modrinth - https://modrinth.com/mod/solid-ground
- Curseforge - https://curseforge.com/minecraft/mc-mods/solid-ground

# 🛠 Features
- Fatigue System: Uses a HashMap-based tracking system to apply a BreakSpeed penalty that grows as the player breaks whitelisted blocks.
- Dynamic Depth Multiplier: Implements linear interpolation to increase block hardness as players descend from Y=66 down to Bedrock.
- Networked Sync: Features a custom packet handling system (SimpleChannel) to sync block placement data and fatigue levels between the Server and Client for a smooth visual experience.
- NBT Persistence: Fatigue data is stored in the player's PersistentData NBT, ensuring survival through restarts and relogs.
- Optimized Data Storage: Player-placed blocks are tracked using a SavedData manager organized by Chunk, ensuring the mod remains extremely lightweight even in pre-generated worlds.

# 📁 Configuration
The mod generates a solidground-common.toml file where you can define:
- Whitelist: Which blocks trigger the fatigue system.
- FatigueAddedPerBlock & FatigueDecayPerTick: Tuning for the exhaustion curve.
- Depth Scaling: Define specific Y-levels and their corresponding hardness multipliers.

# 🤝 Compatibility
Designed to be used in large modpacks (300+ mods). Works seamlessly with:
- Create: Encourages the use of mechanical drills for large-scale excavation.
- Iron's Spells 'n Spellbooks: Makes mining spells a valuable asset.
- Chunky: Fully compatible with pre-generated worlds.
