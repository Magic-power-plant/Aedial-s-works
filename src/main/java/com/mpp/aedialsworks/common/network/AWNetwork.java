package com.mpp.aedialsworks.common.network;

import com.mpp.aedialsworks.common.util.AWIds;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The mod's sole channel. IDs and directions are fixed, never conditional on config. */
public final class AWNetwork {
    public static final String PROTOCOL_VERSION = "6";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(AWIds.id("main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();
    private static boolean initialized;

    private AWNetwork() {}

    public static void init() {
        if (initialized) return;
        register(0, PacketMenuAction.class, PacketMenuAction::read, NetworkDirection.PLAY_TO_SERVER);
        register(1, PacketMenuData.class, PacketMenuData::read, NetworkDirection.PLAY_TO_CLIENT);
        register(2, com.mpp.aedialsworks.cellterminal.network.PacketNBTChunk.class,
                com.mpp.aedialsworks.cellterminal.network.PacketNBTChunk::read, NetworkDirection.PLAY_TO_CLIENT);
        register(3, com.mpp.aedialsworks.cellterminal.network.PacketOpenWirelessTerminal.class,
                com.mpp.aedialsworks.cellterminal.network.PacketOpenWirelessTerminal::read, NetworkDirection.PLAY_TO_SERVER);
        register(4,com.mpp.aedialsworks.powertools.network.PacketPowerHud.class,com.mpp.aedialsworks.powertools.network.PacketPowerHud::read,NetworkDirection.PLAY_TO_CLIENT);
        register(5,com.mpp.aedialsworks.cells.network.PacketCellsMemoryCard.class,com.mpp.aedialsworks.cells.network.PacketCellsMemoryCard::read,NetworkDirection.PLAY_TO_SERVER);
        register(6,com.mpp.aedialsworks.cells.network.PacketCellsFeedback.class,com.mpp.aedialsworks.cells.network.PacketCellsFeedback::read,NetworkDirection.PLAY_TO_CLIENT);
        initialized = true;
    }

    private static <T extends IAWPacket> void register(int id, Class<T> type,
            Function<FriendlyByteBuf, T> decoder, NetworkDirection direction) {
        CHANNEL.messageBuilder(type, id, direction)
                .encoder(IAWPacket::write)
                .decoder(decoder)
                .consumerMainThread((packet, context) -> packet.handle(context.get()))
                .add();
    }

    public static void sendToServer(IAWPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, IAWPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
