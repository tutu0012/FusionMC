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

                boolean isVisible = frustum.isVisible(x * 16, 0, z * 16, (x + 1) * 16, 256, (z + 1) * 16);

                if (isVisible) visibleChunks++;
                else culledChunks++;
            }
        }

        if (FusionMCMod.INSTANCE.getDebugMode()) {
            System.out.println("[FusionMC Debug] Chunks - Visible: " + visibleChunks + ", Culled: " + culledChunks);
        }
    }


}