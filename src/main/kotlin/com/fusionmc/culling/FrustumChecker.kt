package com.fusionmc.culling

import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.Frustum
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Box
import net.minecraft.util.math.ChunkPos

/**
 * Class responsible for checking if objects are within the view frustum
 */
class FrustumChecker {

    /**
     * Checks which block entities are visible in the frustum
     */
    fun checkBlockEntities(frustum: Frustum, blockEntities: List<BlockEntity>): CullingResult {
        val visibleHigh = mutableListOf<BlockEntity>()
        val visibleMedium = mutableListOf<BlockEntity>()
        val visibleLow = mutableListOf<BlockEntity>()
        val culled = mutableListOf<BlockEntity>()

        for (blockEntity in blockEntities) {
            val pos = blockEntity.pos
            val box = Box(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)

            if (frustum.isVisible(box)) {
                // Classify by priority based on block entity type
                when {
                    isHighPriorityBlockEntity(blockEntity) -> visibleHigh.add(blockEntity)
                    isMediumPriorityBlockEntity(blockEntity) -> visibleMedium.add(blockEntity)
                    else -> visibleLow.add(blockEntity)
                }
            } else {
                culled.add(blockEntity)
            }
        }

        return CullingResult(visibleHigh, visibleMedium, visibleLow, culled)
    }

    /**
     * Checks if a chunk is visible considering distance
     */
    fun checkChunkVisibilityWithDistance(frustum: Frustum, chunkX: Int, chunkZ: Int, cameraPos: Vec3d): Boolean {
        // Create chunk bounding box (16x16x384 blocks)
        val minX = chunkX * 16.0
        val maxX = minX + 16.0
        val minZ = chunkZ * 16.0
        val maxZ = minZ + 16.0

        val chunkBox = Box(minX, -64.0, minZ, maxX, 320.0, maxZ)

        // Check if it's in the frustum
        if (!frustum.isVisible(chunkBox)) {
            return false
        }

        // Check the distance from chunk center to camera
        val chunkCenterX = minX + 8.0
        val chunkCenterZ = minZ + 8.0
        val distanceSquared = (chunkCenterX - cameraPos.x) * (chunkCenterX - cameraPos.x) +
                (chunkCenterZ - cameraPos.z) * (chunkCenterZ - cameraPos.z)

        // Consider visible if within render distance
        val maxDistanceSquared = (16.0 * 16.0) * (16.0 * 16.0) // 16 chunks distance
        return distanceSquared <= maxDistanceSquared
    }

    /**
     * Checks if an entity is visible in the frustum
     */
    fun checkEntityVisibility(frustum: Frustum, entityPos: Vec3d, entityBox: Box): Boolean {
        return frustum.isVisible(entityBox)
    }

    /**
     * Checks if it's a high priority block entity (always render)
     */
    private fun isHighPriorityBlockEntity(blockEntity: BlockEntity): Boolean {
        val blockName = blockEntity.type.toString()
        return blockName.contains("chest") ||
                blockName.contains("beacon") ||
                blockName.contains("conduit") ||
                blockName.contains("end_gateway")
    }

    /**
     * Checks if it's a medium priority block entity
     */
    private fun isMediumPriorityBlockEntity(blockEntity: BlockEntity): Boolean {
        val blockName = blockEntity.type.toString()
        return blockName.contains("furnace") ||
                blockName.contains("brewing_stand") ||
                blockName.contains("enchanting_table") ||
                blockName.contains("anvil")
    }
}

/**
 * Class for storing culling results
 */
data class CullingResult(
    val visibleHigh: List<BlockEntity>,
    val visibleMedium: List<BlockEntity>,
    val visibleLow: List<BlockEntity>,
    val culled: List<BlockEntity>
)