package com.aerodynamics4mc.platform.fabric;

//? fabric {

import com.aerodynamics4mc.ModTemplate;
import com.aerodynamics4mc.client.AeroClientCommands;
import com.aerodynamics4mc.client.AeroClientMod;
import com.aerodynamics4mc.net.AeroCoarseWindPayload;
import com.aerodynamics4mc.net.AeroFlowAnalysisPayload;
import com.aerodynamics4mc.net.AeroFlowPayload;
import com.aerodynamics4mc.net.AeroRuntimeStatePayload;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

@Entrypoint("client")
public class FabricClientEntrypoint implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(AeroRuntimeStatePayload.ID, AeroClientMod::onRuntimeState);
		ClientPlayNetworking.registerGlobalReceiver(AeroFlowPayload.ID, AeroClientMod::onFlowField);
		ClientPlayNetworking.registerGlobalReceiver(AeroCoarseWindPayload.ID, AeroClientMod::onCoarseWindField);
		ClientPlayNetworking.registerGlobalReceiver(AeroFlowAnalysisPayload.ID, AeroClientMod::onFlowAnalysis);
		ModTemplate.onInitializeClient();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			if (!environment.includeDedicated) {
				AeroClientCommands.register(dispatcher, registryAccess);
			}
		});
	}
}
//?}
