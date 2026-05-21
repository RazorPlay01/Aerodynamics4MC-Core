package com.aerodynamics4mc.flow;

import com.aerodynamics4mc.network.packet.AeroFlowAnalysisPacket;
import com.aerodynamics4mc.runtime.NativeSimulationBridge;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class AnalysisFlowCodec {
    private AnalysisFlowCodec() {}

    public static AeroFlowAnalysisPacket encodePayload(
        NativeSimulationBridge bridge,
        Identifier dimensionId,
        BlockPos origin,
        int baseSampleStride,
        short[] basePackedFlow,
        int fullResolution,
        float[] fullFlowState,
        float velocityTolerance,
        float pressureTolerance
    ) {
        if (bridge == null || basePackedFlow == null || fullFlowState == null || fullResolution <= 0) {
            return null;
        }
        int cells = fullResolution * fullResolution * fullResolution;
        if (fullFlowState.length != cells * PackedFlowField.CHANNELS) {
            return null;
        }

        float[] predicted = PackedFlowField.reconstructFullState(basePackedFlow, baseSampleStride, fullResolution);
        float[] residualVx = new float[cells];
        float[] residualVy = new float[cells];
        float[] residualVz = new float[cells];
        float[] residualPressure = new float[cells];
        for (int cell = 0; cell < cells; cell++) {
            int base = cell * PackedFlowField.CHANNELS;
            residualVx[cell] = fullFlowState[base] - predicted[base];
            residualVy[cell] = fullFlowState[base + 1] - predicted[base + 1];
            residualVz[cell] = fullFlowState[base + 2] - predicted[base + 2];
            residualPressure[cell] = fullFlowState[base + 3] - predicted[base + 3];
        }

        byte[] compressedVx = bridge.compressFloatGrid3d(residualVx, fullResolution, fullResolution, fullResolution, velocityTolerance);
        byte[] compressedVy = bridge.compressFloatGrid3d(residualVy, fullResolution, fullResolution, fullResolution, velocityTolerance);
        byte[] compressedVz = bridge.compressFloatGrid3d(residualVz, fullResolution, fullResolution, fullResolution, velocityTolerance);
        byte[] compressedPressure = bridge.compressFloatGrid3d(residualPressure, fullResolution, fullResolution, fullResolution, pressureTolerance);
        if (compressedVx == null || compressedVy == null || compressedVz == null || compressedPressure == null) {
            return null;
        }

        return new AeroFlowAnalysisPacket(
            dimensionId,
            origin,
            baseSampleStride,
            fullResolution,
            velocityTolerance,
            pressureTolerance,
            basePackedFlow,
            compressedVx,
            compressedVy,
            compressedVz,
            compressedPressure
        );
    }

    public static float[] decodePayload(NativeSimulationBridge bridge, AeroFlowAnalysisPacket packet) {
        if (bridge == null || packet == null || packet.getFullResolution() <= 0) {
            return null;
        }
        int resolution = packet.getFullResolution();
        int cells = resolution * resolution * resolution;
        float[] flowState = PackedFlowField.reconstructFullState(packet.getBasePackedFlow(), packet.getBaseSampleStride(), resolution);
        float[] residual = new float[cells];
        if (!bridge.decompressFloatGrid3d(packet.getResidualVx(), resolution, resolution, resolution, residual)) {
            return null;
        }
        addResidualChannel(flowState, 0, residual);
        if (!bridge.decompressFloatGrid3d(packet.getResidualVy(), resolution, resolution, resolution, residual)) {
            return null;
        }
        addResidualChannel(flowState, 1, residual);
        if (!bridge.decompressFloatGrid3d(packet.getResidualVz(), resolution, resolution, resolution, residual)) {
            return null;
        }
        addResidualChannel(flowState, 2, residual);
        if (!bridge.decompressFloatGrid3d(packet.getResidualPressure(), resolution, resolution, resolution, residual)) {
            return null;
        }
        addResidualChannel(flowState, 3, residual);
        return flowState;
    }

    private static void addResidualChannel(float[] flowState, int channel, float[] residual) {
        for (int cell = 0; cell < residual.length; cell++) {
            flowState[cell * PackedFlowField.CHANNELS + channel] += residual[cell];
        }
    }
}
