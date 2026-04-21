package net.inertis.solidground.events;

// net imports
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkWatchEvent; // NEW IMPORT
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

// java imports
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// com imports
import net.inertis.solidground.SolidGround;
import net.inertis.solidground.config.SolidGroundConfigCommon;
import net.inertis.solidground.data.PlacedBlockManager;
import net.inertis.solidground.network.NetworkHandler;
import net.inertis.solidground.network.SyncPlacedBlockPacket;
import net.inertis.solidground.network.SyncFatiguePacket;
import net.inertis.solidground.network.SyncChunkBlocksPacket; // NEW IMPORT

// logger import
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = SolidGround.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MiningFatigueHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_KEY = "SolidGroundFatigue";
    private static float clientFatigueCache = 0f;

    // --- HELPER METHODS ---
    private static float getFatigue(Player player) {
        if (player.level().isClientSide) return clientFatigueCache;
        return player.getPersistentData().getFloat(NBT_KEY);
    }

    private static void setFatigue(Player player, float value) {
        if (player.level().isClientSide) clientFatigueCache = value;
        else player.getPersistentData().putFloat(NBT_KEY, value);
    }

    public static void setClientFatigue(float value) {
        clientFatigueCache = value;
    }

    // --- EVENTS ---

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            float savedFatigue = getFatigue(serverPlayer);
            NetworkHandler.sendToPlayer(new SyncFatiguePacket(savedFatigue), serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Level level = event.getLevel() instanceof Level ? (Level) event.getLevel() : null;
        if (level == null || level.isClientSide) return;

        BlockState state = event.getState();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());

        if (blockId != null && SolidGroundConfigCommon.WHITELIST.get().contains(blockId.toString())) {
            PlacedBlockManager.get(level).addBlock(event.getPos());
            NetworkHandler.sendToAllClients(new SyncPlacedBlockPacket(event.getPos(), true));
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        BlockState state = event.getState();
        if (player == null || state == null) return;

        // FIX: Ignore Creative and Spectator players
        if (player.isCreative() || player.isSpectator()) return;

        if (player instanceof FakePlayer) return;

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null || !SolidGroundConfigCommon.WHITELIST.get().contains(blockId.toString())) {
            return;
        }

        if (event.getPosition().isPresent()) {
            BlockPos pos = event.getPosition().get();
            Level level = player.level();
            boolean isSafe = false;

            if (level.isClientSide) {
                if (PlacedBlockManager.CLIENT_CACHE.contains(pos.asLong())) isSafe = true;
            } else {
                if (PlacedBlockManager.get(level).isPlayerPlaced(pos)) isSafe = true;
            }

            if (isSafe) return;
        }

        float depthDivider = 1.0f;
        if (event.getPosition().isPresent()) {
            int y = event.getPosition().get().getY();
            double addedHardness = getDepthHardness(y);
            depthDivider = (float) (1.0 + addedHardness);
        }

        float currentFatigue = getFatigue(player);
        float fatigueDivider = 1.0f;

        if (currentFatigue > 0) {
            float scalingFactor = SolidGroundConfigCommon.PENALTY_SCALING_FACTOR.get().floatValue();
            fatigueDivider = 1.0f + (currentFatigue / scalingFactor);
        }

        float totalDivider = depthDivider * fatigueDivider;

        if (totalDivider > 1.0f) {
            float originalSpeed = event.getOriginalSpeed();
            float newSpeed = originalSpeed / totalDivider;
            if (newSpeed < 0.05f) newSpeed = 0.05f;
            event.setNewSpeed(newSpeed);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide) return;

        BlockPos pos = event.getPos();
        PlacedBlockManager manager = PlacedBlockManager.get(level);

        if (manager.isPlayerPlaced(pos)) {
            manager.removeBlock(pos);
            NetworkHandler.sendToAllClients(new SyncPlacedBlockPacket(pos, false));
            return;
        }

        Player player = event.getPlayer();

        // FIX: Ignore Creative and Spectator players
        if (player.isCreative() || player.isSpectator()) return;

        if (player instanceof FakePlayer) return;

        BlockState state = event.getState();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());

        if (blockId == null || !SolidGroundConfigCommon.WHITELIST.get().contains(blockId.toString())) {
            return;
        }

        float current = getFatigue(player);
        float addedPerBlock = SolidGroundConfigCommon.FATIGUE_ADDED_PER_BLOCK.get().floatValue();
        float maxCap = SolidGroundConfigCommon.MAX_FATIGUE_CAP.get().floatValue();

        float newFatigue = Math.min(current + addedPerBlock, maxCap);

        setFatigue(player, newFatigue);

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToPlayer(new SyncFatiguePacket(newFatigue), serverPlayer);
        }

        if (newFatigue > (maxCap * 0.8f)) {
            player.displayClientMessage(Component.literal("The rock feels impossibly hard...").withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;
        List<BlockPos> affectedBlocks = event.getAffectedBlocks();
        PlacedBlockManager manager = PlacedBlockManager.get(level);
        for (BlockPos pos : affectedBlocks) {
            if (manager.isPlayerPlaced(pos)) {
                manager.removeBlock(pos);
                NetworkHandler.sendToAllClients(new SyncPlacedBlockPacket(pos, false));
            }
        }
    }

    /**
     * NEW: CHUNK WATCH SYNC
     * This fires when a client loads a chunk (login, teleport, walking).
     * We send them the list of Safe Blocks for that chunk.
     */
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        ChunkPos chunkPos = event.getPos();
        ServerLevel level = player.serverLevel();

        PlacedBlockManager manager = PlacedBlockManager.get(level);

        // Get safe blocks in this chunk
        Set<Long> blocksInChunk = manager.getBlocksInChunk(chunkPos);

        // If there are safe blocks, send them to the player
        if (!blocksInChunk.isEmpty()) {
            NetworkHandler.sendToPlayer(new SyncChunkBlocksPacket(blocksInChunk), player);
        }
    }

    // --- HELPER METHODS ---
    private static double getDepthHardness(int y) {
        int surfaceY = SolidGroundConfigCommon.Y_LEVEL_SURFACE.get();
        int deepslateY = SolidGroundConfigCommon.Y_LEVEL_DEEPSLATE_START.get();
        int bedrockY = SolidGroundConfigCommon.Y_LEVEL_BEDROCK.get();
        double surfHard = SolidGroundConfigCommon.HARDNESS_AT_SURFACE.get();
        double deepHard = SolidGroundConfigCommon.HARDNESS_AT_DEEPSLATE_START.get();
        double bedHard = SolidGroundConfigCommon.HARDNESS_AT_BEDROCK.get();
        if (y >= surfaceY) return surfHard;
        else if (y >= deepslateY) return interpolate(y, surfaceY, deepslateY, surfHard, deepHard);
        else return interpolate(Math.max(y, bedrockY), deepslateY, bedrockY, deepHard, bedHard);
    }

    private static double interpolate(int currentY, int y1, int y2, double val1, double val2) {
        double percentage = (double) (currentY - y1) / (double) (y2 - y1);
        return val1 + percentage * (val2 - val1);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;
            if (!player.level().isClientSide) {
                float current = getFatigue(player);
                if (current > 0) {
                    float decay = SolidGroundConfigCommon.FATIGUE_DECAY_PER_TICK.get().floatValue();
                    float newValue = Math.max(0, current - decay);
                    setFatigue(player, newValue);
                    current = newValue;
                }
                if (player.tickCount % 100 == 0 && player instanceof ServerPlayer serverPlayer) {
                    NetworkHandler.sendToPlayer(new SyncFatiguePacket(current), serverPlayer);
                }
            } else {
                float current = getFatigue(player);
                if (current > 0) {
                    float decay = SolidGroundConfigCommon.FATIGUE_DECAY_PER_TICK.get().floatValue();
                    setFatigue(player, Math.max(0, current - decay));
                }
            }
        }
    }
}