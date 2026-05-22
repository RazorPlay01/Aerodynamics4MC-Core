package com.aerodynamics4mc.platform.neoforge;

//? neoforge {

import com.aerodynamics4mc.ModTemplate;
import com.aerodynamics4mc.block.ModBlocks;
import com.aerodynamics4mc.runtime.AeroCommands;
import com.aerodynamics4mc.runtime.AeroServerRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(ModTemplate.MOD_ID)
public class NeoforgeEntrypoint {
	private final AeroServerRuntime runtime = AeroServerRuntime.getInstance();

	public NeoforgeEntrypoint(IEventBus modEventBus, ModContainer modContainer) {
		ModTemplate.onInitialize();

		ModBlocks.register(modEventBus);

		NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
		NeoForge.EVENT_BUS.addListener(this::onServerTick);
		NeoForge.EVENT_BUS.addListener(this::onChunkLoad);
		NeoForge.EVENT_BUS.addListener(this::onChunkUnload);
		NeoForge.EVENT_BUS.addListener(this::onWorldUnload);
		NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
		NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
		NeoForge.EVENT_BUS.addListener(this::onServerStarted);
		NeoForge.EVENT_BUS.addListener(this::onServerStopped);

		modEventBus.addListener(NeoforgeClientEventSubscriber::onClientSetup);
	}

	private void onRegisterCommands(RegisterCommandsEvent event) {
		AeroCommands.register(event.getDispatcher());
	}

	private void onServerTick(ServerTickEvent.Post event) {
		runtime.onServerTick(event.getServer());
	}

	private void onChunkLoad(ChunkEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			runtime.onChunkLoad(serverLevel, event.getChunk());
		}
	}

	private void onChunkUnload(ChunkEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			runtime.onChunkUnload(serverLevel, event.getChunk());
		}
	}

	private void onWorldUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			runtime.onWorldUnload(serverLevel);
		}
	}

	private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			MinecraftServer server = player.server;
			runtime.sendStateToPlayer(player, server);
			runtime.broadcastState(server);
			runtime.sendFlowSnapshotToPlayer(player, server);
		}
	}

	private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			MinecraftServer server = player.server;
			runtime.onPlayerDisconnected(player);
			runtime.broadcastState(server);
		}
	}

	private void onServerStarted(ServerStartedEvent event) {
		runtime.enableStreamingOnServerStart(event.getServer());
	}

	private void onServerStopped(ServerStoppedEvent event) {
		runtime.shutdownAll(event.getServer());
	}
}
//?}
