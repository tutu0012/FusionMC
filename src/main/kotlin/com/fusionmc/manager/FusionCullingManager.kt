package com.fusionmc.manager

import com.fusionmc.culling.CullingResult
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.util.math.ChunkPos
import java.util.concurrent.ConcurrentHashMap

/**
 * Culling manager that maintains state of culled objects
 */
class FusionCullingManager {

    // Sets to track culled objects
    private val culledBlockEntities = ConcurrentHashMap.newKeySet<BlockEntity>()
    private val culledEntities = ConcurrentHashMap.newKeySet<Entity>()
    private val culledChunks = ConcurrentHashMap.newKeySet<ChunkPos>()

    // Counters for statistics
    private var totalBlockEntitiesProcessed = 0
    private var totalEntitiesProcessed = 0
    private var totalChunksProcessed = 0

    /**
     * Updates culling state for block entities
     */
    fun updateBlockEntityCulling(result: CullingResult) {
        // Clear previous state
        culledBlockEntities.clear()

        // Add culled block entities
        culledBlockEntities.addAll(result.culled)

        // Update statistics
        totalBlockEntitiesProcessed = result.visibleHigh.size +
                result.visibleMedium.size +
                result.visibleLow.size +
                result.culled.size
    }

    /**
     * Updates culling state for entities
     */
    fun updateEntityCulling(visibleEntities: List<Entity>, culledEntities: List<Entity>) {
        // Clear previous state
        this.culledEntities.clear()

        // Add culled entities
        this.culledEntities.addAll(culledEntities)

        // Update statistics
        totalEntitiesProcessed = visibleEntities.size + culledEntities.size
    }

    /**
     * Updates culling state for chunks
     */
    fun updateChunkCulling(visibleChunks: List<ChunkPos>, culledChunks: List<ChunkPos>) {
        // Clear previous state
        this.culledChunks.clear()

        // Add culled chunks
        this.culledChunks.addAll(culledChunks)

        // Update statistics
        totalChunksProcessed = visibleChunks.size + culledChunks.size
    }

    /**
     * Checks if a block entity is culled
     */
    fun isBlockEntityCulled(blockEntity: BlockEntity): Boolean {
        return culledBlockEntities.contains(blockEntity)
    }

    /**
     * Checks if an entity is culled
     */
    fun isEntityCulled(entity: Entity): Boolean {
        return culledEntities.contains(entity)
    }

    /**
     * Checks if a chunk is culled
     */
    fun isChunkCulled(chunkPos: ChunkPos): Boolean {
        return culledChunks.contains(chunkPos)
    }

    /**
     * Gets culling statistics
     */
    fun getStats(): CullingStats {
        return CullingStats(
            blockEntitiesVisible = totalBlockEntitiesProcessed - culledBlockEntities.size,
            blockEntitiesCulled = culledBlockEntities.size,
            entitiesVisible = totalEntitiesProcessed - culledEntities.size,
            entitiesCulled = culledEntities.size,
            chunksVisible = totalChunksProcessed - culledChunks.size,
            chunksCulled = culledChunks.size
        )
    }

    /**
     * Clears all culling data
     */
    fun clearAll() {
        culledBlockEntities.clear()
        culledEntities.clear()
        culledChunks.clear()
        totalBlockEntitiesProcessed = 0
        totalEntitiesProcessed = 0
        totalChunksProcessed = 0
    }

    /**
     * Applies Level of Detail (LOD) logic based on distance
     */
    fun getLODLevel(distance: Double): LODLevel {
        return when {
            distance < 32.0 -> LODLevel.HIGH
            distance < 64.0 -> LODLevel.MEDIUM
            distance < 128.0 -> LODLevel.LOW
            else -> LODLevel.NONE
        }
    }

    /**
     * Checks if an object should be rendered based on LOD
     */
    fun shouldRenderWithLOD(distance: Double, objectType: ObjectType): Boolean {
        val lodLevel = getLODLevel(distance)

        return when (objectType) {
            ObjectType.BLOCK_ENTITY -> lodLevel != LODLevel.NONE
            ObjectType.ENTITY -> lodLevel == LODLevel.HIGH || lodLevel == LODLevel.MEDIUM
            ObjectType.PARTICLE -> lodLevel == LODLevel.HIGH
            ObjectType.CHUNK_DETAIL -> lodLevel == LODLevel.HIGH
        }
    }
}

/**
 * Culling statistics
 */
data class CullingStats(
    val blockEntitiesVisible: Int,
    val blockEntitiesCulled: Int,
    val entitiesVisible: Int,
    val entitiesCulled: Int,
    val chunksVisible: Int,
    val chunksCulled: Int
) {
    fun getTotalObjects(): Int = blockEntitiesVisible + blockEntitiesCulled + entitiesVisible + entitiesCulled
    fun getCullingEfficiency(): Double = if (getTotalObjects() > 0) (blockEntitiesCulled + entitiesCulled).toDouble() / getTotalObjects() else 0.0
}

/**
 * Level of Detail levels
 */
enum class LODLevel {
    HIGH,    // Full rendering
    MEDIUM,  // Reduced rendering
    LOW,     // Minimal rendering
    NONE     // Don't render
}

/**
 * Object types for LOD
 */
enum class ObjectType {
    BLOCK_ENTITY,
    ENTITY,
    PARTICLE,
    CHUNK_DETAIL
}