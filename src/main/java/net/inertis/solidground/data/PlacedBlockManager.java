package net.inertis.solidground.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class PlacedBlockManager extends SavedData {

    private static final String DATA_NAME = "solidground_placed_blocks";

    // STORAGE: Map<ChunkPosLong, Set<BlockPosLong>>
    // This allows us to instantly find all safe blocks in a specific chunk.
    private final Map<Long, Set<Long>> chunkMap = new HashMap<>();

    // CLIENT CACHE (Still a simple set because clients only hold loaded chunks)
    public static final Set<Long> CLIENT_CACHE = new HashSet<>();

    public static PlacedBlockManager get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getDataStorage().computeIfAbsent(
                    PlacedBlockManager::load,
                    PlacedBlockManager::new,
                    DATA_NAME
            );
        }
        throw new RuntimeException("Attempted to get PlacedBlockManager from a Client Level.");
    }

    public void addBlock(BlockPos pos) {
        long chunkKey = new ChunkPos(pos).toLong();
        long blockKey = pos.asLong();

        // Get or create the set for this chunk
        if (chunkMap.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(blockKey)) {
            setDirty();
        }
    }

    public void removeBlock(BlockPos pos) {
        long chunkKey = new ChunkPos(pos).toLong();
        long blockKey = pos.asLong();

        Set<Long> chunkBlocks = chunkMap.get(chunkKey);
        if (chunkBlocks != null) {
            if (chunkBlocks.remove(blockKey)) {
                setDirty();
                // Optional: Cleanup empty chunks to save RAM
                if (chunkBlocks.isEmpty()) {
                    chunkMap.remove(chunkKey);
                }
            }
        }
    }

    public boolean isPlayerPlaced(BlockPos pos) {
        long chunkKey = new ChunkPos(pos).toLong();
        Set<Long> chunkBlocks = chunkMap.get(chunkKey);
        return chunkBlocks != null && chunkBlocks.contains(pos.asLong());
    }

    /**
     * Helper to get all blocks in a specific chunk (For syncing)
     */
    public Set<Long> getBlocksInChunk(ChunkPos pos) {
        return chunkMap.getOrDefault(pos.toLong(), Collections.emptySet());
    }

    // --- SAVE / LOAD LOGIC ---

    public PlacedBlockManager() {}

    public static PlacedBlockManager load(CompoundTag tag) {
        PlacedBlockManager data = new PlacedBlockManager();
        ListTag list = tag.getList("Positions", Tag.TAG_LONG);

        // Reconstruct the Map from the flat list
        for (Tag t : list) {
            if (t instanceof LongTag longTag) {
                long blockLong = longTag.getAsLong();
                BlockPos pos = BlockPos.of(blockLong);
                long chunkKey = new ChunkPos(pos).toLong();

                data.chunkMap
                        .computeIfAbsent(chunkKey, k -> new HashSet<>())
                        .add(blockLong);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        // Flatten the map back into a list for saving
        for (Set<Long> blockSet : chunkMap.values()) {
            for (Long longPos : blockSet) {
                list.add(LongTag.valueOf(longPos));
            }
        }
        tag.put("Positions", list);
        return tag;
    }
}