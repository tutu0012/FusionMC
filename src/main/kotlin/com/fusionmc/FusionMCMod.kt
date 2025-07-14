package com.fusionmc

import com.fusionmc.culling.FrustumChecker
import com.fusionmc.culling.FrustumCache
import com.fusionmc.manager.FusionCullingManager
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
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
        const val VERSION = "0.1.1d-beta+mc1.20.1"
        
        // --- Static getters for mixins ---
        @JvmStatic
        fun isChunkCullingEnabled(): Boolean = INSTANCE.enableChunkCulling
        @JvmStatic
        fun isDistanceCullingEnabled(): Boolean = INSTANCE.enableDistanceCulling
        @JvmStatic
        fun getRenderDistance(): Int = INSTANCE.maxRenderDistance
        @JvmStatic
        fun isChunkRenderingOptimizationEnabled(): Boolean = INSTANCE.enableChunkRenderingOptimization
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
    var enableChunkRenderingOptimization = true
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

        // Register commands
        registerCommands()

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
     * Registers client commands
     */
    private fun registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            // Register /fusion command
            dispatcher.register(
                ClientCommandManager.literal("fusion")
                    .executes { context ->
                        showHelp()
                        1
                    }
                    .then(
                        ClientCommandManager.literal("help")
                            .executes { context ->
                                showHelp()
                                1
                            }
                    )
                    .then(
                        ClientCommandManager.literal("toggle")
                            .then(
                                ClientCommandManager.literal("frustum")
                                    .executes { context ->
                                        val enabled = toggleFrustumCulling()
                                        sendMessage("§6Frustum Culling: ${if (enabled) "§aON" else "§cOFF"}")
                                        1
                                    }
                            )
                            .then(
                                ClientCommandManager.literal("distance")
                                    .executes { context ->
                                        val enabled = toggleDistanceCulling()
                                        sendMessage("§6Distance Culling: ${if (enabled) "§aON" else "§cOFF"}")
                                        1
                                    }
                            )
                            .then(
                                ClientCommandManager.literal("blockentity")
                                    .executes { context ->
                                        val enabled = toggleBlockEntityCulling()
                                        sendMessage("§6Block Entity Culling: ${if (enabled) "§aON" else "§cOFF"}")
                                        1
                                    }
                            )
                            .then(
                                ClientCommandManager.literal("chunk")
                                    .executes { context ->
                                        val enabled = toggleChunkCulling()
                                        sendMessage("§6Chunk Culling: ${if (enabled) "§aON" else "§cOFF"}")
                                        1
                                    }
                            )
                            .then(
                                ClientCommandManager.literal("debug")
                                    .executes { context ->
                                        val enabled = toggleDebugMode()
                                        sendMessage("§6Debug Mode: ${if (enabled) "§aON" else "§cOFF"}")
                                        1
                                    }
                            )
                            .then(
                                ClientCommandManager.literal("chunkrendering")
                                    .executes { context ->
                                        val enabled = toggleChunkRenderingOptimization()
                                        sendMessage("§6Chunk Rendering Optimization: ${if (enabled) "§aON" else "§cOFF"}")
                                        1
                                    }
                            )
                    )
                    .then(
                        ClientCommandManager.literal("status")
                            .executes { context ->
                                showStatus()
                                1
                            }
                    )
                    .then(
                        ClientCommandManager.literal("stats")
                            .executes { context ->
                                showStats()
                                1
                            }
                    )
                    .then(
                        ClientCommandManager.literal("clear")
                            .executes { context ->
                                clearCache()
                                sendMessage("§aCache cleaned successfully!")
                                1
                            }
                    )
                    .then(
                        ClientCommandManager.literal("reload")
                            .executes { context ->
                                clearCache()
                                sendMessage("§aConfiguration reloaded!")
                                1
                            }
                    )
            )
        }
        println("[$MOD_NAME] Commands registered successfully")
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

    fun toggleChunkRenderingOptimization(): Boolean {
        enableChunkRenderingOptimization = !enableChunkRenderingOptimization
        return enableChunkRenderingOptimization
    }

    // --- Cache cleaner ---
    fun clearCache() {
        frustumCache.clearAll()
    }



    // --- Command helpers ---
    private fun sendMessage(message: String) {
        net.minecraft.client.MinecraftClient.getInstance().player?.sendMessage(
            Text.literal(message), false
        )
    }

    private fun showHelp() {
        sendMessage("§6=== FusionMC Commands ===")
        sendMessage("§f/fusion help §7- Show this help")
        sendMessage("§f/fusion toggle <type> §7- Toggle a functionality")
        sendMessage("§f/fusion status §7- Show current status")
        sendMessage("§f/fusion stats §7- Show culling statistics")
        sendMessage("§f/fusion clear §7- Clear culling cache")
        sendMessage("§f/fusion reload §7- Reload configurations")
        sendMessage("§7Types: frustum, distance, blockentity, chunk, debug, chunkrendering")
    }

    private fun showStatus() {
        sendMessage("§6=== FusionMC Status ===")
        sendMessage("§fMod version: $VERSION")
        sendMessage("§fFrustum Culling: ${if (enableFrustumCulling) "§aON" else "§cOFF"}")
        sendMessage("§fDistance Culling: ${if (enableDistanceCulling) "§aON" else "§cOFF"}")
        sendMessage("§fBlock Entity Culling: ${if (enableBlockEntityCulling) "§aON" else "§cOFF"}")
        sendMessage("§fChunk Culling: ${if (enableChunkCulling) "§aON" else "§cOFF"}")
        sendMessage("§fDebug Mode: ${if (debugMode) "§aON" else "§cOFF"}")
        sendMessage("§fChunk Rendering Optimization: ${if (enableChunkRenderingOptimization) "§aON" else "§cOFF"}")
    }

    private fun showStats() {
        val stats = cullingManager.getStats()
        val cacheStats = frustumCache.getStats()
        val efficiency = stats.getCullingEfficiency()

        sendMessage("§6=== FusionMC Statistics ===")
        sendMessage("§fBlock Entities: §a${stats.blockEntitiesVisible} §7visible / §c${stats.blockEntitiesCulled} §7culled")
        sendMessage("§fChunks: §a${stats.chunksVisible} §7visible / §c${stats.chunksCulled} §7culled")
        sendMessage("§fEntities: §a${stats.entitiesVisible} §7visible / §c${stats.entitiesCulled} §7culled")
        sendMessage("§fCache: §e${cacheStats.entityCacheSize} §7entities / §e${cacheStats.chunkCacheSize} §7chunks")
        sendMessage("§fEfficiency: §b${"%.1f".format(efficiency * 100)}%")
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