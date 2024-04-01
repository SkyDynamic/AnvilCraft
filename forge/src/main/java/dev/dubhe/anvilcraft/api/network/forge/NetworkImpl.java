package dev.dubhe.anvilcraft.api.network.forge;

import dev.dubhe.anvilcraft.api.network.Network;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public abstract class NetworkImpl<T> extends Network<T> {
    private static final String PROTOCOL_VERSION = "1";
    public SimpleChannel instance = null;

    @Override
    public void init(Class<T> type) {
        this.instance = NetworkRegistry.newSimpleChannel(
                this.getType(),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        this.instance.registerMessage(0, type, this::encode, this::decode, (data, ctx) -> {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.handler(data));
                else this.handler(data, sender.server, sender);
            });
            ctx.get().setPacketHandled(true);
        });
    }

    @Override
    public void send(T data) {
        if (this.instance == null) return;
        this.instance.sendToServer(data);
    }

    @Override
    public void broadcastAll(T data) {
        if (this.instance == null) return;
        this.instance.send(PacketDistributor.ALL.noArg(), data);
    }

    @Override
    public void broadcastTrackingChunk(LevelChunk chunk, T data) {
        if (this.instance == null) return;
        this.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), data);
    }

    @Override
    public void send(ServerPlayer player, T data) {
        if (this.instance == null) return;
        this.instance.send(PacketDistributor.PLAYER.with(() -> player), data);
    }
}
