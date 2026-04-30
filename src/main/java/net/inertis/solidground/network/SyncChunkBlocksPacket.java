package net.inertis.solidground.network;

import net.inertis.solidground.data.PlacedBlockManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class SyncChunkBlocksPacket {
    private final List<Long> positions;

    public SyncChunkBlocksPacket(Collection<Long> positions) {
        this.positions = new ArrayList<>(positions);
    }

    public SyncChunkBlocksPacket(List<Long> positions) {
        this.positions = positions;
    }

    public static void encode(SyncChunkBlocksPacket msg, FriendlyByteBuf buf) {
        // Write the size, then the longs
        buf.writeVarInt(msg.positions.size());
        for (Long pos : msg.positions) {
            buf.writeLong(pos);
        }
    }

    public static SyncChunkBlocksPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Long> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readLong());
        }
        return new SyncChunkBlocksPacket(list);
    }

    public static void handle(SyncChunkBlocksPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Add all received blocks to the Client Cache
            PlacedBlockManager.CLIENT_CACHE.addAll(msg.positions);
        });
        ctx.get().setPacketHandled(true);
    }
}