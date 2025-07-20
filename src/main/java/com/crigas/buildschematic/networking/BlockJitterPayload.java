package com.crigas.buildschematic.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record BlockJitterPayload(BlockPos pos) implements CustomPayload {
    public static final Id<BlockJitterPayload> ID = new Id<>(Identifier.of("buidlshematic", "block_jitter"));
    public static final PacketCodec<PacketByteBuf, BlockJitterPayload> CODEC = PacketCodec.of(BlockJitterPayload::write, BlockJitterPayload::new);

    public BlockJitterPayload(PacketByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
