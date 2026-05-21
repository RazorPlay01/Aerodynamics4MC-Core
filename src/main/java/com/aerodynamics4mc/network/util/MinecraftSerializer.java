package com.aerodynamics4mc.network.util;

import com.github.razorplay.packet_handler.exceptions.PacketSerializationException;
import com.github.razorplay.packet_handler.network.network_util.PacketDataSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class MinecraftSerializer {

	private MinecraftSerializer() {}

	public static void writeIdentifier(PacketDataSerializer s, Identifier id) {
		s.writeString(id.getNamespace());
		s.writeString(id.getPath());
	}

	public static Identifier readIdentifier(PacketDataSerializer s) throws PacketSerializationException {
		String namespace = s.readString();
		String path = s.readString();
		return Identifier.fromNamespaceAndPath(namespace, path);
	}

	public static void writeBlockPos(PacketDataSerializer s, BlockPos pos) {
		s.writeInt(pos.getX());
		s.writeInt(pos.getY());
		s.writeInt(pos.getZ());
	}

	public static BlockPos readBlockPos(PacketDataSerializer s) throws PacketSerializationException {
		return new BlockPos(s.readInt(), s.readInt(), s.readInt());
	}

	public static void writeShortArray(PacketDataSerializer s, short[] array) {
		s.writeInt(array.length);
		for (short v : array) {
			s.writeShort(v);
		}
	}

	public static short[] readShortArray(PacketDataSerializer s) throws PacketSerializationException {
		int len = s.readInt();
		if (len < 0) throw new RuntimeException("Invalid length for short[]");
		short[] arr = new short[len];
		for (int i = 0; i < len; i++) {
			arr[i] = s.readShort();
		}
		return arr;
	}
}
