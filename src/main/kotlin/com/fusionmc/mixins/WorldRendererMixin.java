package com.fusionmc.mixins;

import com.fusionmc.FusionMCMod;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final
    private MinecraftClient client;

    /**
     * Intercepts the block entity rendering to apply culling
     */
    @Inject(method = "renderBlockEntities", at = @At("HEAD"))
    private void onRenderBlockEntities(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                       Camera camera, float tickDelta, CallbackInfo ci) {
        if (!FusionMCMod.INSTANCE.getEnableBlockEntityCulling()) return;

        World world = this.client.world;
        if (world == null) return;

        // Get block entities from loaded chunks
        List<BlockEntity> blockEntities = new ArrayList<>();

        // Iterate through loaded chunks to get block entities
        world.getChunkManager().getLoadedChunks().forEach(chunk -> {
            chunk.getBlockEntities().values().forEach(blockEntity -> {
                blockEntities.add(blockEntity);
            });
        });

        if (blockEntities.isEmpty()) return;

        Frustum frustum = new Frustum(matrices.peek().getPositionMatrix(), matrices.peek().getNormalMatrix());
        var result = FusionMCMod.INSTANCE.getFrustumChecker().checkBlockEntities(frustum, blockEntities);

        // Apply culling results
        FusionMCMod.INSTANCE.getCullingManager().updateBlockEntityCulling(result);

        if (FusionMCMod.INSTANCE.getDebugMode()) {
            System.out.println("[FusionMC Debug] Block Entities - Visible: " +
                    (result.getVisibleHigh().size() + result.getVisibleMedium().size() + result.getVisibleLow().size()) +
                    ", Culled: " + result.getCulled().size());
        }
    }

    /**
     * Intercepts the chunk rendering to apply culling
     */
    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void onSetupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci) {
        if (!FusionMCMod.INSTANCE.getEnableChunkCulling()) return;

        World world = this.client.world;
        if (world == null) return;

        Vec3d cameraPos = camera.getPos();
        performChunkCulling(frustum, cameraPos, world);
    }

    /**
     * Intercepts the entity rendering to apply culling
     */
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void onRenderEntities(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                  Camera camera, float tickDelta, CallbackInfo ci) {
        if (!FusionMCMod.INSTANCE.getEnableDistanceCulling()) return;

        World world = this.client.world;
        if (world == null) return;

        Vec3d cameraPos = camera.getPos();
        performEntityDistanceCulling(cameraPos, world);
    }

    @Unique
    private void performChunkCulling(Frustum frustum, Vec3d cameraPos, World world) {
        int renderDistance = this.client.options.getViewDistance().getValue();
        int playerChunkX = (int)(cameraPos.x / 16);
        int playerChunkZ = (int)(cameraPos.z / 16);

        int visibleChunks = 0;
        int culledChunks = 0;

        for (int x = playerChunkX - renderDistance; x <= playerChunkX + renderDistance; x++) {
            for (int z = playerChunkZ - renderDistance; z <= playerChunkZ + renderDistance; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);

                Boolean cachedResult = FusionMCMod.INSTANCE.getFrustumCache().getCachedChunkResult(chunkPos);
                boolean isVisible = cachedResult != null ? cachedResult :
                        FusionMCMod.INSTANCE.getFrustumChecker().checkChunkVisibilityWithDistance(frustum, x, z, cameraPos);

                FusionMCMod.INSTANCE.getFrustumCache().cacheChunkResult(chunkPos, isVisible);

                if (isVisible) visibleChunks++;
                else culledChunks++;
            }
        }

        if (FusionMCMod.INSTANCE.getDebugMode()) {
            System.out.println("[FusionMC Debug] Chunks - Visible: " + visibleChunks + ", Culled: " + culledChunks);
        }
    }

    @Unique
    private void performEntityDistanceCulling(Vec3d cameraPos, World world) {
        List<Entity> entities = new ArrayList<>();

        // Use getEntitiesByClass to get all entities in the world
        world.getEntitiesByClass(Entity.class, world.getBoundingBox(), entity -> true)
                .forEach(entity -> entities.add(entity));

        double maxDistance = this.client.options.getViewDistance().getValue() * 16.0;

        int visibleEntities = 0;
        int culledEntities = 0;

        for (Entity entity : entities) {
            double distance = entity.getPos().distanceTo(cameraPos);

            if (distance <= maxDistance) visibleEntities++;
            else culledEntities++;
        }

        if (FusionMCMod.INSTANCE.getDebugMode()) {
            System.out.println("[FusionMC Debug] Entities - Visible: " + visibleEntities + ", Culled: " + culledEntities);
        }
    }
}