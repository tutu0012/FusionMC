package com.fusionmc

import com.fusionmc.culling.FrustumChecker
import com.fusionmc.culling.FrustumCache
import com.fusionmc.manager.FusionCullingManager
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

/**
 * Main class of FusionMC Mod
 */
class FusionMCMod : ClientModInitializer {

    companion object {
        @JvmField
        val INSTANCE = FusionMCMod()

        const val MOD_ID = "fusionmc"
        const val MOD_NAME = "FusionMC"
        const val VERSION = "0.1.0e-beta+mc1.20.1"
    }

    // Mod settings
    var enableBlockEntityCulling = true
        private set
    var enableChunkCulling = true
        private set
    var enableDistanceCulling = true
        private set
    var enableLOD = true
        private set
    var debugMode = false
        private set
    var maxRenderDistance = 16
        private set
    var cullingUpdateInterval = 5 // ticks
        private set
    var enableFrustumCulling = true
        private set

    // Main components
    lateinit var frustumChecker: FrustumChecker
        private set
    lateinit var frustumCache: FrustumCache
        private set
    lateinit var cullingManager: FusionCullingManager
        private set

    // Keybindings
    private lateinit var toggleDebugKey: KeyBinding
    private lateinit var toggleCullingKey: KeyBinding
    private lateinit var showStatsKey: KeyBinding

    // Time control
    private var tickCounter = 0
    private var lastStatsUpdate = 0L

    override fun onInitializeClient() {
        println("[$MOD_NAME] Initializing version $VERSION")

        // Initialize components
        initializeComponents()

        // Register keybindings
        registerKeyBindings()

        // Register events
        registerEvents()

        println("[$MOD_NAME] Initialization complete!")
    }

    /**
     * Initializes main mod parts
     */
    private fun initializeComponents() {
        frustumChecker = FrustumChecker()
        frustumCache = FrustumCache()
        cullingManager = FusionCullingManager()

        println("[$MOD_NAME] Components initialized")
    }

