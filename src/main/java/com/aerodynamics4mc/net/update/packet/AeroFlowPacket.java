package com.aerodynamics4mc.net.update.packet;

import com.aerodynamics4mc.net.update.util.MinecraftSerializer;
import com.github.razorplay.packet_handler.exceptions.PacketSerializationException;
import com.github.razorplay.packet_handler.network.IPacket;
import com.github.razorplay.packet_handler.network.network_util.PacketDataSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AeroFlowPacket implements IPacket {
	private Identifier dimensionId;
	private BlockPos origin;
	private int sampleStride;
	private short[] packedFlow;
	private byte[] packedFlowBytes;

	@Override
	public void read(PacketDataSerializer serializer) throws PacketSerializationException {
		dimensionId = MinecraftSerializer.readIdentifier(serializer);
		origin = MinecraftSerializer.readBlockPos(serializer);
		sampleStride = serializer.readInt();
		packedFlow = MinecraftSerializer.readShortArray(serializer);
		packedFlowBytes = serializer.readByteArray();
	}

	@Override
	public void write(PacketDataSerializer serializer) throws PacketSerializationException {
		MinecraftSerializer.writeIdentifier(serializer, dimensionId);
		MinecraftSerializer.writeBlockPos(serializer, origin);
		serializer.writeInt(sampleStride);
		MinecraftSerializer.writeShortArray(serializer, packedFlow);
		serializer.writeByteArray(packedFlowBytes);
	}
}
