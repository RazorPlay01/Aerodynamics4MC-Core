package com.aerodynamics4mc.net.update.packet;

import com.github.razorplay.packet_handler.exceptions.PacketSerializationException;
import com.github.razorplay.packet_handler.network.IPacket;
import com.github.razorplay.packet_handler.network.network_util.PacketDataSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AeroRuntimeStatePacket implements IPacket {
	private boolean streamingEnabled;
	private boolean renderVelocityVectors;
	private boolean renderStreamlines;

	@Override
	public void read(PacketDataSerializer serializer) throws PacketSerializationException {
		this.streamingEnabled = serializer.readBoolean();
		this.renderVelocityVectors = serializer.readBoolean();
		this.renderStreamlines = serializer.readBoolean();
	}

	@Override
	public void write(PacketDataSerializer serializer) throws PacketSerializationException {
		serializer.writeBoolean(this.streamingEnabled);
		serializer.writeBoolean(this.renderVelocityVectors);
		serializer.writeBoolean(this.renderStreamlines);
	}
}
