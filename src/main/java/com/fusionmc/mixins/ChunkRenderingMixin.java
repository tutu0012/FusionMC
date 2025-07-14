package com.fusionmc.mixins;

import com.fusionmc.FusionMCMod;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class ChunkRenderingMixin {
    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void onSetupTerrain(CallbackInfo ci) {
        if (!FusionMCMod.isChunkRenderingOptimizationEnabled()) {
            return;
        }

        // Optimize chunk rendering by reducing update frequency
        if (FusionMCMod.isDistanceCullingEnabled()) {
            // Skip terrain setup on some frames for better performance
            if (System.currentTimeMillis() % 3 == 0) {
                return;
            }
        }
    }

    @Inject(method = "scheduleTerrainUpdate", at = @At("HEAD"), cancellable = true)
    private void onScheduleTerrainUpdate(CallbackInfo ci) {
        if (!FusionMCMod.isChunkRenderingOptimizationEnabled()) {
            return;
        }

        // Reduce terrain update frequency for better performance
        if (System.currentTimeMillis() % 2 == 0) {
            ci.cancel();
        }
    }
}