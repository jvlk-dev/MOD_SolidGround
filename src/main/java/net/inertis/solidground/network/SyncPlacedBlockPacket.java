package net.inertis.solidground.network;

import net.inertis.solidground.data.PlacedBlockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPlacedBlockPacket {
    private final BlockPos pos;
    private final boolean isAdd; // true = add to list, false = remove

    public SyncPlacedBlockPacket(BlockPos pos, boolean isAdd) {
        this.pos = pos;
        this.isAdd = isAdd;
    }

    // Write data to buffer
    public static void encode(SyncPlacedBlockPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.isAdd);
    }

    // Read data from buffer
    public static SyncPlacedBlockPacket decode(FriendlyByteBuf buf) {
        return new SyncPlacedBlockPacket(buf.readBlockPos(), buf.readBoolean());
    }

    // Handle the packet on the Client Side
    public static void handle(SyncPlacedBlockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Update the Client-Side Cache
            if (msg.isAdd) {
                PlacedBlockManager.CLIENT_CACHE.add(msg.pos.asLong());
            } else {
                PlacedBlockManager.CLIENT_CACHE.remove(msg.pos.asLong());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}