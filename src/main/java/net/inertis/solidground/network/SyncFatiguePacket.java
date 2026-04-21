package net.inertis.solidground.network;

import net.inertis.solidground.events.MiningFatigueHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncFatiguePacket {
    private final float fatigue;

    public SyncFatiguePacket(float fatigue) {
        this.fatigue = fatigue;
    }

    public static void encode(SyncFatiguePacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.fatigue);
    }

    public static SyncFatiguePacket decode(FriendlyByteBuf buf) {
        return new SyncFatiguePacket(buf.readFloat());
    }

    public static void handle(SyncFatiguePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Update the Client-Side Cache
            MiningFatigueHandler.setClientFatigue(msg.fatigue);
        });
        ctx.get().setPacketHandled(true);
    }
}