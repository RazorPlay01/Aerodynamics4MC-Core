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
public class AeroClientL2PreferencePacket implements IPacket {
	private boolean localL2Enabled;

	@Override
	public void read(PacketDataSerializer serializer) throws PacketSerializationException {
		this.localL2Enabled = serializer.readBoolean();
	}

	@Override
	public void write(PacketDataSerializer serializer) throws PacketSerializationException {
		serializer.writeBoolean(localL2Enabled);
	}
}