    /**
     * Registers keybindings
     */
    private fun registerKeyBindings() {
        toggleDebugKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.fusionmc.toggle_debug",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F3,
                "category.fusionmc.general"
            )
        )

        toggleCullingKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.fusionmc.toggle_culling",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                "category.fusionmc.general"
            )
        )

        showStatsKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.fusionmc.show_stats",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F5,
                "category.fusionmc.general"
            )
        )

        println("[$MOD_NAME] Successfully registered keybinds")
    }

    /**
     * Registers client events
     */
    private fun registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            tickCounter++

            // Process key inputs
            processKeyInput(client)

            // Updates culling system periodically
            if (tickCounter % cullingUpdateInterval == 0) {
                updateCullingSystem()
            }

            // Shows statistics every second
            if (debugMode && System.currentTimeMillis() - lastStatsUpdate > 1000) {
                showDetailedStats(client)
                lastStatsUpdate = System.currentTimeMillis()
            }
        }
    }

    /**
     * Processes key inputs
     */
    private fun processKeyInput(client: net.minecraft.client.MinecraftClient) {
        while (toggleDebugKey.wasPressed()) {
            debugMode = !debugMode
            client.player?.sendMessage(
                Text.literal("[$MOD_NAME] Debug Mode: ${if (debugMode) "ON" else "OFF"}"),
                false
            )
        }

        while (toggleCullingKey.wasPressed()) {
            val newState = !enableBlockEntityCulling
            enableBlockEntityCulling = newState
            enableChunkCulling = newState
            enableDistanceCulling = newState

            client.player?.sendMessage(
                Text.literal("[$MOD_NAME] Culling: ${if (newState) "ON" else "OFF"}"),
                false
            )
        }

        while (showStatsKey.wasPressed()) {
            showStatsInChat(client)
        }
    }

    /**
     * Updates culling system
     */
    private fun updateCullingSystem() {
        // Cleans expired cache
        val cacheStats = frustumCache.getStats()
        if (cacheStats.chunkCacheSize > 500) {
            // Cache too big, cleans old entries
            frustumCache.clearAll()
        }

        if (debugMode) {
            println("[$MOD_NAME] Cache updated - Chunks: ${cacheStats.chunkCacheSize}, Entities: ${cacheStats.entityCacheSize}")
        }
    }

    /**
     * Shows detailed statistics on console
     */
    private fun showDetailedStats(client: net.minecraft.client.MinecraftClient) {
        val cullingStats = cullingManager.getStats()
        val cacheStats = frustumCache.getStats()

        println("=== FusionMC Stats ===")
        println("Block Entities - Visible: ${cullingStats.blockEntitiesVisible}, Culled: ${cullingStats.blockEntitiesCulled}")
        println("Entities - Visible: ${cullingStats.entitiesVisible}, Culled: ${cullingStats.entitiesCulled}")
        println("Chunks - Visible: ${cullingStats.chunksVisible}, Culled: ${cullingStats.chunksCulled}")
        println("Cache - Chunks: ${cacheStats.chunkCacheSize}, Entities: ${cacheStats.entityCacheSize}")
        println("Efficiency: ${"%.2f".format(cullingStats.getCullingEfficiency() * 100)}%")
        println("======================")
    }

    /**
     * Shows statistics on chat
     */
    private fun showStatsInChat(client: net.minecraft.client.MinecraftClient) {
        val cullingStats = cullingManager.getStats()
        val efficiency = cullingStats.getCullingEfficiency() * 100

        client.player?.sendMessage(
            Text.literal("[$MOD_NAME] Culling Efficiency: ${"%.1f".format(efficiency)}% | Objects Culled: ${cullingStats.blockEntitiesCulled + cullingStats.entitiesCulled}"),
            false
        )
    }

    /**
     * Gets current configurations
     */
    fun getConfig(): FusionMCConfig {
        return FusionMCConfig(
            enableBlockEntityCulling = enableBlockEntityCulling,
            enableChunkCulling = enableChunkCulling,
            enableDistanceCulling = enableDistanceCulling,
            enableLOD = enableLOD,
            debugMode = debugMode,
            maxRenderDistance = maxRenderDistance,
            cullingUpdateInterval = cullingUpdateInterval
        )
    }

    /**
     * Applies configurations
     */
    fun applyConfig(config: FusionMCConfig) {
        enableBlockEntityCulling = config.enableBlockEntityCulling
        enableChunkCulling = config.enableChunkCulling
        enableDistanceCulling = config.enableDistanceCulling
        enableLOD = config.enableLOD
        debugMode = config.debugMode
        maxRenderDistance = config.maxRenderDistance
        cullingUpdateInterval = config.cullingUpdateInterval

        println("[$MOD_NAME] Configurations applied")
    }

    // --- Culling toggles ---
    fun toggleFrustumCulling(): Boolean {
        enableFrustumCulling = !enableFrustumCulling
        return enableFrustumCulling
    }

    fun toggleDistanceCulling(): Boolean {
        enableDistanceCulling = !enableDistanceCulling
        return enableDistanceCulling
    }

    fun toggleBlockEntityCulling(): Boolean {
        enableBlockEntityCulling = !enableBlockEntityCulling
        return enableBlockEntityCulling
    }

    fun toggleChunkCulling(): Boolean {
        enableChunkCulling = !enableChunkCulling
        return enableChunkCulling
    }

    fun toggleDebugMode(): Boolean {
        debugMode = !debugMode
        return debugMode
    }

    // --- Cache cleaner ---
    fun clearCache() {
        frustumCache.clearAll()
    }
}

/**
 * Configuration class
 */
data class FusionMCConfig(
    val enableBlockEntityCulling: Boolean = true,
    val enableChunkCulling: Boolean = true,
    val enableDistanceCulling: Boolean = true,
    val enableLOD: Boolean = true,
    val debugMode: Boolean = false,
    val maxRenderDistance: Int = 16,
    val cullingUpdateInterval: Int = 5
)