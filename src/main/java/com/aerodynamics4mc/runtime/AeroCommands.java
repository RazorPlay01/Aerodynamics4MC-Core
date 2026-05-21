package com.aerodynamics4mc.runtime;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.aerodynamics4mc.runtime.AeroServerRuntime.*;

public class AeroCommands {
	private AeroCommands() {
		// private constructor
	}

	private static final AeroServerRuntime aeroServerRuntime = AeroServerRuntime.getInstance();

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher,
	                            final CommandBuildContext registryAccess,
	                            final Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("aero")
				.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
				.then(Commands.literal("status")
						.executes(ctx -> {
							AeroServerRuntime.PublishedFrame currentFrame = aeroServerRuntime.publishedFrame.get();
							float reportedMaxFlow = currentFrame == null
									? aeroServerRuntime.lastMaxFlowSpeed
									: Math.max(aeroServerRuntime.lastMaxFlowSpeed, currentFrame.maxSpeed());
							feedback(
									ctx.getSource(),
									"Status streaming=" + aeroServerRuntime.streamingEnabled
											+ " box=" + format2(DOMAIN_SIZE_METERS) + "m"
											+ " n=" + GRID_SIZE
											+ " dx=" + format4(CELL_SIZE_METERS) + "m"
											+ " dt=" + format3(SOLVER_STEP_SECONDS) + "s"
											+ " particleStride=" + PARTICLE_FLOW_SAMPLE_STRIDE
											+ " windows=" + attachedWindowCount()
											+ " simTicks=" + aeroServerRuntime.simulationTicks
											+ " simTickPerSec=" + format2(aeroServerRuntime.simulationTicksPerSecond)
											+ " maxFlow=" + format2(reportedMaxFlow)
											+ " publishedRegions=" + (currentFrame == null ? 0 : currentFrame.regionAtlases().size())
											+ " probes=" + aeroServerRuntime.publishedPlayerProbes.get().size()
											+ " entitySamples=" + aeroServerRuntime.publishedEntitySamples.get().size()
											+ " serverAuthL2=" + SERVER_AUTHORITATIVE_L2_ENABLED
											+ " clientLocalL2=" + aeroServerRuntime.clientLocalL2Players.size()
											+ " l0Cells=" + backgroundMetCellCount()
											+ " l1Cells=" + mesoscaleMetCellCount()
											+ " simBridge=" + aeroServerRuntime.simulationBridge.runtimeInfo()
							);
							feedback(
									ctx.getSource(),
									"SimDetail frameId=" + (currentFrame == null ? 0L : currentFrame.frameId())
											+ " frameAgeTicks=" + currentPublishedFrameAgeTicks()
											+ " stepBudget=" + aeroServerRuntime.simulationStepBudget.get()
											+ " activeSolveTasks=0"
											+ " busyRegions=0"
											+ " attachedReadyRegions=" + attachedWindowCount()
											+ " coordinatorAlive=" + isCoordinatorAlive()
											+ " coordTick=" + aeroServerRuntime.lastCoordinatorObservedTick
											+ " coordState=" + aeroServerRuntime.lastCoordinatorState
											+ " coordWindows=" + aeroServerRuntime.lastCoordinatorActiveWindowCount
											+ " coordSolveWindows=" + aeroServerRuntime.lastCoordinatorSolveWindowCount
											+ " coordScheduled=" + aeroServerRuntime.lastCoordinatorScheduledWindowCount
											+ " coordBusy=" + aeroServerRuntime.lastCoordinatorBusyWindowCount
											+ " coordPublishAge=" + currentCoordinatorPublishAgeTicks()
											+ " coordScheduleAge=" + currentCoordinatorScheduleAgeTicks()
											+ " coordSolveAge=" + currentCoordinatorSolveCompleteAgeTicks()
											+ " coordWaitMs=" + format3(nanosToMillis(aeroServerRuntime.lastCoordinatorWaitNanos))
											+ " coordPostMs=" + format3(nanosToMillis(aeroServerRuntime.lastCoordinatorPostSolveNanos))
											+ " bgRefreshAge=" + ageTicks(aeroServerRuntime.lastBackgroundRefreshAppliedTick)
											+ " bgRefreshWorlds=" + aeroServerRuntime.lastBackgroundRefreshWorldCount
											+ " bgRefreshMs=" + format3(nanosToMillis(aeroServerRuntime.lastBackgroundRefreshNanos))
											+ " bgDiagMs=" + format3(nanosToMillis(aeroServerRuntime.lastBackgroundDiagnosticsNanos))
											+ " bgDriverMs=" + format3(nanosToMillis(aeroServerRuntime.lastBackgroundDriverNanos))
											+ " bgL0Ms=" + format3(nanosToMillis(aeroServerRuntime.lastBackgroundL0Nanos))
											+ " bgL1Ms=" + format3(nanosToMillis(aeroServerRuntime.lastBackgroundL1Nanos))
											+ " bgFeedbackMs=" + format3(nanosToMillis(aeroServerRuntime.lastBackgroundFeedbackNanos))
											+ " deltaFlush=" + aeroServerRuntime.lastWorldDeltaFlushCount + "/" + aeroServerRuntime.pendingWorldDeltaCount
											+ " deltaFlushMs=" + format3(nanosToMillis(aeroServerRuntime.lastWorldDeltaFlushNanos))
											+ " staticRefresh=" + aeroServerRuntime.lastResidentBrickStaticRefreshCount + "/" + aeroServerRuntime.pendingResidentBrickStaticRefreshCount
											+ " staticRefreshMs=" + format3(nanosToMillis(aeroServerRuntime.lastResidentBrickStaticRefreshNanos))
											+ " coordAppliedMax=" + format3(aeroServerRuntime.lastCoordinatorAppliedMaxSpeed)
											+ " coordPublishedMax=" + format3(aeroServerRuntime.lastCoordinatorPublishedMaxSpeed)
											+ " coordNoPublish=" + (aeroServerRuntime.lastCoordinatorNoPublishReason.isEmpty() ? "-" : aeroServerRuntime.lastCoordinatorNoPublishReason)
							);
							feedback(
									ctx.getSource(),
									"MainThread lastTick=" + format3(nanosToMillis(aeroServerRuntime.lastMainThreadPhaseNanos[MAIN_THREAD_PHASE_TOTAL])) + "ms"
											+ " hot=" + hottestMainThreadPhaseSummary(aeroServerRuntime.lastMainThreadPhaseNanos)
											+ " maxTick=" + format3(nanosToMillis(aeroServerRuntime.maxMainThreadPhaseNanos[MAIN_THREAD_PHASE_TOTAL])) + "ms"
											+ " maxHot=" + hottestMainThreadPhaseSummary(aeroServerRuntime.maxMainThreadPhaseNanos)
							);
							feedback(ctx.getSource(), "MainThread breakdown=" + formatMainThreadPhaseBreakdown(aeroServerRuntime.lastMainThreadPhaseNanos));
							feedback(
									ctx.getSource(),
									"Callbacks lastHot=" + hottestCallbackPhaseSummary(aeroServerRuntime.lastCallbackTotalNanos, aeroServerRuntime.lastCallbackLockWaitNanos, aeroServerRuntime.lastCallbackLockHeldNanos)
											+ " maxHot=" + hottestCallbackPhaseSummary(aeroServerRuntime.maxCallbackTotalNanos, aeroServerRuntime.maxCallbackLockWaitNanos, aeroServerRuntime.maxCallbackLockHeldNanos)
							);
							if (!aeroServerRuntime.lastSolverError.isEmpty()) {
								feedback(ctx.getSource(), "Last solver error: " + aeroServerRuntime.lastSolverError);
							}
							if (!aeroServerRuntime.lastCoordinatorError.isEmpty()) {
								feedback(ctx.getSource(), "Last coordinator error: " + aeroServerRuntime.lastCoordinatorError);
							}
							sendNestedFeedbackStatus(ctx.getSource());
							return 1;
						}))
				.then(Commands.literal("dumpdata")
						.executes(ctx -> dumpRuntimeData(ctx.getSource()))
				)
				.then(Commands.literal("dump_l1")
						.executes(ctx -> dumpMesoscaleSnapshot(ctx.getSource()))
				)
				.then(Commands.literal("nested_feedback")
						.executes(ctx -> nestedFeedbackStatus(ctx.getSource()))
				)
				.then(Commands.literal("capture_l2")
						.then(Commands.literal("start")
								.executes(ctx -> startL2Capture(ctx.getSource(), L2_CAPTURE_DEFAULT_DURATION_SECONDS, L2_CAPTURE_DEFAULT_FPS))
								.then(Commands.argument("duration_seconds", IntegerArgumentType.integer(L2_CAPTURE_MIN_DURATION_SECONDS, L2_CAPTURE_MAX_DURATION_SECONDS))
										.executes(ctx -> startL2Capture(
												ctx.getSource(),
												IntegerArgumentType.getInteger(ctx, "duration_seconds"),
												L2_CAPTURE_DEFAULT_FPS
										))
										.then(Commands.argument("fps", IntegerArgumentType.integer(L2_CAPTURE_MIN_FPS, L2_CAPTURE_MAX_FPS))
												.executes(ctx -> startL2Capture(
														ctx.getSource(),
														IntegerArgumentType.getInteger(ctx, "duration_seconds"),
														IntegerArgumentType.getInteger(ctx, "fps")
												))
										)
								)
						)
						.then(Commands.literal("stop")
								.executes(ctx -> stopL2Capture(ctx.getSource(), true))
						)
						.then(Commands.literal("status")
								.executes(ctx -> l2CaptureStatus(ctx.getSource()))
						)
				)
				.then(Commands.literal("inspect_patch")
						.then(Commands.literal("dump")
								.executes(ctx -> dumpInspectionPatch(
										ctx.getSource(),
										INSPECTION_PATCH_DEFAULT_DOMAIN_BLOCKS,
										INSPECTION_PATCH_DEFAULT_GRID_RESOLUTION,
										INSPECTION_PATCH_DEFAULT_FACE_RESOLUTION
								))
								.then(Commands.argument("domain_blocks", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_DOMAIN_BLOCKS, INSPECTION_PATCH_MAX_DOMAIN_BLOCKS))
										.executes(ctx -> dumpInspectionPatch(
												ctx.getSource(),
												IntegerArgumentType.getInteger(ctx, "domain_blocks"),
												defaultInspectionPatchGridResolution(IntegerArgumentType.getInteger(ctx, "domain_blocks")),
												INSPECTION_PATCH_DEFAULT_FACE_RESOLUTION
										))
										.then(Commands.argument("face_resolution", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_FACE_RESOLUTION, INSPECTION_PATCH_MAX_FACE_RESOLUTION))
												.executes(ctx -> dumpInspectionPatch(
														ctx.getSource(),
														IntegerArgumentType.getInteger(ctx, "domain_blocks"),
														defaultInspectionPatchGridResolution(IntegerArgumentType.getInteger(ctx, "domain_blocks")),
														IntegerArgumentType.getInteger(ctx, "face_resolution")
												))
										)
										.then(Commands.argument("grid_resolution", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_GRID_RESOLUTION, INSPECTION_PATCH_MAX_GRID_RESOLUTION))
												.executes(ctx -> dumpInspectionPatch(
														ctx.getSource(),
														IntegerArgumentType.getInteger(ctx, "domain_blocks"),
														IntegerArgumentType.getInteger(ctx, "grid_resolution"),
														INSPECTION_PATCH_DEFAULT_FACE_RESOLUTION
												))
												.then(Commands.argument("face_resolution", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_FACE_RESOLUTION, INSPECTION_PATCH_MAX_FACE_RESOLUTION))
														.executes(ctx -> dumpInspectionPatch(
																ctx.getSource(),
																IntegerArgumentType.getInteger(ctx, "domain_blocks"),
																IntegerArgumentType.getInteger(ctx, "grid_resolution"),
																IntegerArgumentType.getInteger(ctx, "face_resolution")
														))
												)
										)
								)
						)
						.then(Commands.literal("solve")
								.executes(ctx -> startInspectionPatchSolve(
										ctx.getSource(),
										INSPECTION_PATCH_DEFAULT_DOMAIN_BLOCKS,
										INSPECTION_PATCH_DEFAULT_GRID_RESOLUTION,
										INSPECTION_PATCH_DEFAULT_FACE_RESOLUTION,
										INSPECTION_PATCH_DEFAULT_SOLVE_STEPS
								))
								.then(Commands.argument("domain_blocks", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_DOMAIN_BLOCKS, INSPECTION_PATCH_MAX_DOMAIN_BLOCKS))
										.executes(ctx -> startInspectionPatchSolve(
												ctx.getSource(),
												IntegerArgumentType.getInteger(ctx, "domain_blocks"),
												defaultInspectionPatchGridResolution(IntegerArgumentType.getInteger(ctx, "domain_blocks")),
												INSPECTION_PATCH_DEFAULT_FACE_RESOLUTION,
												INSPECTION_PATCH_DEFAULT_SOLVE_STEPS
										))
										.then(Commands.argument("face_resolution", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_FACE_RESOLUTION, INSPECTION_PATCH_MAX_FACE_RESOLUTION))
												.executes(ctx -> startInspectionPatchSolve(
														ctx.getSource(),
														IntegerArgumentType.getInteger(ctx, "domain_blocks"),
														defaultInspectionPatchGridResolution(IntegerArgumentType.getInteger(ctx, "domain_blocks")),
														IntegerArgumentType.getInteger(ctx, "face_resolution"),
														INSPECTION_PATCH_DEFAULT_SOLVE_STEPS
												))
												.then(Commands.argument("steps", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_SOLVE_STEPS, INSPECTION_PATCH_MAX_SOLVE_STEPS))
														.executes(ctx -> startInspectionPatchSolve(
																ctx.getSource(),
																IntegerArgumentType.getInteger(ctx, "domain_blocks"),
																defaultInspectionPatchGridResolution(IntegerArgumentType.getInteger(ctx, "domain_blocks")),
																IntegerArgumentType.getInteger(ctx, "face_resolution"),
																IntegerArgumentType.getInteger(ctx, "steps")
														))
												)
										)
										.then(Commands.argument("grid_resolution", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_GRID_RESOLUTION, INSPECTION_PATCH_MAX_GRID_RESOLUTION))
												.executes(ctx -> startInspectionPatchSolve(
														ctx.getSource(),
														IntegerArgumentType.getInteger(ctx, "domain_blocks"),
														IntegerArgumentType.getInteger(ctx, "grid_resolution"),
														INSPECTION_PATCH_DEFAULT_FACE_RESOLUTION,
														INSPECTION_PATCH_DEFAULT_SOLVE_STEPS
												))
												.then(Commands.argument("face_resolution", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_FACE_RESOLUTION, INSPECTION_PATCH_MAX_FACE_RESOLUTION))
														.executes(ctx -> startInspectionPatchSolve(
																ctx.getSource(),
																IntegerArgumentType.getInteger(ctx, "domain_blocks"),
																IntegerArgumentType.getInteger(ctx, "grid_resolution"),
																IntegerArgumentType.getInteger(ctx, "face_resolution"),
																INSPECTION_PATCH_DEFAULT_SOLVE_STEPS
														))
														.then(Commands.argument("steps", IntegerArgumentType.integer(INSPECTION_PATCH_MIN_SOLVE_STEPS, INSPECTION_PATCH_MAX_SOLVE_STEPS))
																.executes(ctx -> startInspectionPatchSolve(
																		ctx.getSource(),
																		IntegerArgumentType.getInteger(ctx, "domain_blocks"),
																		IntegerArgumentType.getInteger(ctx, "grid_resolution"),
																		IntegerArgumentType.getInteger(ctx, "face_resolution"),
																		IntegerArgumentType.getInteger(ctx, "steps")
																))
														)
												)
										)
								)
						)
						.then(Commands.literal("stop")
								.executes(ctx -> stopInspectionPatchSolve(ctx.getSource()))
						)
						.then(Commands.literal("status")
								.executes(ctx -> inspectionPatchSolveStatus(ctx.getSource()))
						)
				)
		);
	}

	private static int stopInspectionPatchSolve(CommandSourceStack source) {
		feedback(source, DEPRECATED_ANALYSIS_CAPTURE_MESSAGE);
		InspectionSolveSession session = aeroServerRuntime.activeInspectionSolveSession;
		if (session == null) {
			feedback(source, "No inspection solve is active");
			return 0;
		}
		session.getStopRequested().set(true);
		feedback(source, "Requested stop for inspection solve at " + session.getOutputDir());
		return 1;
	}

	private static int inspectionPatchSolveStatus(CommandSourceStack source) {
		feedback(source, DEPRECATED_ANALYSIS_CAPTURE_MESSAGE);
		InspectionSolveSession session = aeroServerRuntime.activeInspectionSolveSession;
		if (session == null) {
			feedback(source, "No inspection solve is active");
			return 1;
		}
		feedback(
				source,
				"Inspection solve phase=" + session.getPhase().get()
						+ " steps=" + session.getCompletedSteps().get() + "/" + session.getTotalSteps()
						+ " maxSpeed=" + format3(session.getMaxSpeedMetersPerSecond().get()) + " m/s"
						+ " output=" + session.getOutputDir()
		);
		if (!session.getLastError().get().isBlank()) {
			feedback(source, "Inspection solve error: " + session.getLastError().get());
		}
		return 1;
	}

	private static int dumpRuntimeData(CommandSourceStack source) {
		ServerLevel world = source.getLevel();
		BackgroundMetGrid l0Grid = aeroServerRuntime.backgroundMetGrids.get(world.dimension());
		MesoscaleGrid l1Grid = aeroServerRuntime.mesoscaleMetGrids.get(world.dimension());
		if (l0Grid == null && l1Grid == null) {
			feedback(source, "No L0 or L1 grid is active for " + world.dimension().identifier());
			return 0;
		}
		String worldId = storageSafeWorldId(world.dimension());
		Path baseOutputDir = source.getServer()
				.getWorldPath(LevelResource.ROOT)
				.resolve("aerodynamics4mc")
				.resolve("diagnostics");
		Path l0OutputPath = baseOutputDir.resolve("l0").resolve("l0_" + worldId + "_tick" + aeroServerRuntime.tickCounter + ".json");
		Path l1OutputPath = baseOutputDir.resolve("l1").resolve("l1_" + worldId + "_tick" + aeroServerRuntime.tickCounter + ".json");
		try {
			if (l0Grid != null) {
				Files.createDirectories(l0OutputPath.getParent());
				Files.writeString(
						l0OutputPath,
						encodeBackgroundSnapshot(world.dimension(), l0Grid.snapshot()),
						StandardCharsets.UTF_8
				);
			}
			if (l1Grid != null) {
				Files.createDirectories(l1OutputPath.getParent());
				Files.writeString(
						l1OutputPath,
						encodeMesoscaleSnapshot(world.dimension(), l1Grid.snapshot()),
						StandardCharsets.UTF_8
				);
			}
		} catch (IOException e) {
			feedback(source, "Failed to dump runtime snapshots: " + e.getMessage());
			return 0;
		}
		if (l0Grid != null) {
			feedback(source, "Dumped L0 snapshot to " + l0OutputPath);
			feedback(source, "View L0 with: python3 eval_background_snapshot.py --input \"" + l0OutputPath + "\"");
		}
		if (l1Grid != null) {
			feedback(source, "Dumped L1 snapshot to " + l1OutputPath);
			feedback(source, "View L1 with: python3 eval_mesoscale_snapshot.py --input \"" + l1OutputPath + "\"");
		}
		return 1;
	}

	private static int dumpMesoscaleSnapshot(CommandSourceStack source) {
		ServerLevel world = source.getLevel();
		MesoscaleGrid grid = aeroServerRuntime.mesoscaleMetGrids.get(world.dimension());
		if (grid == null) {
			feedback(source, "No mesoscale grid is active for " + world.dimension().identifier());
			return 0;
		}
		MesoscaleGrid.Snapshot snapshot = grid.snapshot();
		String worldId = storageSafeWorldId(world.dimension());
		Path outputDir = source.getServer()
				.getWorldPath(LevelResource.ROOT)
				.resolve("aerodynamics4mc")
				.resolve("diagnostics")
				.resolve("mesoscale");
		Path outputPath = outputDir.resolve("l1_" + worldId + "_tick" + aeroServerRuntime.tickCounter + ".json");
		try {
			Files.createDirectories(outputDir);
			Files.writeString(
					outputPath,
					encodeMesoscaleSnapshot(world.dimension(), snapshot),
					StandardCharsets.UTF_8
			);
		} catch (IOException e) {
			feedback(source, "Failed to dump L1 snapshot: " + e.getMessage());
			return 0;
		}
		feedback(source, "Dumped L1 snapshot to " + outputPath);
		feedback(source, "View with: python3 eval_mesoscale_snapshot.py --input \"" + outputPath + "\"");
		return 1;
	}

	private static int nestedFeedbackStatus(CommandSourceStack source) {
		if (sendNestedFeedbackStatus(source)) {
			return 1;
		}
		feedback(source, "Nested feedback diagnostics unavailable for " + source.getLevel().dimension().identifier());
		return 0;
	}

	private static boolean sendNestedFeedbackStatus(CommandSourceStack source) {
		ServerLevel world = source.getLevel();
		ResourceKey<Level> worldKey = world.dimension();
		ConcurrentLinkedQueue<MesoscaleGrid.NestedFeedbackBin> queue = aeroServerRuntime.pendingNestedFeedbackBins.get(worldKey);
		int pendingBinCount = queue == null ? 0 : queue.size();
		NestedFeedbackRuntimeDiagnostics runtimeDiagnostics = aeroServerRuntime.nestedFeedbackRuntimeDiagnostics.get(worldKey);
		NativeNestedFeedbackWorldDiagnostics nativeDiagnostics = collectNativeNestedFeedbackWorldDiagnostics(worldKey);
		MesoscaleGrid grid = aeroServerRuntime.mesoscaleMetGrids.get(worldKey);
		MesoscaleGrid.NestedFeedbackDiagnostics applyDiagnostics = grid == null
				? null
				: grid.nestedFeedbackDiagnostics();
		if (runtimeDiagnostics == null
				&& nativeDiagnostics == null
				&& (applyDiagnostics == null || applyDiagnostics.lastAppliedTick() == Long.MIN_VALUE)
				&& pendingBinCount <= 0) {
			return false;
		}

		int lastPollAgeTicks = runtimeDiagnostics == null || runtimeDiagnostics.lastPolledTick() == Integer.MIN_VALUE
				? -1
				: Math.max(0, aeroServerRuntime.tickCounter - runtimeDiagnostics.lastPolledTick());
		long lastAppliedAgeTicks = applyDiagnostics == null || applyDiagnostics.lastAppliedTick() == Long.MIN_VALUE
				? -1L
				: Math.max(0L, aeroServerRuntime.tickCounter - applyDiagnostics.lastAppliedTick());
		feedback(
				source,
				"NestedFeedback poll pendingBins=" + pendingBinCount
						+ " polledPackets=" + (runtimeDiagnostics == null ? 0L : runtimeDiagnostics.polledPacketCount())
						+ " polledBins=" + (runtimeDiagnostics == null ? 0L : runtimeDiagnostics.polledBinCount())
						+ " lastPacketBins=" + (runtimeDiagnostics == null ? 0 : runtimeDiagnostics.lastPacketBinCount())
						+ " lastPollAge=" + lastPollAgeTicks
						+ " lastMeanVolume=" + format3(runtimeDiagnostics == null ? 0.0f : runtimeDiagnostics.lastMeanVolumeAverage())
						+ " lastBottomFlux=" + format3(runtimeDiagnostics == null ? 0.0f : runtimeDiagnostics.lastMeanBottomFluxDensity())
						+ " lastTopFlux=" + format3(runtimeDiagnostics == null ? 0.0f : runtimeDiagnostics.lastMeanTopFluxDensity())
		);
		feedback(
				source,
				"NestedFeedback apply appliedCells=" + (applyDiagnostics == null ? 0 : applyDiagnostics.appliedCellCount())
						+ " inputBins=" + (applyDiagnostics == null ? 0 : applyDiagnostics.inputBinCount())
						+ " acceptedBins=" + (applyDiagnostics == null ? 0 : applyDiagnostics.acceptedBinCount())
						+ " lastApplyAge=" + lastAppliedAgeTicks
						+ " coverageMean=" + format3(applyDiagnostics == null ? 0.0f : applyDiagnostics.meanCoverage())
						+ " coverageMax=" + format3(applyDiagnostics == null ? 0.0f : applyDiagnostics.maxCoverage())
						+ " windDeltaMean=" + format3(applyDiagnostics == null ? 0.0f : applyDiagnostics.meanWindDelta())
						+ " windDeltaMax=" + format3(applyDiagnostics == null ? 0.0f : applyDiagnostics.maxWindDelta())
						+ " airDeltaMean=" + format3(applyDiagnostics == null ? 0.0f : applyDiagnostics.meanAirDeltaKelvin())
						+ " surfaceDeltaMean=" + format3(applyDiagnostics == null ? 0.0f : applyDiagnostics.meanSurfaceDeltaKelvin())
						+ " updraftMean=" + format3(applyDiagnostics == null ? 0.0f : applyDiagnostics.meanNestedUpdraft())
						+ " updraftMax=" + format3(applyDiagnostics == null ? 0.0f : applyDiagnostics.maxAbsNestedUpdraft())
		);
		if (nativeDiagnostics != null) {
			int lastBackendResetAgeTicks = nativeDiagnostics.lastBackendResetTick() == Integer.MIN_VALUE
					? -1
					: Math.max(0, aeroServerRuntime.tickCounter - nativeDiagnostics.lastBackendResetTick());
			feedback(
					source,
					"NestedFeedback native regions=" + nativeDiagnostics.regionCount()
							+ " bins=" + nativeDiagnostics.configuredBinCount()
							+ " steps=" + nativeDiagnostics.maxStepsAccumulated()
							+ "/" + nativeDiagnostics.stepsPerFeedback()
							+ " minSteps=" + nativeDiagnostics.minStepsAccumulated()
							+ " readyRegions=" + nativeDiagnostics.readyRegionCount()
							+ " emittedPackets=" + nativeDiagnostics.emittedPacketCount()
							+ " nativeResets=" + nativeDiagnostics.nativeResetCount()
							+ " backendResets=" + nativeDiagnostics.backendResetCount()
							+ " lastBackendResetAge=" + lastBackendResetAgeTicks
			);
		}
		return true;
	}

	private static int startL2Capture(CommandSourceStack source, int durationSeconds, int fps) {
		feedback(source, DEPRECATED_ANALYSIS_CAPTURE_MESSAGE);
		if (!aeroServerRuntime.streamingEnabled) {
			feedback(source, "Streaming must be enabled before starting L2 capture");
			return 0;
		}
		L2CaptureSession existing = aeroServerRuntime.activeL2CaptureSession;
		if (existing != null && !existing.stopRequested.get()) {
			feedback(source, "L2 capture already active: " + existing.outputDir);
			return 0;
		}
		ServerLevel world = source.getLevel();
		BlockPos focus = BlockPos.containing(source.getPosition());
		BlockPos anchorCoreOrigin = aeroServerRuntime.coreOriginForPosition(focus);
		List<L2CaptureRegionSpec> captureRegions = aeroServerRuntime.solveRegionKeys(List.of(new PlayerRegionAnchor(world.dimension(), anchorCoreOrigin, focus)))
				.stream()
				.sorted(Comparator
						.comparingInt((WindowKey key) -> key.origin().getY())
						.thenComparingInt(key -> key.origin().getX())
						.thenComparingInt(key -> key.origin().getZ()))
				.map(key -> new L2CaptureRegionSpec(
						key,
						key.origin().offset(REGION_HALO_CELLS, REGION_HALO_CELLS, REGION_HALO_CELLS)
				))
				.toList();
		int sampleIntervalTicks = Math.max(1, Math.round((float) TICKS_PER_SECOND / Math.max(1, fps)));
		Path outputDir = source.getServer()
				.getWorldPath(LevelResource.ROOT)
				.resolve("aerodynamics4mc")
				.resolve("diagnostics")
				.resolve("l2_capture")
				.resolve("l2_" + storageSafeWorldId(world.dimension()) + "_tick" + aeroServerRuntime.tickCounter + "_" + anchorCoreOrigin.getX() + "_" + anchorCoreOrigin.getY() + "_" + anchorCoreOrigin.getZ());
		try {
			Files.createDirectories(outputDir.resolve("frames"));
		} catch (IOException e) {
			feedback(source, "Failed to create capture output dir: " + e.getMessage());
			return 0;
		}
		L2CaptureSession session = new L2CaptureSession(
				world.dimension(),
				world.dimension().identifier(),
				anchorCoreOrigin,
				captureRegions,
				outputDir,
				aeroServerRuntime.tickCounter,
				aeroServerRuntime.tickCounter + durationSeconds * TICKS_PER_SECOND,
				durationSeconds,
				fps,
				sampleIntervalTicks
		);
		aeroServerRuntime.activeL2CaptureSession = session;
		aeroServerRuntime.persistL2CaptureMetadata(session);
		feedback(
				source,
				"Started L2 capture duration=" + durationSeconds + "s fps=" + fps
						+ " intervalTicks=" + sampleIntervalTicks
						+ " regions=" + captureRegions.size()
						+ " anchorCore=" + anchorCoreOrigin
						+ " output=" + outputDir
		);
		feedback(source, "Render with: python3 eval_l2_capture.py --input \"" + outputDir + "\"");
		return 1;
	}

	private static int stopL2Capture(CommandSourceStack source, boolean explicit) {
		feedback(source, DEPRECATED_ANALYSIS_CAPTURE_MESSAGE);
		L2CaptureSession session = aeroServerRuntime.activeL2CaptureSession;
		if (session == null) {
			feedback(source, "No L2 capture session is active");
			return 0;
		}
		session.stopRequested.set(true);
		aeroServerRuntime.activeL2CaptureSession = null;
		aeroServerRuntime.persistL2CaptureMetadata(session);
		feedback(
				source,
				(explicit ? "Stopping" : "Stopped") + " L2 capture framesWritten=" + session.framesWritten.get()
						+ " dropped=" + session.framesDropped.get()
						+ " failed=" + session.framesFailed.get()
						+ " output=" + session.outputDir
		);
		return 1;
	}

	private static int l2CaptureStatus(CommandSourceStack source) {
		feedback(source, DEPRECATED_ANALYSIS_CAPTURE_MESSAGE);
		L2CaptureSession session = aeroServerRuntime.activeL2CaptureSession;
		if (session == null) {
			feedback(source, "No L2 capture session is active");
			return 1;
		}
		feedback(
				source,
				"L2 capture active dim=" + session.dimensionId
						+ " anchorCore=" + session.anchorCoreOrigin
						+ " regions=" + session.regions.size()
						+ " layout=[" + session.layoutMinX + "," + session.layoutMinY + "," + session.layoutMinZ + " -> "
						+ session.layoutMaxExclusiveX + "," + session.layoutMaxExclusiveY + "," + session.layoutMaxExclusiveZ + ")"
						+ " tickRange=[" + session.startTick + "," + session.endTick + "]"
						+ " fps=" + session.fps
						+ " intervalTicks=" + session.sampleIntervalTicks
						+ " nextSampleTick=" + session.nextSampleTick
						+ " inFlight=" + session.inFlightFrames.get()
						+ " framesScheduled=" + session.nextFrameIndex.get()
						+ " framesWritten=" + session.framesWritten.get()
						+ " dropped=" + session.framesDropped.get()
						+ " failed=" + session.framesFailed.get()
						+ " output=" + session.outputDir
		);
		return 1;
	}

	private static int dumpInspectionPatch(CommandSourceStack source, int domainBlocks, int gridResolution, int faceResolution) {
		feedback(source, DEPRECATED_ANALYSIS_CAPTURE_MESSAGE);
		InspectionPatchInput input = captureInspectionPatchInput(source, domainBlocks, gridResolution, faceResolution);
		if (input == null) {
			return 0;
		}
		try {
			persistInspectionPatchDump(input);
		} catch (IOException e) {
			feedback(source, "Failed to dump inspection patch: " + e.getMessage());
			return 0;
		}

		feedback(
				source,
				"Dumped inspection patch blocks=" + input.domainBlocks()
						+ " grid=" + input.gridResolution()
						+ " cellsPerBlock=" + input.cellsPerBlock()
						+ " faceRes=" + input.faceResolution()
						+ " origin=" + input.origin()
						+ " fans=" + input.fans().size()
						+ " output=" + input.outputDir()
		);
		feedback(source, "Use /aero inspect_patch solve to run a monolithic inspection solve on this patch");
		return 1;
	}

	private static int startInspectionPatchSolve(CommandSourceStack source, int domainBlocks, int gridResolution, int faceResolution, int steps) {
		feedback(source, DEPRECATED_ANALYSIS_CAPTURE_MESSAGE);
		if (!aeroServerRuntime.streamingEnabled) {
			feedback(source, "Streaming must be enabled before solving an inspection patch");
			return 0;
		}
		if (!aeroServerRuntime.simulationBridge.isLoaded()) {
			feedback(source, "Native simulation bridge is not loaded: " + aeroServerRuntime.simulationBridge.getLoadError());
			return 0;
		}
		if (aeroServerRuntime.activeL2CaptureSession != null) {
			feedback(source, "Stop L2 capture before starting an inspection solve");
			return 0;
		}
		InspectionSolveSession existing = aeroServerRuntime.activeInspectionSolveSession;
		if (existing != null) {
			feedback(source, "Inspection solve already active: " + existing.getOutputDir());
			return 0;
		}
		InspectionPatchInput input = captureInspectionPatchInput(source, domainBlocks, gridResolution, faceResolution);
		if (input == null) {
			return 0;
		}
		try {
			persistInspectionPatchDump(input);
		} catch (IOException e) {
			feedback(source, "Failed to dump inspection patch input: " + e.getMessage());
			return 0;
		}
		aeroServerRuntime.stopSimulationCoordinator();
		aeroServerRuntime.waitForSolverIdle();
		InspectionSolveSession session = new InspectionSolveSession(input, steps);
		aeroServerRuntime.activeInspectionSolveSession = session;
		aeroServerRuntime.diagnosticsExecutor.execute(() -> runInspectionPatchSolve(session));
		feedback(
				source,
				"Started inspection solve blocks=" + input.domainBlocks()
						+ " grid=" + input.gridResolution()
						+ " cellsPerBlock=" + input.cellsPerBlock()
						+ " faceRes=" + faceResolution
						+ " steps=" + steps
						+ " output=" + input.outputDir()
		);
		feedback(source, "Use /aero inspect_patch status to monitor progress");
		return 1;
	}

	private static int defaultInspectionPatchGridResolution(int domainBlocks) {
		return Math.min(INSPECTION_PATCH_MAX_GRID_RESOLUTION, domainBlocks * 2);
	}

	private static String hottestCallbackPhaseSummary(long[] totalNanos, long[] waitNanos, long[] heldNanos) {
		int hottestPhase = 0;
		long hottestTotal = totalNanos[0];
		for (int i = 1; i < CALLBACK_PHASE_NAMES.length; i++) {
			if (totalNanos[i] > hottestTotal) {
				hottestTotal = totalNanos[i];
				hottestPhase = i;
			}
		}
		return CALLBACK_PHASE_NAMES[hottestPhase]
				+ ":total=" + format3(nanosToMillis(totalNanos[hottestPhase])) + "ms"
				+ ",wait=" + format3(nanosToMillis(waitNanos[hottestPhase])) + "ms"
				+ ",held=" + format3(nanosToMillis(heldNanos[hottestPhase])) + "ms";
	}

	private static String hottestMainThreadPhaseSummary(long[] phaseNanos) {
		int hottestPhase = MAIN_THREAD_PHASE_SERVICE_INIT;
		long hottestNanos = phaseNanos[hottestPhase];
		for (int i = 1; i < MAIN_THREAD_PHASE_TOTAL; i++) {
			if (phaseNanos[i] > hottestNanos) {
				hottestNanos = phaseNanos[i];
				hottestPhase = i;
			}
		}
		return MAIN_THREAD_PHASE_NAMES[hottestPhase] + ":" + format3(nanosToMillis(hottestNanos)) + "ms";
	}

	private static String formatMainThreadPhaseBreakdown(long[] phaseNanos) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < MAIN_THREAD_PHASE_TOTAL; i++) {
			if (i > 0) {
				builder.append(' ');
			}
			builder.append(MAIN_THREAD_PHASE_NAMES[i])
					.append('=')
					.append(format3(nanosToMillis(phaseNanos[i])))
					.append("ms");
		}
		return builder.toString();
	}

	private static String formatPos(BlockPos pos) {
		return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
	}

	private static String format2(float value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static String format3(float value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}

	private static String format4(float value) {
		return String.format(Locale.ROOT, "%.4f", value);
	}

	private static InspectionPatchInput captureInspectionPatchInput(CommandSourceStack source, int domainBlocks, int gridResolution, int faceResolution) {
		if ((domainBlocks % CHUNK_SIZE) != 0) {
			feedback(source, "Inspection patch domain size must be a multiple of " + CHUNK_SIZE + " blocks");
			return null;
		}
		if (!isValidInspectionPatchGridResolution(domainBlocks, gridResolution)) {
			feedback(
					source,
					"Inspection patch grid resolution must be >= domain blocks and an integer multiple of it"
							+ " (blocks=" + domainBlocks + ", grid=" + gridResolution + ")"
			);
			return null;
		}
		if (!aeroServerRuntime.streamingEnabled) {
			feedback(source, "Streaming must be enabled before dumping an inspection patch");
			return null;
		}
		if (!aeroServerRuntime.simulationBridge.isLoaded()) {
			feedback(source, "Native simulation bridge is not loaded: " + aeroServerRuntime.simulationBridge.getLoadError());
			return null;
		}
		ServerLevel world = source.getLevel();
		ResourceKey<Level> worldKey = world.dimension();
		if (aeroServerRuntime.mesoscaleMetGrids.get(worldKey) == null && aeroServerRuntime.backgroundMetGrids.get(worldKey) == null) {
			feedback(source, "No active L0/L1 background fields are available for " + worldKey.identifier());
			return null;
		}

		BlockPos focus = BlockPos.containing(source.getPosition());
		BlockPos origin = inspectionPatchOriginForFocus(world, focus, domainBlocks);
		WorldEnvironmentSnapshot environmentSnapshot = new WorldEnvironmentSnapshot(
				world.getDayTime(),
				world.getRainLevel(1.0f),
				world.getThunderLevel(1.0f),
				world.getSeaLevel()
		);
		NestedBoundaryCoupler.BoundarySample fallbackBoundary = sampleNestedBoundaryAtPosition(worldKey, focus);
		BoundaryFieldData boundaryField = sampleInspectionBoundaryField(worldKey, origin, domainBlocks, faceResolution, fallbackBoundary);
		if (boundaryField == null) {
			feedback(source, "Failed to sample six-face inspection boundary field");
			return null;
		}
		ThermalEnvironment thermalEnvironment = aeroServerRuntime.sampleThermalEnvironment(
				environmentSnapshot,
				worldKey,
				focus,
				SOLVER_STEP_SECONDS
		);
		int cellsPerBlock = gridResolution / domainBlocks;
		InspectionPatchStaticFields staticFields = aeroServerRuntime.captureInspectionPatchStaticFields(world, origin, domainBlocks, gridResolution, cellsPerBlock);
		List<FanSource> fans = aeroServerRuntime.queryFanSources(worldKey, origin, domainBlocks);

		Path outputDir = inspectionPatchOutputDir(source.getServer(), worldKey, focus, domainBlocks, gridResolution);
		try {
			Files.createDirectories(outputDir.resolve("static"));
			Files.createDirectories(outputDir.resolve("boundary"));
		} catch (IOException e) {
			feedback(source, "Failed to create inspection patch output dir: " + e.getMessage());
			return null;
		}
		return new InspectionPatchInput(
				worldKey,
				focus,
				origin,
				domainBlocks,
				gridResolution,
				cellsPerBlock,
				faceResolution,
				outputDir,
				environmentSnapshot,
				fallbackBoundary,
				thermalEnvironment,
				boundaryField,
				staticFields,
				fans
		);
	}

	private static BoundaryFieldData sampleInspectionBoundaryField(
			ResourceKey<Level> worldKey,
			BlockPos origin,
			int size,
			int faceResolution,
			NestedBoundaryCoupler.BoundarySample fallback
	) {
		int faceCells = FACE_COUNT * faceResolution * faceResolution;
		float[] windX = new float[faceCells];
		float[] windY = new float[faceCells];
		float[] windZ = new float[faceCells];
		float[] airTemperature = new float[faceCells];
		double minX = origin.getX();
		double minY = origin.getY();
		double minZ = origin.getZ();
		double maxX = minX + size;
		double maxY = minY + size;
		double maxZ = minZ + size;

		fillVerticalBoundaryFace(worldKey, Direction.WEST.ordinal(), minX + 0.5, minZ, maxZ, minY, maxY, faceResolution, windX, windY, windZ, airTemperature, fallback);
		fillVerticalBoundaryFace(worldKey, Direction.EAST.ordinal(), maxX - 0.5, minZ, maxZ, minY, maxY, faceResolution, windX, windY, windZ, airTemperature, fallback);
		fillVerticalBoundaryFace(worldKey, Direction.NORTH.ordinal(), minZ + 0.5, minX, maxX, minY, maxY, faceResolution, windX, windY, windZ, airTemperature, fallback);
		fillVerticalBoundaryFace(worldKey, Direction.SOUTH.ordinal(), maxZ - 0.5, minX, maxX, minY, maxY, faceResolution, windX, windY, windZ, airTemperature, fallback);
		fillHorizontalBoundaryFace(worldKey, Direction.DOWN.ordinal(), minX, maxX, minZ, maxZ, minY + 0.5, faceResolution, windX, windY, windZ, airTemperature, minY, maxY, fallback);
		fillHorizontalBoundaryFace(worldKey, Direction.UP.ordinal(), minX, maxX, minZ, maxZ, maxY - 0.5, faceResolution, windX, windY, windZ, airTemperature, minY, maxY, fallback);
		return new BoundaryFieldData(faceResolution, INSPECTION_PATCH_ALL_FACE_MASK, windX, windY, windZ, airTemperature);
	}

	private static BlockPos inspectionPatchOriginForFocus(ServerLevel world, BlockPos focus, int domainBlocks) {
		int half = domainBlocks / 2;
		int rawX = Math.floorDiv(focus.getX() - half, CHUNK_SIZE) * CHUNK_SIZE;
		int rawY = Math.floorDiv(focus.getY() - half, CHUNK_SIZE) * CHUNK_SIZE;
		int rawZ = Math.floorDiv(focus.getZ() - half, CHUNK_SIZE) * CHUNK_SIZE;
		int maxOriginY = world.getMaxY() + 1 - domainBlocks;
		int clampedY = Mth.clamp(rawY, world.getMinY(), maxOriginY);
		return new BlockPos(rawX, clampedY, rawZ);
	}

	private static Path inspectionPatchOutputDir(MinecraftServer server, ResourceKey<Level> worldKey, BlockPos focus, int domainBlocks, int gridResolution) {
		return server.getWorldPath(LevelResource.ROOT)
				.resolve("aerodynamics4mc")
				.resolve("diagnostics")
				.resolve("inspection_patch")
				.resolve("inspection_" + storageSafeWorldId(worldKey) + "_tick" + aeroServerRuntime.tickCounter + "_"
						+ focus.getX() + "_" + focus.getY() + "_" + focus.getZ()
						+ "_b" + domainBlocks + "_r" + gridResolution);
	}

	private static boolean isValidInspectionPatchGridResolution(int domainBlocks, int gridResolution) {
		return gridResolution >= domainBlocks
				&& gridResolution <= INSPECTION_PATCH_MAX_GRID_RESOLUTION
				&& (gridResolution % domainBlocks) == 0;
	}

	private static int attachedWindowCount() {
		return aeroServerRuntime.desiredWindowKeys.size();
	}

	private static int currentPublishedFrameAgeTicks() {
		if (aeroServerRuntime.publishedFrame.get() == null) {
			return -1;
		}
		if (aeroServerRuntime.lastPublishedFrameTick == Integer.MIN_VALUE) {
			return -1;
		}
		return Math.max(0, aeroServerRuntime.tickCounter - aeroServerRuntime.lastPublishedFrameTick);
	}

	private static int currentCoordinatorPublishAgeTicks() {
		if (aeroServerRuntime.lastCoordinatorPublishTick == Integer.MIN_VALUE) {
			return -1;
		}
		return Math.max(0, aeroServerRuntime.tickCounter - aeroServerRuntime.lastCoordinatorPublishTick);
	}

	private static int currentCoordinatorScheduleAgeTicks() {
		if (aeroServerRuntime.lastCoordinatorScheduleTick == Integer.MIN_VALUE) {
			return -1;
		}
		return Math.max(0, aeroServerRuntime.tickCounter - aeroServerRuntime.lastCoordinatorScheduleTick);
	}

	private static int currentCoordinatorSolveCompleteAgeTicks() {
		if (aeroServerRuntime.lastCoordinatorSolveCompleteTick == Integer.MIN_VALUE) {
			return -1;
		}
		return Math.max(0, aeroServerRuntime.tickCounter - aeroServerRuntime.lastCoordinatorSolveCompleteTick);
	}

	private static int ageTicks(int tick) {
		if (tick == Integer.MIN_VALUE) {
			return -1;
		}
		return Math.max(0, aeroServerRuntime.tickCounter - tick);
	}

	private static int backgroundMetCellCount() {
		int total = 0;
		for (BackgroundMetGrid grid : aeroServerRuntime.backgroundMetGrids.values()) {
			total += grid.cellCount();
		}
		return total;
	}

	private static int mesoscaleMetCellCount() {
		int total = 0;
		for (MesoscaleGrid grid : aeroServerRuntime.mesoscaleMetGrids.values()) {
			total += grid.cellCount();
		}
		return total;
	}

	private static boolean isCoordinatorAlive() {
		AeroServerRuntime.SimulationCoordinator coordinator = aeroServerRuntime.simulationCoordinator;
		return coordinator != null && coordinator.running();
	}

	private static String encodeBackgroundSnapshot(ResourceKey<Level> worldKey, BackgroundMetGrid.Snapshot snapshot) {
		StringBuilder builder = new StringBuilder(1 << 18);
		builder.append("{\n");
		appendJsonField(builder, "dimension_id", worldKey.identifier().toString(), true);
		appendJsonField(builder, "grid_width", String.valueOf(snapshot.gridWidth()), true);
		appendJsonField(builder, "cell_size_blocks", String.valueOf(snapshot.cellSizeBlocks()), true);
		appendJsonField(builder, "radius_cells", String.valueOf(snapshot.radiusCells()), true);
		appendJsonField(builder, "center_cell_x", String.valueOf(snapshot.centerCellX()), true);
		appendJsonField(builder, "center_cell_z", String.valueOf(snapshot.centerCellZ()), true);
		appendJsonField(builder, "tick", String.valueOf(snapshot.tick()), true);
		appendJsonField(builder, "delta_seconds", String.valueOf(snapshot.deltaSeconds()), true);
		appendJsonField(builder, "solar_altitude", String.valueOf(snapshot.solarAltitude()), true);
		appendJsonField(builder, "clear_sky", String.valueOf(snapshot.clearSky()), true);
		appendJsonField(builder, "rain_gradient", String.valueOf(snapshot.rainGradient()), true);
		appendJsonField(builder, "thunder_gradient", String.valueOf(snapshot.thunderGradient()), true);
		WorldScaleDriver.Snapshot driver = snapshot.driver();
		if (driver != null) {
			builder.append("  \"driver\": {\n");
			appendJsonField(builder, "driver_time_seconds", String.valueOf(driver.driverTimeSeconds()), true, 4);
			appendJsonField(builder, "base_flow_x", String.valueOf(driver.baseFlowX()), true, 4);
			appendJsonField(builder, "base_flow_z", String.valueOf(driver.baseFlowZ()), true, 4);
			appendJsonField(builder, "airmass_temperature_bias", String.valueOf(driver.airmassTemperatureBias()), true, 4);
			appendJsonField(builder, "airmass_moisture_bias", String.valueOf(driver.airmassMoistureBias()), true, 4);
			appendJsonField(builder, "planetary_wave_phase", String.valueOf(driver.planetaryWavePhase()), true, 4);
			appendJsonField(builder, "storm_activity", String.valueOf(driver.stormActivity()), true, 4);
			appendJsonField(builder, "season_phase", String.valueOf(driver.seasonPhase()), true, 4);
			appendJsonField(builder, "mesoscale_convective_support", String.valueOf(driver.mesoscaleConvectiveSupport()), true, 4);
			appendJsonField(builder, "mesoscale_lift_support", String.valueOf(driver.mesoscaleLiftSupport()), true, 4);
			appendJsonField(builder, "mesoscale_shear_support", String.valueOf(driver.mesoscaleShearSupport()), true, 4);
			builder.append("    \"cyclone_cells\": [\n");
			List<WorldScaleDriver.CycloneCellSnapshot> cycloneCells = driver.cycloneCells();
			for (int i = 0; i < cycloneCells.size(); i++) {
				WorldScaleDriver.CycloneCellSnapshot cell = cycloneCells.get(i);
				builder.append("      {\n");
				appendJsonField(builder, "center_cell_x", String.valueOf(cell.centerCellX()), true, 8);
				appendJsonField(builder, "center_cell_z", String.valueOf(cell.centerCellZ()), true, 8);
				appendJsonField(builder, "radius_cells", String.valueOf(cell.radiusCells()), true, 8);
				appendJsonField(builder, "intensity", String.valueOf(cell.intensity()), true, 8);
				appendJsonField(builder, "pressure_sign", String.valueOf(cell.pressureSign()), true, 8);
				appendJsonField(builder, "drift_x_cells_per_second", String.valueOf(cell.driftCellsPerSecondX()), true, 8);
				appendJsonField(builder, "drift_z_cells_per_second", String.valueOf(cell.driftCellsPerSecondZ()), true, 8);
				appendJsonField(builder, "lifecycle_phase", String.valueOf(cell.lifecyclePhase()), true, 8);
				appendJsonField(builder, "warm_core_bias_kelvin", String.valueOf(cell.warmCoreBiasKelvin()), true, 8);
				appendJsonField(builder, "moisture_core_bias", String.valueOf(cell.moistureCoreBias()), false, 8);
				builder.append("      }");
				if (i + 1 < cycloneCells.size()) {
					builder.append(',');
				}
				builder.append('\n');
			}
			builder.append("    ],\n");
			builder.append("    \"convective_clusters\": [\n");
			List<WorldScaleDriver.ConvectiveClusterSnapshot> convectiveClusters = driver.convectiveClusters();
			for (int i = 0; i < convectiveClusters.size(); i++) {
				WorldScaleDriver.ConvectiveClusterSnapshot cluster = convectiveClusters.get(i);
				builder.append("      {\n");
				appendJsonField(builder, "center_cell_x", String.valueOf(cluster.centerCellX()), true, 8);
				appendJsonField(builder, "center_cell_z", String.valueOf(cluster.centerCellZ()), true, 8);
				appendJsonField(builder, "radius_cells", String.valueOf(cluster.radiusCells()), true, 8);
				appendJsonField(builder, "intensity", String.valueOf(cluster.intensity()), true, 8);
				appendJsonField(builder, "drift_x_cells_per_second", String.valueOf(cluster.driftCellsPerSecondX()), true, 8);
				appendJsonField(builder, "drift_z_cells_per_second", String.valueOf(cluster.driftCellsPerSecondZ()), true, 8);
				appendJsonField(builder, "lifecycle_phase", String.valueOf(cluster.lifecyclePhase()), true, 8);
				appendJsonField(builder, "warm_bias_kelvin", String.valueOf(cluster.warmBiasKelvin()), true, 8);
				appendJsonField(builder, "moisture_bias", String.valueOf(cluster.moistureBias()), true, 8);
				appendJsonField(builder, "convergence_mps", String.valueOf(cluster.convergenceMps()), false, 8);
				builder.append("      }");
				if (i + 1 < convectiveClusters.size()) {
					builder.append(',');
				}
				builder.append('\n');
			}
			builder.append("    ],\n");
			builder.append("    \"tornado_vortices\": [\n");
			List<WorldScaleDriver.TornadoVortexSnapshot> tornadoVortices = driver.tornadoVortices();
			for (int i = 0; i < tornadoVortices.size(); i++) {
				WorldScaleDriver.TornadoVortexSnapshot vortex = tornadoVortices.get(i);
				builder.append("      {\n");
				appendJsonField(builder, "id", String.valueOf(vortex.id()), true, 8);
				appendJsonField(builder, "parent_convective_cluster_id", String.valueOf(vortex.parentConvectiveClusterId()), true, 8);
				appendJsonField(builder, "age_seconds", String.valueOf(vortex.ageSeconds()), true, 8);
				appendJsonField(builder, "lifetime_seconds", String.valueOf(vortex.lifetimeSeconds()), true, 8);
				appendJsonField(builder, "state_ordinal", String.valueOf(vortex.stateOrdinal()), true, 8);
				appendJsonField(builder, "center_block_x", String.valueOf(vortex.centerBlockX()), true, 8);
				appendJsonField(builder, "center_block_z", String.valueOf(vortex.centerBlockZ()), true, 8);
				appendJsonField(builder, "base_y", String.valueOf(vortex.baseY()), true, 8);
				appendJsonField(builder, "translation_x_blocks_per_second", String.valueOf(vortex.translationXBlocksPerSecond()), true, 8);
				appendJsonField(builder, "translation_z_blocks_per_second", String.valueOf(vortex.translationZBlocksPerSecond()), true, 8);
				appendJsonField(builder, "core_radius_blocks", String.valueOf(vortex.coreRadiusBlocks()), true, 8);
				appendJsonField(builder, "influence_radius_blocks", String.valueOf(vortex.influenceRadiusBlocks()), true, 8);
				appendJsonField(builder, "tangential_wind_scale_mps", String.valueOf(vortex.tangentialWindScaleMps()), true, 8);
				appendJsonField(builder, "radial_inflow_scale_mps", String.valueOf(vortex.radialInflowScaleMps()), true, 8);
				appendJsonField(builder, "updraft_scale", String.valueOf(vortex.updraftScale()), true, 8);
				appendJsonField(builder, "condensation_bias", String.valueOf(vortex.condensationBias()), true, 8);
				appendJsonField(builder, "intensity", String.valueOf(vortex.intensity()), true, 8);
				appendJsonField(builder, "rotation_sign", String.valueOf(vortex.rotationSign()), false, 8);
				builder.append("      }");
				if (i + 1 < tornadoVortices.size()) {
					builder.append(',');
				}
				builder.append('\n');
			}
			builder.append("    ]\n");
			builder.append("  },\n");
		}
		appendJsonArray(builder, "terrain_height_blocks", snapshot.terrainHeightBlocks(), true);
		appendJsonArray(builder, "biome_temperature", snapshot.biomeTemperature(), true);
		appendJsonArray(builder, "roughness_length_meters", snapshot.roughnessLengthMeters(), true);
		appendJsonByteArray(builder, "surface_class", snapshot.surfaceClass(), true);
		appendJsonArray(builder, "ambient_air_temperature_kelvin", snapshot.ambientAirTemperatureKelvin(), true);
		appendJsonArray(builder, "deep_ground_temperature_kelvin", snapshot.deepGroundTemperatureKelvin(), true);
		appendJsonArray(builder, "surface_temperature_kelvin", snapshot.surfaceTemperatureKelvin(), true);
		appendJsonArray(builder, "pressure_anomaly_pa", snapshot.pressureAnomalyPa(), true);
		appendJsonArray(builder, "pressure_gradient_x_pa_per_m", snapshot.pressureGradientXPaPerMeter(), true);
		appendJsonArray(builder, "pressure_gradient_z_pa_per_m", snapshot.pressureGradientZPaPerMeter(), true);
		appendJsonArray(builder, "geostrophic_wind_x", snapshot.geostrophicWindX(), true);
		appendJsonArray(builder, "geostrophic_wind_z", snapshot.geostrophicWindZ(), true);
		appendJsonArray(builder, "wind_x", snapshot.windX(), true);
		appendJsonArray(builder, "wind_z", snapshot.windZ(), true);
		appendJsonArray(builder, "humidity", snapshot.humidity(), true);
		appendJsonArray(builder, "vorticity", snapshot.vorticity(), true);
		appendJsonArray(builder, "divergence", snapshot.divergence(), true);
		appendJsonArray(builder, "temperature_anomaly", snapshot.temperatureAnomaly(), false);
		builder.append("\n}\n");
		return builder.toString();
	}

	private static String encodeMesoscaleSnapshot(ResourceKey<Level> worldKey, MesoscaleGrid.Snapshot snapshot) {
		StringBuilder builder = new StringBuilder(1 << 20);
		builder.append("{\n");
		appendJsonField(builder, "dimension_id", worldKey.identifier().toString(), true);
		appendJsonField(builder, "grid_width", String.valueOf(snapshot.gridWidth()), true);
		appendJsonField(builder, "active_layers", String.valueOf(snapshot.activeLayers()), true);
		appendJsonField(builder, "cell_size_blocks", String.valueOf(snapshot.cellSizeBlocks()), true);
		appendJsonField(builder, "layer_height_blocks", String.valueOf(snapshot.layerHeightBlocks()), true);
		appendJsonField(builder, "radius_cells", String.valueOf(snapshot.radiusCells()), true);
		appendJsonField(builder, "center_cell_x", String.valueOf(snapshot.centerCellX()), true);
		appendJsonField(builder, "center_cell_z", String.valueOf(snapshot.centerCellZ()), true);
		appendJsonField(builder, "vertical_base_y", String.valueOf(snapshot.verticalBaseY()), true);
		appendJsonField(builder, "step_seconds", String.valueOf(snapshot.stepSeconds()), true);
		appendJsonField(builder, "tick", String.valueOf(snapshot.lastTickProcessed()), true);
		builder.append("  \"nested_feedback_diagnostics\": {\n");
		MesoscaleGrid.NestedFeedbackDiagnostics nestedFeedbackDiagnostics = snapshot.nestedFeedbackDiagnostics();
		appendJsonField(builder, "last_applied_tick", String.valueOf(nestedFeedbackDiagnostics.lastAppliedTick()), true, 4);
		appendJsonField(builder, "input_bin_count", String.valueOf(nestedFeedbackDiagnostics.inputBinCount()), true, 4);
		appendJsonField(builder, "accepted_bin_count", String.valueOf(nestedFeedbackDiagnostics.acceptedBinCount()), true, 4);
		appendJsonField(builder, "applied_cell_count", String.valueOf(nestedFeedbackDiagnostics.appliedCellCount()), true, 4);
		appendJsonField(builder, "mean_coverage", String.valueOf(nestedFeedbackDiagnostics.meanCoverage()), true, 4);
		appendJsonField(builder, "max_coverage", String.valueOf(nestedFeedbackDiagnostics.maxCoverage()), true, 4);
		appendJsonField(builder, "mean_wind_delta", String.valueOf(nestedFeedbackDiagnostics.meanWindDelta()), true, 4);
		appendJsonField(builder, "max_wind_delta", String.valueOf(nestedFeedbackDiagnostics.maxWindDelta()), true, 4);
		appendJsonField(builder, "mean_air_delta_kelvin", String.valueOf(nestedFeedbackDiagnostics.meanAirDeltaKelvin()), true, 4);
		appendJsonField(builder, "max_air_delta_kelvin", String.valueOf(nestedFeedbackDiagnostics.maxAirDeltaKelvin()), true, 4);
		appendJsonField(builder, "mean_surface_delta_kelvin", String.valueOf(nestedFeedbackDiagnostics.meanSurfaceDeltaKelvin()), true, 4);
		appendJsonField(builder, "max_surface_delta_kelvin", String.valueOf(nestedFeedbackDiagnostics.maxSurfaceDeltaKelvin()), true, 4);
		appendJsonField(builder, "mean_bottom_flux_density", String.valueOf(nestedFeedbackDiagnostics.meanBottomFluxDensity()), true, 4);
		appendJsonField(builder, "mean_top_flux_density", String.valueOf(nestedFeedbackDiagnostics.meanTopFluxDensity()), true, 4);
		appendJsonField(builder, "mean_nested_updraft", String.valueOf(nestedFeedbackDiagnostics.meanNestedUpdraft()), true, 4);
		appendJsonField(builder, "max_abs_nested_updraft", String.valueOf(nestedFeedbackDiagnostics.maxAbsNestedUpdraft()), false, 4);
		builder.append("  },\n");
		appendJsonArray(builder, "terrain_height_blocks", snapshot.terrainHeightBlocks(), true);
		appendJsonArray(builder, "biome_temperature", snapshot.biomeTemperature(), true);
		appendJsonArray(builder, "roughness_length_meters", snapshot.roughnessLengthMeters(), true);
		appendJsonByteArray(builder, "surface_class", snapshot.surfaceClass(), true);
		appendJsonArray(builder, "ambient_air_temperature_kelvin", snapshot.ambientAirTemperatureKelvin(), true);
		appendJsonArray(builder, "deep_ground_temperature_kelvin", snapshot.deepGroundTemperatureKelvin(), true);
		appendJsonArray(builder, "surface_temperature_kelvin", snapshot.surfaceTemperatureKelvin(), true);
		appendJsonArray(builder, "forcing_ambient_target_kelvin", snapshot.forcingAmbientTargetKelvin(), true);
		appendJsonArray(builder, "forcing_surface_target_kelvin", snapshot.forcingSurfaceTargetKelvin(), true);
		appendJsonArray(builder, "forcing_background_wind_x", snapshot.forcingBackgroundWindX(), true);
		appendJsonArray(builder, "forcing_background_wind_z", snapshot.forcingBackgroundWindZ(), true);
		appendJsonArray(builder, "forcing_surface_wind_x", snapshot.forcingSurfaceWindX(), true);
		appendJsonArray(builder, "forcing_surface_wind_z", snapshot.forcingSurfaceWindZ(), true);
		appendJsonArray(builder, "forcing_geostrophic_wind_x", snapshot.forcingGeostrophicWindX(), true);
		appendJsonArray(builder, "forcing_geostrophic_wind_z", snapshot.forcingGeostrophicWindZ(), true);
		appendJsonArray(builder, "forcing_wind_shear_x_per_block", snapshot.forcingWindShearXPerBlock(), true);
		appendJsonArray(builder, "forcing_wind_shear_z_per_block", snapshot.forcingWindShearZPerBlock(), true);
		appendJsonArray(builder, "abl_height_blocks", snapshot.ablHeightBlocks(), true);
		appendJsonArray(builder, "abl_height_agl_blocks", snapshot.ablHeightAglBlocks(), true);
		appendJsonArray(builder, "abl_stability", snapshot.ablStability(), true);
		appendJsonArray(builder, "abl_mixing_strength", snapshot.ablMixingStrength(), true);
		appendJsonArray(builder, "abl_profile_blend", snapshot.ablProfileBlend(), true);
		appendJsonArray(builder, "forcing_nested_ambient_delta_kelvin", snapshot.forcingNestedAmbientDeltaKelvin(), true);
		appendJsonArray(builder, "forcing_nested_surface_delta_kelvin", snapshot.forcingNestedSurfaceDeltaKelvin(), true);
		appendJsonArray(builder, "forcing_nested_wind_x_delta", snapshot.forcingNestedWindXDelta(), true);
		appendJsonArray(builder, "forcing_nested_wind_z_delta", snapshot.forcingNestedWindZDelta(), true);
		appendJsonArray(builder, "forcing_nested_updraft", snapshot.forcingNestedUpdraft(), true);
		appendJsonArray(builder, "terrain_solid_mask", snapshot.terrainSolidMask(), true);
		appendJsonArray(builder, "wind_x", snapshot.windX(), true);
		appendJsonArray(builder, "wind_y", snapshot.windY(), true);
		appendJsonArray(builder, "wind_z", snapshot.windZ(), true);
		appendJsonArray(builder, "humidity", snapshot.humidity(), true);
		appendJsonArray(builder, "instability_proxy", snapshot.instabilityProxy(), true);
		appendJsonArray(builder, "low_level_shear", snapshot.lowLevelShear(), true);
		appendJsonArray(builder, "moisture_convergence", snapshot.moistureConvergence(), true);
		appendJsonArray(builder, "lift_proxy", snapshot.liftProxy(), false);
		builder.append("\n}\n");
		return builder.toString();
	}

	public static String storageSafeWorldId(ResourceKey<Level> worldKey) {
		return worldKey.identifier().toString()
				.replace(':', '_')
				.replace('/', '_');
	}

	private static Path worldScaleDriverPath(ServerLevel world) {
		return world.getServer()
				.getWorldPath(LevelResource.ROOT)
				.resolve("aerodynamics4mc")
				.resolve("weather")
				.resolve("driver_" + storageSafeWorldId(world.dimension()) + ".properties");
	}

	private static NativeNestedFeedbackWorldDiagnostics collectNativeNestedFeedbackWorldDiagnostics(ResourceKey<Level> worldKey) {
		if (aeroServerRuntime.simulationServiceId == 0L) {
			return null;
		}
		int regionCount = 0;
		int configuredBinCount = 0;
		int stepsPerFeedback = 0;
		int minStepsAccumulated = Integer.MAX_VALUE;
		int maxStepsAccumulated = 0;
		int readyRegionCount = 0;
		long emittedPacketCount = 0L;
		long nativeResetCount = 0L;
		long backendResetCount = 0L;
		int lastBackendResetTick = Integer.MIN_VALUE;
		for (Map.Entry<WindowKey, RegionRecord> entry : aeroServerRuntime.regions.entrySet()) {
			WindowKey key = entry.getKey();
			if (!key.worldKey().equals(worldKey)) {
				continue;
			}
			RegionRecord region = entry.getValue();
			if (!region.isServiceActive() || region.getNestedFeedbackLayout() == null) {
				continue;
			}
			NativeSimulationBridge.NestedFeedbackStatus status = aeroServerRuntime.simulationBridge.getRegionNestedFeedbackStatus(
					aeroServerRuntime.simulationServiceId,
					aeroServerRuntime.simulationRegionKey(key)
			);
			if (status == null || status.configuredBinCount() <= 0) {
				continue;
			}
			regionCount++;
			configuredBinCount += status.configuredBinCount();
			stepsPerFeedback = Math.max(stepsPerFeedback, status.stepsPerFeedback());
			minStepsAccumulated = Math.min(minStepsAccumulated, status.stepsAccumulated());
			maxStepsAccumulated = Math.max(maxStepsAccumulated, status.stepsAccumulated());
			if (status.readyPacketBinCount() > 0) {
				readyRegionCount++;
			}
			emittedPacketCount += status.emittedPacketCount();
			nativeResetCount += status.resetCount();
			backendResetCount += region.backendResetCount();
			lastBackendResetTick = Math.max(lastBackendResetTick, region.lastBackendResetTick());
		}
		if (regionCount <= 0) {
			return null;
		}
		if (minStepsAccumulated == Integer.MAX_VALUE) {
			minStepsAccumulated = 0;
		}
		return new NativeNestedFeedbackWorldDiagnostics(
				regionCount,
				configuredBinCount,
				stepsPerFeedback,
				minStepsAccumulated,
				maxStepsAccumulated,
				readyRegionCount,
				emittedPacketCount,
				nativeResetCount,
				backendResetCount,
				lastBackendResetTick
		);
	}

	private static void persistInspectionPatchDump(InspectionPatchInput input) throws IOException {
		persistInspectionPatchDump(
				input.outputDir(),
				input.worldKey(),
				input.focus(),
				input.origin(),
				input.domainBlocks(),
				input.gridResolution(),
				input.cellsPerBlock(),
				input.faceResolution(),
				input.environmentSnapshot(),
				input.fallbackBoundary(),
				input.thermalEnvironment(),
				input.boundaryField(),
				input.staticFields(),
				input.fans()
		);
	}

	private static void persistInspectionPatchDump(
			Path outputDir,
			ResourceKey<Level> worldKey,
			BlockPos focus,
			BlockPos origin,
			int domainBlocks,
			int gridResolution,
			int cellsPerBlock,
			int faceResolution,
			WorldEnvironmentSnapshot environmentSnapshot,
			NestedBoundaryCoupler.BoundarySample fallbackBoundary,
			ThermalEnvironment thermalEnvironment,
			BoundaryFieldData boundaryField,
			InspectionPatchStaticFields staticFields,
			List<FanSource> fans
	) throws IOException {
		Path staticDir = outputDir.resolve("static");
		Path boundaryDir = outputDir.resolve("boundary");

		Path obstaclePath = staticDir.resolve("obstacle.mask");
		Path surfaceKindPath = staticDir.resolve("surface_kind.u8");
		Path openFaceMaskPath = staticDir.resolve("open_face_mask.u8");
		Path emitterPowerPath = staticDir.resolve("emitter_power.zfp");
		Path faceSkyExposurePath = staticDir.resolve("face_sky_exposure.u8");
		Path faceDirectExposurePath = staticDir.resolve("face_direct_exposure.u8");
		Path boundaryWindXPath = boundaryDir.resolve("wind_x.zfp");
		Path boundaryWindYPath = boundaryDir.resolve("wind_y.zfp");
		Path boundaryWindZPath = boundaryDir.resolve("wind_z.zfp");
		Path boundaryAirTemperaturePath = boundaryDir.resolve("air_temperature.zfp");

		Files.write(obstaclePath, staticFields.obstacle());
		Files.write(surfaceKindPath, staticFields.surfaceKind());
		Files.write(openFaceMaskPath, staticFields.openFaceMask());
		Files.write(faceSkyExposurePath, staticFields.faceSkyExposure());
		Files.write(faceDirectExposurePath, staticFields.faceDirectExposure());
		if (!writeCompressedFloatGridFile(
				emitterPowerPath,
				staticFields.emitterPowerWatts(),
				gridResolution,
				gridResolution,
				gridResolution,
				INSPECTION_PATCH_EMITTER_POWER_TOLERANCE
		)) {
			throw new IOException("Failed to compress emitter power field");
		}
		if (!writeCompressedFloatGridFile(
				boundaryWindXPath,
				boundaryField.windX(),
				FACE_COUNT,
				faceResolution,
				faceResolution,
				INSPECTION_PATCH_BOUNDARY_VELOCITY_TOLERANCE
		) || !writeCompressedFloatGridFile(
				boundaryWindYPath,
				boundaryField.windY(),
				FACE_COUNT,
				faceResolution,
				faceResolution,
				INSPECTION_PATCH_BOUNDARY_VELOCITY_TOLERANCE
		) || !writeCompressedFloatGridFile(
				boundaryWindZPath,
				boundaryField.windZ(),
				FACE_COUNT,
				faceResolution,
				faceResolution,
				INSPECTION_PATCH_BOUNDARY_VELOCITY_TOLERANCE
		) || !writeCompressedFloatGridFile(
				boundaryAirTemperaturePath,
				boundaryField.airTemperatureKelvin(),
				FACE_COUNT,
				faceResolution,
				faceResolution,
				INSPECTION_PATCH_BOUNDARY_TEMPERATURE_TOLERANCE
		)) {
			throw new IOException("Failed to compress boundary field");
		}

		StringBuilder builder = new StringBuilder(1 << 15);
		builder.append("{\n");
		appendJsonField(builder, "format", "a4mc_inspection_patch_v2", true);
		appendJsonField(builder, "deprecated", String.valueOf(true), true);
		appendJsonField(builder, "capture_mode", "monolithic_patch_input", true);
		appendJsonField(builder, "dimension_id", worldKey.identifier().toString(), true);
		appendJsonField(builder, "tick", String.valueOf(aeroServerRuntime.tickCounter), true);
		appendJsonField(builder, "focus_x", String.valueOf(focus.getX()), true);
		appendJsonField(builder, "focus_y", String.valueOf(focus.getY()), true);
		appendJsonField(builder, "focus_z", String.valueOf(focus.getZ()), true);
		appendJsonField(builder, "origin_x", String.valueOf(origin.getX()), true);
		appendJsonField(builder, "origin_y", String.valueOf(origin.getY()), true);
		appendJsonField(builder, "origin_z", String.valueOf(origin.getZ()), true);
		appendJsonField(builder, "domain_blocks_x", String.valueOf(domainBlocks), true);
		appendJsonField(builder, "domain_blocks_y", String.valueOf(domainBlocks), true);
		appendJsonField(builder, "domain_blocks_z", String.valueOf(domainBlocks), true);
		appendJsonField(builder, "grid_resolution_x", String.valueOf(gridResolution), true);
		appendJsonField(builder, "grid_resolution_y", String.valueOf(gridResolution), true);
		appendJsonField(builder, "grid_resolution_z", String.valueOf(gridResolution), true);
		appendJsonField(builder, "size_x", String.valueOf(gridResolution), true);
		appendJsonField(builder, "size_y", String.valueOf(gridResolution), true);
		appendJsonField(builder, "size_z", String.valueOf(gridResolution), true);
		appendJsonField(builder, "cells_per_block", String.valueOf(cellsPerBlock), true);
		appendJsonField(builder, "cell_size_blocks", String.valueOf(1.0f / (float) cellsPerBlock), true);
		appendJsonField(builder, "obstacle_mask_encoding", "u8_raw_0_1", true);
		appendJsonField(builder, "surface_kind_encoding", "u8_enum", true);
		appendJsonField(builder, "open_face_mask_encoding", "u8_direction_bits", true);
		appendJsonField(builder, "face_exposure_encoding", "u8_unit_0_255", true);
		appendJsonField(builder, "boundary_face_resolution", String.valueOf(faceResolution), true);
		appendJsonField(builder, "boundary_external_face_mask", String.valueOf(boundaryField.externalFaceMask()), true);
		appendJsonField(builder, "boundary_velocity_tolerance", String.valueOf(INSPECTION_PATCH_BOUNDARY_VELOCITY_TOLERANCE), true);
		appendJsonField(builder, "boundary_temperature_tolerance", String.valueOf(INSPECTION_PATCH_BOUNDARY_TEMPERATURE_TOLERANCE), true);
		appendJsonField(builder, "emitter_power_tolerance", String.valueOf(INSPECTION_PATCH_EMITTER_POWER_TOLERANCE), true);
		builder.append("  \"environment\": {\n");
		appendJsonField(builder, "time_of_day", String.valueOf(environmentSnapshot.timeOfDay()), true, 4);
		appendJsonField(builder, "rain_gradient", String.valueOf(environmentSnapshot.rainGradient()), true, 4);
		appendJsonField(builder, "thunder_gradient", String.valueOf(environmentSnapshot.thunderGradient()), true, 4);
		appendJsonField(builder, "sea_level", String.valueOf(environmentSnapshot.seaLevel()), false, 4);
		builder.append("  },\n");
		builder.append("  \"fallback_boundary_sample\": {\n");
		appendJsonField(builder, "wind_x", String.valueOf(fallbackBoundary.windX()), true, 4);
		appendJsonField(builder, "wind_y", String.valueOf(fallbackBoundary.windY()), true, 4);
		appendJsonField(builder, "wind_z", String.valueOf(fallbackBoundary.windZ()), true, 4);
		appendJsonField(builder, "ambient_air_temperature_kelvin", String.valueOf(fallbackBoundary.ambientAirTemperatureKelvin()), true, 4);
		appendJsonField(builder, "deep_ground_temperature_kelvin", String.valueOf(fallbackBoundary.deepGroundTemperatureKelvin()), false, 4);
		builder.append("  },\n");
		builder.append("  \"thermal_environment\": {\n");
		appendJsonField(builder, "direct_solar_flux_wm2", String.valueOf(thermalEnvironment.directSolarFluxWm2()), true, 4);
		appendJsonField(builder, "diffuse_solar_flux_wm2", String.valueOf(thermalEnvironment.diffuseSolarFluxWm2()), true, 4);
		appendJsonField(builder, "ambient_air_temperature_kelvin", String.valueOf(thermalEnvironment.ambientAirTemperatureKelvin()), true, 4);
		appendJsonField(builder, "deep_ground_temperature_kelvin", String.valueOf(thermalEnvironment.deepGroundTemperatureKelvin()), true, 4);
		appendJsonField(builder, "sky_temperature_kelvin", String.valueOf(thermalEnvironment.skyTemperatureKelvin()), true, 4);
		appendJsonField(builder, "precipitation_temperature_kelvin", String.valueOf(thermalEnvironment.precipitationTemperatureKelvin()), true, 4);
		appendJsonField(builder, "precipitation_strength", String.valueOf(thermalEnvironment.precipitationStrength()), true, 4);
		appendJsonField(builder, "sun_x", String.valueOf(thermalEnvironment.sunX()), true, 4);
		appendJsonField(builder, "sun_y", String.valueOf(thermalEnvironment.sunY()), true, 4);
		appendJsonField(builder, "sun_z", String.valueOf(thermalEnvironment.sunZ()), true, 4);
		appendJsonField(builder, "surface_delta_seconds", String.valueOf(thermalEnvironment.surfaceDeltaSeconds()), false, 4);
		builder.append("  },\n");
		builder.append("  \"static_files\": {\n");
		appendJsonField(builder, "obstacle", outputDir.relativize(obstaclePath).toString(), true, 4);
		appendJsonField(builder, "surface_kind", outputDir.relativize(surfaceKindPath).toString(), true, 4);
		appendJsonField(builder, "open_face_mask", outputDir.relativize(openFaceMaskPath).toString(), true, 4);
		appendJsonField(builder, "emitter_power", outputDir.relativize(emitterPowerPath).toString(), true, 4);
		appendJsonField(builder, "face_sky_exposure", outputDir.relativize(faceSkyExposurePath).toString(), true, 4);
		appendJsonField(builder, "face_direct_exposure", outputDir.relativize(faceDirectExposurePath).toString(), false, 4);
		builder.append("  },\n");
		builder.append("  \"boundary_files\": {\n");
		appendJsonField(builder, "wind_x", outputDir.relativize(boundaryWindXPath).toString(), true, 4);
		appendJsonField(builder, "wind_y", outputDir.relativize(boundaryWindYPath).toString(), true, 4);
		appendJsonField(builder, "wind_z", outputDir.relativize(boundaryWindZPath).toString(), true, 4);
		appendJsonField(builder, "air_temperature", outputDir.relativize(boundaryAirTemperaturePath).toString(), false, 4);
		builder.append("  },\n");
		builder.append("  \"fans\": [\n");
		for (int i = 0; i < fans.size(); i++) {
			FanSource fan = fans.get(i);
			builder.append("    {\n");
			appendJsonField(builder, "world_x", String.valueOf(fan.pos().getX()), true, 6);
			appendJsonField(builder, "world_y", String.valueOf(fan.pos().getY()), true, 6);
			appendJsonField(builder, "world_z", String.valueOf(fan.pos().getZ()), true, 6);
			appendJsonField(builder, "local_x_blocks", String.valueOf(fan.pos().getX() - origin.getX()), true, 6);
			appendJsonField(builder, "local_y_blocks", String.valueOf(fan.pos().getY() - origin.getY()), true, 6);
			appendJsonField(builder, "local_z_blocks", String.valueOf(fan.pos().getZ() - origin.getZ()), true, 6);
			appendJsonField(builder, "facing", fan.facing().name(), true, 6);
			appendJsonField(builder, "duct_length", String.valueOf(fan.ductLength()), false, 6);
			builder.append("    }");
			if (i + 1 < fans.size()) {
				builder.append(',');
			}
			builder.append('\n');
		}
		builder.append("  ]\n");
		builder.append("}\n");
		Files.writeString(outputDir.resolve("metadata.json"), builder.toString(), StandardCharsets.UTF_8);
	}

	private static void runInspectionPatchSolve(InspectionSolveSession session) {
		try {
			session.getPhase().set("prepare_runtime");
			long regionKey;
			synchronized (aeroServerRuntime.simulationStateLock) {
				ensureSimulationServiceInitialized();
				if (aeroServerRuntime.simulationServiceId == 0L) {
					throw new IOException("Simulation service unavailable");
				}
				if (!simulationBridge.ensureL2Runtime(
						aeroServerRuntime.simulationServiceId,
						session.getInput().gridResolution(),
						session.getInput().gridResolution(),
						session.getInput().gridResolution(),
						CHANNELS,
						RESPONSE_CHANNELS
				)) {
					throw new IOException("Failed to switch native runtime to inspection patch dimensions");
				}
				regionKey = inspectionPatchRegionKey(session.getInput());
				prepareInspectionPatchInSimulation(regionKey, session.getInput());
			}

			session.getPhase().set("solve");
			float maxSpeedMps = 0.0f;
			for (int step = 0; step < session.getTotalSteps(); step++) {
				if (session.getStopRequested().get()) {
					session.getPhase().set("stopping");
					break;
				}
				float latticeMaxSpeed;
				synchronized (aeroServerRuntime.simulationStateLock) {
					latticeMaxSpeed = simulationBridge.stepRegionStored(
							aeroServerRuntime.simulationServiceId,
							regionKey,
							session.getInput().gridResolution(),
							session.getInput().gridResolution(),
							session.getInput().gridResolution(),
							session.getInput().fallbackBoundary().windX() / NATIVE_VELOCITY_SCALE,
							session.getInput().fallbackBoundary().windY() / NATIVE_VELOCITY_SCALE,
							session.getInput().fallbackBoundary().windZ() / NATIVE_VELOCITY_SCALE,
							session.getInput().fallbackBoundary().ambientAirTemperatureKelvin(),
							session.getInput().boundaryField().externalFaceMask(),
							session.getInput().boundaryField().faceResolution(),
							session.getInput().boundaryField().windX(),
							session.getInput().boundaryField().windY(),
							session.getInput().boundaryField().windZ(),
							session.getInput().boundaryField().airTemperatureKelvin(),
							INSPECTION_PATCH_SPONGE_THICKNESS_CELLS,
							INSPECTION_PATCH_SPONGE_VELOCITY_RELAXATION,
							INSPECTION_PATCH_SPONGE_TEMPERATURE_RELAXATION,
							0,
							null
					);
				}
				if (!Float.isFinite(latticeMaxSpeed)) {
					throw new IOException("Inspection solve step failed: " + simulationBridge.lastError());
				}
				maxSpeedMps = Math.max(maxSpeedMps, latticeMaxSpeed * NATIVE_VELOCITY_SCALE);
				session.getCompletedSteps().incrementAndGet();
				session.getMaxSpeedMetersPerSecond().set(maxSpeedMps);
			}

			session.getPhase().set("export");
			InspectionPatchSolveResult result;
			synchronized (aeroServerRuntime.simulationStateLock) {
				result = exportInspectionPatchSolveResult(
						regionKey,
						session.getInput().domainBlocks(),
						session.getInput().gridResolution(),
						session.getInput().cellsPerBlock(),
						session.getCompletedSteps().get(),
						maxSpeedMps
				);
			}
			persistInspectionPatchSolveResult(session.getInput().outputDir(), result);
			session.getPhase().set("done");
		} catch (Throwable t) {
			session.getPhase().set("failed");
			session.getLastError().set(t.getClass().getSimpleName() + ": " + t.getMessage());
			aeroServerRuntime.log("Inspection solve failed: " + t.getMessage());
		} finally {
			synchronized (aeroServerRuntime.simulationStateLock) {
				restoreSimulationRuntimeAfterInspectionSolve();
			}
			session.getCompleted().set(true);
		}
	}

	private static long inspectionPatchRegionKey(InspectionPatchInput input) {
		long value = 1469598103934665603L;
		value = (value ^ input.worldKey().identifier().hashCode()) * 1099511628211L;
		value = (value ^ input.origin().getX()) * 1099511628211L;
		value = (value ^ input.origin().getY()) * 1099511628211L;
		value = (value ^ input.origin().getZ()) * 1099511628211L;
		value = (value ^ input.domainBlocks()) * 1099511628211L;
		value = (value ^ input.gridResolution()) * 1099511628211L;
		return value == 0L ? 1L : value;
	}

	private static InspectionPatchSolveResult exportInspectionPatchSolveResult(
			long regionKey,
			int domainBlocks,
			int gridResolution,
			int cellsPerBlock,
			int completedSteps,
			float maxSpeedMps
	)
			throws IOException {
		int cells = gridResolution * gridResolution * gridResolution;
		float[] flowState = new float[cells * RESPONSE_CHANNELS];
		float[] airTemperature = new float[cells];
		float[] surfaceTemperature = new float[cells];
		if (!simulationBridge.exportDynamicRegion(
				aeroServerRuntime.simulationServiceId,
				regionKey,
				gridResolution,
				gridResolution,
				gridResolution,
				flowState,
				airTemperature,
				surfaceTemperature
		)) {
			throw new IOException("Failed to export inspection patch solution");
		}
		float[] vx = new float[cells];
		float[] vy = new float[cells];
		float[] vz = new float[cells];
		float[] pressure = new float[cells];
		for (int cell = 0; cell < cells; cell++) {
			int base = cell * RESPONSE_CHANNELS;
			vx[cell] = flowState[base];
			vy[cell] = flowState[base + 1];
			vz[cell] = flowState[base + 2];
			pressure[cell] = flowState[base + 3];
		}
		simulationBridge.deactivateRegion(aeroServerRuntime.simulationServiceId, regionKey);
		return new InspectionPatchSolveResult(
				domainBlocks,
				gridResolution,
				cellsPerBlock,
				vx,
				vy,
				vz,
				pressure,
				airTemperature,
				surfaceTemperature,
				completedSteps,
				maxSpeedMps
		);
	}

	private static void persistInspectionPatchSolveResult(Path outputDir, InspectionPatchSolveResult result) throws IOException {
		Path solutionDir = outputDir.resolve("solution");
		Files.createDirectories(solutionDir);
		Path vxPath = solutionDir.resolve("velocity_x.zfp");
		Path vyPath = solutionDir.resolve("velocity_y.zfp");
		Path vzPath = solutionDir.resolve("velocity_z.zfp");
		Path pressurePath = solutionDir.resolve("pressure.zfp");
		Path airTemperaturePath = solutionDir.resolve("air_temperature.zfp");
		Path surfaceTemperaturePath = solutionDir.resolve("surface_temperature.zfp");
		int size = result.gridResolution();
		if (!writeCompressedFloatGridFile(vxPath, result.vx(), size, size, size, INSPECTION_PATCH_OUTPUT_VELOCITY_TOLERANCE)
				|| !writeCompressedFloatGridFile(vyPath, result.vy(), size, size, size, INSPECTION_PATCH_OUTPUT_VELOCITY_TOLERANCE)
				|| !writeCompressedFloatGridFile(vzPath, result.vz(), size, size, size, INSPECTION_PATCH_OUTPUT_VELOCITY_TOLERANCE)
				|| !writeCompressedFloatGridFile(pressurePath, result.pressure(), size, size, size, INSPECTION_PATCH_OUTPUT_PRESSURE_TOLERANCE)
				|| !writeCompressedFloatGridFile(airTemperaturePath, result.airTemperature(), size, size, size, INSPECTION_PATCH_OUTPUT_TEMPERATURE_TOLERANCE)
				|| !writeCompressedFloatGridFile(surfaceTemperaturePath, result.surfaceTemperature(), size, size, size, INSPECTION_PATCH_OUTPUT_TEMPERATURE_TOLERANCE)) {
			throw new IOException("Failed to compress inspection patch solution fields");
		}

		StringBuilder builder = new StringBuilder(4096);
		builder.append("{\n");
		appendJsonField(builder, "format", "a4mc_inspection_patch_solution_v2", true);
		appendJsonField(builder, "domain_blocks_x", String.valueOf(result.domainBlocks()), true);
		appendJsonField(builder, "domain_blocks_y", String.valueOf(result.domainBlocks()), true);
		appendJsonField(builder, "domain_blocks_z", String.valueOf(result.domainBlocks()), true);
		appendJsonField(builder, "grid_resolution_x", String.valueOf(result.gridResolution()), true);
		appendJsonField(builder, "grid_resolution_y", String.valueOf(result.gridResolution()), true);
		appendJsonField(builder, "grid_resolution_z", String.valueOf(result.gridResolution()), true);
		appendJsonField(builder, "size_x", String.valueOf(result.gridResolution()), true);
		appendJsonField(builder, "size_y", String.valueOf(result.gridResolution()), true);
		appendJsonField(builder, "size_z", String.valueOf(result.gridResolution()), true);
		appendJsonField(builder, "cells_per_block", String.valueOf(result.cellsPerBlock()), true);
		appendJsonField(builder, "cell_size_blocks", String.valueOf(1.0f / (float) result.cellsPerBlock()), true);
		appendJsonField(builder, "completed_steps", String.valueOf(result.completedSteps()), true);
		appendJsonField(builder, "max_speed_mps", String.valueOf(result.maxSpeedMps()), true);
		appendJsonField(builder, "velocity_tolerance", String.valueOf(AeroServerRuntime.INSPECTION_PATCH_OUTPUT_VELOCITY_TOLERANCE), true);
		appendJsonField(builder, "pressure_tolerance", String.valueOf(AeroServerRuntime.INSPECTION_PATCH_OUTPUT_PRESSURE_TOLERANCE), true);
		appendJsonField(builder, "temperature_tolerance", String.valueOf(AeroServerRuntime.INSPECTION_PATCH_OUTPUT_TEMPERATURE_TOLERANCE), true);
		builder.append("  \"files\": {\n");
		appendJsonField(builder, "velocity_x", outputDir.relativize(vxPath).toString(), true, 4);
		appendJsonField(builder, "velocity_y", outputDir.relativize(vyPath).toString(), true, 4);
		appendJsonField(builder, "velocity_z", outputDir.relativize(vzPath).toString(), true, 4);
		appendJsonField(builder, "pressure", outputDir.relativize(pressurePath).toString(), true, 4);
		appendJsonField(builder, "air_temperature", outputDir.relativize(airTemperaturePath).toString(), true, 4);
		appendJsonField(builder, "surface_temperature", outputDir.relativize(surfaceTemperaturePath).toString(), false, 4);
		builder.append("  }\n");
		builder.append("}\n");
		Files.writeString(solutionDir.resolve("metadata.json"), builder.toString(), StandardCharsets.UTF_8);
	}

	private static void restoreSimulationRuntimeAfterInspectionSolve() {
		if (aeroServerRuntime.simulationServiceId == 0L) {
			return;
		}
		simulationBridge.ensureL2Runtime(aeroServerRuntime.simulationServiceId, GRID_SIZE, GRID_SIZE, GRID_SIZE, CHANNELS, RESPONSE_CHANNELS);
	}

	private static void prepareInspectionPatchInSimulation(long regionKey, InspectionPatchInput input) throws IOException {
		int size = input.gridResolution();
		int cells = size * size * size;
		if (!simulationBridge.activateRegion(aeroServerRuntime.simulationServiceId, regionKey, size, size, size)) {
			throw new IOException("Failed to activate inspection patch region");
		}
		if (!simulationBridge.uploadStaticRegion(
				aeroServerRuntime.simulationServiceId,
				regionKey,
				size,
				size,
				size,
				input.staticFields().obstacle(),
				input.staticFields().surfaceKind(),
				unsignedByteFieldToShorts(input.staticFields().openFaceMask()),
				input.staticFields().emitterPowerWatts(),
				input.staticFields().faceSkyExposure(),
				input.staticFields().faceDirectExposure()
		)) {
			throw new IOException("Failed to upload inspection patch static field");
		}
		InspectionPatchForcing forcing = buildInspectionPatchForcing(input);
		if (!simulationBridge.uploadRegionForcing(
				aeroServerRuntime.simulationServiceId,
				regionKey,
				size,
				size,
				size,
				forcing.thermalSource(),
				forcing.fanMask(),
				forcing.fanVx(),
				forcing.fanVy(),
				forcing.fanVz()
		)) {
			throw new IOException("Failed to upload inspection patch forcing");
		}
		InspectionPatchDynamicState initialState = buildInspectionPatchInitialState(input, cells);
		if (!simulationBridge.importDynamicRegion(
				aeroServerRuntime.simulationServiceId,
				regionKey,
				size,
				size,
				size,
				initialState.flowState(),
				initialState.airTemperature(),
				initialState.surfaceTemperature()
		)) {
			throw new IOException("Failed to initialize inspection patch dynamic state");
		}
		if (!simulationBridge.refreshRegionThermal(
				aeroServerRuntime.simulationServiceId,
				regionKey,
				size,
				size,
				size,
				input.thermalEnvironment().directSolarFluxWm2(),
				input.thermalEnvironment().diffuseSolarFluxWm2(),
				input.thermalEnvironment().ambientAirTemperatureKelvin(),
				input.thermalEnvironment().deepGroundTemperatureKelvin(),
				input.thermalEnvironment().skyTemperatureKelvin(),
				input.thermalEnvironment().precipitationTemperatureKelvin(),
				input.thermalEnvironment().precipitationStrength(),
				input.thermalEnvironment().sunX(),
				input.thermalEnvironment().sunY(),
				input.thermalEnvironment().sunZ(),
				input.thermalEnvironment().surfaceDeltaSeconds()
		)) {
			throw new IOException("Failed to initialize inspection patch thermal state");
		}
	}

	private static short[] unsignedByteFieldToShorts(byte[] values) {
		short[] result = new short[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = (short) Byte.toUnsignedInt(values[i]);
		}
		return result;
	}

	private static InspectionPatchForcing buildInspectionPatchForcing(InspectionPatchInput input) {
		int size = input.gridResolution();
		int cells = size * size * size;
		byte[] fanMask = new byte[cells];
		float[] fanVx = new float[cells];
		float[] fanVy = new float[cells];
		float[] fanVz = new float[cells];
		float[] thermalSource = new float[cells];
		for (FanSource fan : input.fans()) {
			applyFanSourceToPatchForcing(
					input.staticFields().obstacle(),
					size,
					fanMask,
					fanVx,
					fanVy,
					fanVz,
					fan,
					input.cellsPerBlock(),
					input.origin().getX(),
					input.origin().getY(),
					input.origin().getZ()
			);
		}
		return new InspectionPatchForcing(thermalSource, fanMask, fanVx, fanVy, fanVz);
	}

	private static void applyFanSourceToPatchForcing(
			byte[] obstacleMask,
			int size,
			byte[] fanMask,
			float[] fanVxField,
			float[] fanVyField,
			float[] fanVzField,
			FanSource fan,
			int cellsPerBlock,
			int minX,
			int minY,
			int minZ
	) {
		BlockPos inflowPos = fan.pos().relative(fan.facing());
		int cx = worldToPatchCell(inflowPos.getX() + 0.5, minX, cellsPerBlock);
		int cy = worldToPatchCell(inflowPos.getY() + 0.5, minY, cellsPerBlock);
		int cz = worldToPatchCell(inflowPos.getZ() + 0.5, minZ, cellsPerBlock);

		float inflowSpeed = runtimeFanSpeedMetersPerSecond();
		float fanVx = fan.facing().getStepX() * inflowSpeed;
		float fanVy = fan.facing().getStepY() * inflowSpeed;
		float fanVz = fan.facing().getStepZ() * inflowSpeed;

		int radiusCells = Math.max(1, FAN_RADIUS * cellsPerBlock);
		int radius2 = radiusCells * radiusCells;
		switch (fan.facing().getAxis()) {
			case X -> {
				for (int y = cy - radiusCells; y <= cy + radiusCells; y++) {
					for (int z = cz - radiusCells; z <= cz + radiusCells; z++) {
						int dy = y - cy;
						int dz = z - cz;
						if (dy * dy + dz * dz > radius2) {
							continue;
						}
						applyFanAtVoxelToPatchForcing(
								obstacleMask,
								size,
								fanMask,
								fanVxField,
								fanVyField,
								fanVzField,
								cx,
								y,
								z,
								fanVx,
								fanVy,
								fanVz
						);
					}
				}
			}
			case Y -> {
				for (int x = cx - radiusCells; x <= cx + radiusCells; x++) {
					for (int z = cz - radiusCells; z <= cz + radiusCells; z++) {
						int dx = x - cx;
						int dz = z - cz;
						if (dx * dx + dz * dz > radius2) {
							continue;
						}
						applyFanAtVoxelToPatchForcing(
								obstacleMask,
								size,
								fanMask,
								fanVxField,
								fanVyField,
								fanVzField,
								x,
								cy,
								z,
								fanVx,
								fanVy,
								fanVz
						);
					}
				}
			}
			case Z -> {
				for (int x = cx - radiusCells; x <= cx + radiusCells; x++) {
					for (int y = cy - radiusCells; y <= cy + radiusCells; y++) {
						int dx = x - cx;
						int dy = y - cy;
						if (dx * dx + dy * dy > radius2) {
							continue;
						}
						applyFanAtVoxelToPatchForcing(
								obstacleMask,
								size,
								fanMask,
								fanVxField,
								fanVyField,
								fanVzField,
								x,
								y,
								cz,
								fanVx,
								fanVy,
								fanVz
						);
					}
				}
			}
			default -> applyFanAtVoxelToPatchForcing(
					obstacleMask,
					size,
					fanMask,
					fanVxField,
					fanVyField,
					fanVzField,
					cx,
					cy,
					cz,
					fanVx,
					fanVy,
					fanVz
			);
		}
		applyDuctJetToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, fan, cellsPerBlock, minX, minY, minZ);
	}

	private static int worldToPatchCell(double worldCoord, int originCoord, int cellsPerBlock) {
		return (int) Math.floor((worldCoord - originCoord) * cellsPerBlock);
	}

	private static void applyDuctJetToPatchForcing(
			byte[] obstacleMask,
			int size,
			byte[] fanMask,
			float[] fanVxField,
			float[] fanVyField,
			float[] fanVzField,
			FanSource fan,
			int cellsPerBlock,
			int minX,
			int minY,
			int minZ
	) {
		int level = ductLevel(fan.ductLength());
		if (level <= 0) {
			return;
		}

		BlockPos inflowPos = fan.pos().relative(fan.facing());
		int sx = worldToPatchCell(inflowPos.getX() + 0.5, minX, cellsPerBlock);
		int sy = worldToPatchCell(inflowPos.getY() + 0.5, minY, cellsPerBlock);
		int sz = worldToPatchCell(inflowPos.getZ() + 0.5, minZ, cellsPerBlock);
		int dx = fan.facing().getStepX();
		int dy = fan.facing().getStepY();
		int dz = fan.facing().getStepZ();
		float levelBoost = switch (level) {
			case 1 -> 1.05f;
			case 2 -> 1.25f;
			default -> 1.55f;
		};

		float inflowSpeed = runtimeFanSpeedMetersPerSecond();
		float baseVx = dx * inflowSpeed;
		float baseVy = dy * inflowSpeed;
		float baseVz = dz * inflowSpeed;
		int range = switch (level) {
			case 1 -> 8;
			case 2 -> 14;
			default -> DUCT_JET_RANGE;
		} * cellsPerBlock;
		for (int step = 0; step < range; step++) {
			float t = range > 1 ? (float) step / (range - 1) : 0.0f;
			float decay = 1.0f - 0.55f * t;
			float coreScale = levelBoost * Math.max(0.35f, decay);
			int cx = sx + dx * step;
			int cy = sy + dy * step;
			int cz = sz + dz * step;
			applyFanAtVoxelToPatchForcing(
					obstacleMask,
					size,
					fanMask,
					fanVxField,
					fanVyField,
					fanVzField,
					cx,
					cy,
					cz,
					baseVx * coreScale,
					baseVy * coreScale,
					baseVz * coreScale
			);

			float edgeFalloff = Math.max(0.10f, 1.0f - 0.90f * t);
			float edgeScale = coreScale * DUCT_EDGE_FACTOR * edgeFalloff;
			switch (fan.facing().getAxis()) {
				case X -> {
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx, cy + 1, cz, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx, cy - 1, cz, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx, cy, cz + 1, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx, cy, cz - 1, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
				}
				case Y -> {
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx + 1, cy, cz, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx - 1, cy, cz, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx, cy, cz + 1, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx, cy, cz - 1, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
				}
				case Z -> {
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx + 1, cy, cz, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx - 1, cy, cz, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx, cy + 1, cz, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
					applyFanAtVoxelToPatchForcing(obstacleMask, size, fanMask, fanVxField, fanVyField, fanVzField, cx, cy - 1, cz, baseVx * edgeScale, baseVy * edgeScale, baseVz * edgeScale);
				}
			}
		}
	}

	private static void applyFanAtVoxelToPatchForcing(
			byte[] obstacleMask,
			int size,
			byte[] fanMask,
			float[] fanVxField,
			float[] fanVyField,
			float[] fanVzField,
			int x,
			int y,
			int z,
			float fanVx,
			float fanVy,
			float fanVz
	) {
		if (obstacleAtPatch(obstacleMask, size, x, y, z)) {
			return;
		}
		int cell = patchCellIndex(x, y, z, size);
		fanMask[cell] = 1;
		fanVxField[cell] += fanVx;
		fanVyField[cell] += fanVy;
		fanVzField[cell] += fanVz;
	}

	private static boolean obstacleAtPatch(byte[] obstacleMask, int size, int x, int y, int z) {
		return x < 0
				|| y < 0
				|| z < 0
				|| x >= size
				|| y >= size
				|| z >= size
				|| obstacleMask[patchCellIndex(x, y, z, size)] != 0;
	}

	private static InspectionPatchDynamicState buildInspectionPatchInitialState(InspectionPatchInput input, int cells) {
		float[] flowState = new float[cells * RESPONSE_CHANNELS];
		float[] airTemperature = new float[cells];
		float[] surfaceTemperature = new float[cells];
		float initialVx = input.fallbackBoundary().windX() / NATIVE_VELOCITY_SCALE;
		float initialVy = input.fallbackBoundary().windY() / NATIVE_VELOCITY_SCALE;
		float initialVz = input.fallbackBoundary().windZ() / NATIVE_VELOCITY_SCALE;
		float initialAirTemperature = input.thermalEnvironment().ambientAirTemperatureKelvin();
		float initialSurfaceTemperature = input.thermalEnvironment().deepGroundTemperatureKelvin();
		for (int cell = 0; cell < cells; cell++) {
			airTemperature[cell] = initialAirTemperature;
			surfaceTemperature[cell] = initialSurfaceTemperature;
			if (input.staticFields().obstacle()[cell] != 0) {
				continue;
			}
			int base = cell * RESPONSE_CHANNELS;
			flowState[base] = initialVx;
			flowState[base + 1] = initialVy;
			flowState[base + 2] = initialVz;
			flowState[base + 3] = 0.0f;
		}
		return new InspectionPatchDynamicState(flowState, airTemperature, surfaceTemperature);
	}
}
