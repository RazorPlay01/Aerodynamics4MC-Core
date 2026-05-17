package com.aerodynamics4mc.client;

import com.aerodynamics4mc.api.AeroWindSample;
import com.aerodynamics4mc.api.SamplePolicy;
import com.aerodynamics4mc.net.AeroClientL2PreferencePayload;
import com.aerodynamics4mc.net.AeroCoarseWindPayload;
import com.aerodynamics4mc.net.AeroFlowAnalysisPayload;
import com.aerodynamics4mc.net.AeroFlowPayload;
import com.aerodynamics4mc.net.AeroRuntimeStatePayload;
import lombok.Getter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Getter
public final class AeroClientMod {

	private static AeroClientMod instance = null;
	private final AeroVisualizer visualizer = new AeroVisualizer();
	private final IrisWindBridge irisWindBridge = new IrisWindBridge(visualizer);
	private final ClientL2Solver clientL2Solver = new ClientL2Solver(visualizer);

	private AeroClientMod() {
		// private constructor
	}

	public static synchronized AeroClientMod getInstance() {
		if (instance == null)
			instance = new AeroClientMod();

		return instance;
	}

	public void onInitializeClient() {
		visualizer.initialize();
		irisWindBridge.initialize();
		clientL2Solver.initialize();
	}

	Component renderStatusText() {
		return Component.literal(
				"Render vectors=" + visualizer.renderVelocityVectorsEnabled()
						+ " streamlines=" + visualizer.renderStreamlinesEnabled()
		);
	}

	public static void sendClientL2Preference(boolean enabled) {
		try {
			if (ClientPlayNetworking.canSend(AeroClientL2PreferencePayload.ID)) {
				ClientPlayNetworking.send(new AeroClientL2PreferencePayload(enabled));
			}
		} catch (IllegalStateException ignored) {
			// The client may be between play-networking sessions
		}
	}

	// ====================== Network Handlers ======================

	public static void onRuntimeState(AeroRuntimeStatePayload payload, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			getInstance().getVisualizer().onRuntimeState(new AeroVisualizer.AeroFlowState(
					payload.streamingEnabled(),
					payload.renderVelocityVectors(),
					payload.renderStreamlines()
			));
			getInstance().getIrisWindBridge().onRuntimeState(payload.streamingEnabled());
			getInstance().getClientL2Solver().onRuntimeState(payload.streamingEnabled());
			sendClientL2Preference(getInstance().getClientL2Solver().isExperimentalEnabled() && payload.streamingEnabled());
		});
	}

	public static void onFlowField(AeroFlowPayload payload, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			getInstance().getVisualizer().onFlowField(payload);
			getInstance().getIrisWindBridge().markDirty();
		});
	}

	public static void onCoarseWindField(AeroCoarseWindPayload payload, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			getInstance().getVisualizer().onCoarseWindField(payload);
			getInstance().getClientL2Solver().onCoarseWindField(payload);
			getInstance().getIrisWindBridge().markDirty();
		});
	}

	public static void onFlowAnalysis(AeroFlowAnalysisPayload payload, ClientPlayNetworking.Context context) {
		context.client().execute(() -> getInstance().getVisualizer().onFlowAnalysis(payload));
	}

	// ====================== Static API ======================

	public static AeroWindSample sampleFlow(ClientLevel world, Vec3 position) {
		AeroClientMod active = instance;
		SamplePolicy policy = active != null && active.clientL2Solver.isExperimentalEnabled()
				? SamplePolicy.CLIENT_LOCAL_PREFERRED
				: SamplePolicy.SERVER_COARSE_ONLY;
		return sampleFlow(world, position, policy);
	}

	public static AeroWindSample sampleFlow(ClientLevel world, Vec3 position, SamplePolicy policy) {
		AeroClientMod active = instance;
		if (active == null || world == null) {
			return AeroWindSample.ZERO;
		}
		return active.visualizer.sampleFlow(world.dimension().identifier(), position, policy);
	}

	public static Vec3 sampleWind(ClientLevel world, Vec3 position) {
		return sampleFlow(world, position).velocity();
	}

	public static void notifyBlockStateChanged(ClientLevel world, BlockPos pos, BlockState oldState, BlockState newState) {
		AeroClientMod active = instance;
		if (active == null) {
			return;
		}
		active.clientL2Solver.onBlockStateChanged(world, pos, oldState, newState);
	}
}
