package net.inertis.solidground.network;

import net.inertis.solidground.SolidGround;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SolidGround.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        // 1. Single Block Update (Placement/Break)
        CHANNEL.registerMessage(id++, SyncPlacedBlockPacket.class,
                SyncPlacedBlockPacket::encode,
                SyncPlacedBlockPacket::decode,
                SyncPlacedBlockPacket::handle);

        // 2. Fatigue Update
        CHANNEL.registerMessage(id++, SyncFatiguePacket.class,
                SyncFatiguePacket::encode,
                SyncFatiguePacket::decode,
                SyncFatiguePacket::handle);

        // 3. NEW: Chunk Bulk Update
        CHANNEL.registerMessage(id++, SyncChunkBlocksPacket.class,
                SyncChunkBlocksPacket::encode,
                SyncChunkBlocksPacket::decode,
                SyncChunkBlocksPacket::handle);
    }

    public static <MSG> void sendToAllClients(MSG message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}