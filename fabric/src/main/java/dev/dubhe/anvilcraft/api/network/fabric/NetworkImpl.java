package dev.dubhe.anvilcraft.api.network.fabric;

import dev.dubhe.anvilcraft.api.network.Network;
import dev.dubhe.anvilcraft.utils.fabric.ServerHooks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

public abstract class NetworkImpl<T> extends Network<T> {
    @Override
    public void init(Class<T> type) {
        ClientPlayNetworking.registerGlobalReceiver(this.getType(), (client, handler, buf, sender) -> this.handler(this.decode(buf)));
        ServerPlayNetworking.registerGlobalReceiver(this.getType(), (server, player, handler, buf, sender) -> this.handler(this.decode(buf), server, player));
    }

    @Override
    public void send(T data) {
        FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
        this.encode(data, friendlyByteBuf);
        ClientPlayNetworking.send(this.getType(), friendlyByteBuf);
    }

    @Override
    public void broadcastTrackingChunk(@NotNull LevelChunk chunk, T data) {
        FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
        this.encode(data, friendlyByteBuf);
        for (ServerPlayer player : ((ServerChunkCache) chunk.getLevel().getChunkSource()).chunkMap.getPlayers(chunk.getPos(), false)) {
            ServerPlayNetworking.getSender(player).sendPacket(this.getType(), friendlyByteBuf);
        }
    }

    @Override
    public void broadcastAll(T data) {
        FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
        this.encode(data, friendlyByteBuf);
        MinecraftServer server = ServerHooks.getServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.getSender(player).sendPacket(this.getType(), friendlyByteBuf);
        }
    }

    @Override
    public void send(ServerPlayer player, T data) {
        FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
        this.encode(data, friendlyByteBuf);
        ServerPlayNetworking.getSender(player).sendPacket(this.getType(), friendlyByteBuf);
    }
}
