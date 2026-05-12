package com.aerodynamics4mc.api;

import com.aerodynamics4mc.runtime.AeroServerRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class AeroWindApi {
    private AeroWindApi() {
    }

    public static AeroWindSample sample(ServerLevel world, Vec3 position) {
        return AeroServerRuntime.sampleFlow(world, position);
    }

    public static AeroWindSample sample(ServerLevel world, Vec3 position, SamplePolicy policy) {
        return AeroServerRuntime.sampleFlow(world, position, policy);
    }

    public static AeroWindSample sample(ServerLevel world, BlockPos position) {
        return AeroServerRuntime.sampleFlow(world, position);
    }

    public static AeroWindSample sample(ServerLevel world, BlockPos position, SamplePolicy policy) {
        return AeroServerRuntime.sampleFlow(world, position, policy);
    }

    public static AeroWindSample sample(ServerPlayer player, Vec3 position) {
        return AeroServerRuntime.sampleFlow(player, position);
    }

    public static AeroWindSample sample(ServerPlayer player, Vec3 position, SamplePolicy policy) {
        return AeroServerRuntime.sampleFlow(player, position, policy);
    }

    public static AeroWindSample sample(ServerPlayer player, BlockPos position) {
        return AeroServerRuntime.sampleFlow(player, position);
    }

    public static AeroWindSample sample(ServerPlayer player, BlockPos position, SamplePolicy policy) {
        return AeroServerRuntime.sampleFlow(player, position, policy);
    }

    public static GameplayWindSample sampleGameplay(ServerLevel world, Vec3 position) {
        return AeroServerRuntime.sampleGameplay(world, position);
    }

    public static GameplayWindSample sampleGameplay(ServerLevel world, Vec3 position, SamplePolicy policy) {
        return AeroServerRuntime.sampleGameplay(world, position, policy);
    }

    public static GameplayWindSample sampleGameplay(ServerLevel world, BlockPos position) {
        return AeroServerRuntime.sampleGameplay(world, position);
    }

    public static GameplayWindSample sampleGameplay(ServerLevel world, BlockPos position, SamplePolicy policy) {
        return AeroServerRuntime.sampleGameplay(world, position, policy);
    }

    public static GameplayWindSample sampleGameplay(ServerPlayer player, Vec3 position) {
        return AeroServerRuntime.sampleGameplay(player, position);
    }

    public static GameplayWindSample sampleGameplay(ServerPlayer player, Vec3 position, SamplePolicy policy) {
        return AeroServerRuntime.sampleGameplay(player, position, policy);
    }

    public static GameplayWindSample sampleGameplay(ServerPlayer player, BlockPos position) {
        return AeroServerRuntime.sampleGameplay(player, position);
    }

    public static GameplayWindSample sampleGameplay(ServerPlayer player, BlockPos position, SamplePolicy policy) {
        return AeroServerRuntime.sampleGameplay(player, position, policy);
    }

    public static Vec3 sampleMeanVelocity(ServerLevel world, Vec3 position) {
        return sample(world, position).meanVelocity();
    }

    public static Vec3 sampleEffectiveVelocity(ServerLevel world, Vec3 position) {
        return sample(world, position).effectiveVelocity();
    }

    public static Vec3 sampleMeanVelocity(ServerPlayer player, Vec3 position) {
        return sample(player, position).meanVelocity();
    }

    public static Vec3 sampleEffectiveVelocity(ServerPlayer player, Vec3 position) {
        return sample(player, position).effectiveVelocity();
    }

    public static Vec3 sampleGameplayMeanVelocity(ServerLevel world, Vec3 position) {
        return sampleGameplay(world, position).meanVelocity();
    }

    public static Vec3 sampleGameplayEffectiveVelocity(ServerLevel world, Vec3 position) {
        return sampleGameplay(world, position).effectiveVelocity();
    }

    public static Vec3 sampleGameplayMeanVelocity(ServerPlayer player, Vec3 position) {
        return sampleGameplay(player, position).meanVelocity();
    }

    public static Vec3 sampleGameplayEffectiveVelocity(ServerPlayer player, Vec3 position) {
        return sampleGameplay(player, position).effectiveVelocity();
    }
}
