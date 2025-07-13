package com.fusionmc.culling

import net.minecraft.util.math.ChunkPos
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache for frustum culling results to improve performance
 */
class FrustumCache {

    private val chunkCache = ConcurrentHashMap<ChunkPos, CachedChunkResult>()
    private val entityCache = ConcurrentHashMap<Int, CachedEntityResult>()

    // Cache settings
    private val maxCacheSize = 1000
    private val cacheExpiryTime = 5000L // 5 seconds

    /**
     * Gets cached result for a chunk
     */
    fun getCachedChunkResult(chunkPos: ChunkPos): Boolean? {
        val cached = chunkCache[chunkPos]

        if (cached != null && !cached.isExpired()) {
            return cached.isVisible
        }

        // Remove expired entry
        if (cached != null && cached.isExpired()) {
            chunkCache.remove(chunkPos)
        }

        return null
    }

    /**
     * Stores chunk result in cache
     */
    fun cacheChunkResult(chunkPos: ChunkPos, isVisible: Boolean) {
        // Clear cache if too large
        if (chunkCache.size >= maxCacheSize) {
            clearExpiredChunkEntries()
        }

        chunkCache[chunkPos] = CachedChunkResult(isVisible, System.currentTimeMillis())
    }

    /**
     * Gets cached result for an entity
     */
    fun getCachedEntityResult(entityId: Int): Boolean? {
        val cached = entityCache[entityId]

        if (cached != null && !cached.isExpired()) {
            return cached.isVisible
        }

        // Remove expired entry
        if (cached != null && cached.isExpired()) {
            entityCache.remove(entityId)
        }

        return null
    }

    /**
     * Stores entity result in cache
     */
    fun cacheEntityResult(entityId: Int, isVisible: Boolean) {
        // Clear cache if too large
        if (entityCache.size >= maxCacheSize) {
            clearExpiredEntityEntries()
        }

        entityCache[entityId] = CachedEntityResult(isVisible, System.currentTimeMillis())
    }

    /**
     * Clears expired chunk cache entries
     */
    private fun clearExpiredChunkEntries() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = chunkCache.entries
            .filter { it.value.isExpired(currentTime) }
            .map { it.key }

        expiredKeys.forEach { chunkCache.remove(it) }
    }

    /**
     * Clears expired entity cache entries
     */
    private fun clearExpiredEntityEntries() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = entityCache.entries
            .filter { it.value.isExpired(currentTime) }
            .map { it.key }

        expiredKeys.forEach { entityCache.remove(it) }
    }

    /**
     * Clears all cache
     */
    fun clearAll() {
        chunkCache.clear()
        entityCache.clear()
    }

    fun clear() {
        clearAll()
    }

    /**
     * Gets cache statistics
     */
    fun getStats(): CacheStats {
        return CacheStats(
            chunkCacheSize = chunkCache.size,
            entityCacheSize = entityCache.size,
            maxCacheSize = maxCacheSize
        )
    }

    /**
     * Cached result for chunk
     */
    private data class CachedChunkResult(
        val isVisible: Boolean,
        val timestamp: Long
    ) {
        fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
            return currentTime - timestamp > 5000L // 5 seconds
        }
    }

    /**
     * Cached result for entity
     */
    private data class CachedEntityResult(
        val isVisible: Boolean,
        val timestamp: Long
    ) {
        fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
            return currentTime - timestamp > 3000L // 3 seconds (entities move more)
        }
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val chunkCacheSize: Int,
    val entityCacheSize: Int,
    val maxCacheSize: Int
)