package com.fusionmc.commands

import com.fusionmc.FusionMCMod
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text

object FusionCommands {

    // Map of supported commands to their handlers
    private val commands = mapOf(
        "fusionmc" to ::handleFusionMCCommand,
        "fusion" to ::handleFusionMCCommand
    )

    /**
     * Entry point for processing a command typed in the chat.
     */
    fun processCommand(message: String): Boolean {
        val parts = message.trim().split(" ")
        if (parts.isEmpty()) return false

        val command = parts[0].removePrefix("/").lowercase()
        val args = parts.drop(1)

        return commands[command]?.invoke(args) ?: false
    }

    /**
     * Handles all subcommands of /fusionmc
     */
    private fun handleFusionMCCommand(args: List<String>): Boolean {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return false

        if (args.isEmpty()) {
            showHelp(player)
            return true
        }

        when (args[0].lowercase()) {
            "help" -> showHelp(player)

            "toggle" -> {
                if (args.size < 2) {
                    sendMessage(player, "§cUse: /fusionmc toggle <frustum|distance|blockentity|chunk|debug>")
                } else {
                    // Toggles the specified functionality and sends feedback
                    when (args[1].lowercase()) {
                        "frustum" -> {
                            val enabled = FusionMCMod.INSTANCE.toggleFrustumCulling()
                            sendMessage(player, "§6Frustum Culling: ${if (enabled) "§aON" else "§cOFF"}")
                        }

                        "distance" -> {
                            val enabled = FusionMCMod.INSTANCE.toggleDistanceCulling()
                            sendMessage(player, "§6Distance Culling: ${if (enabled) "§aON" else "§cOFF"}")
                        }

                        "blockentity" -> {
                            val enabled = FusionMCMod.INSTANCE.toggleBlockEntityCulling()
                            sendMessage(player, "§6Block Entity Culling: ${if (enabled) "§aON" else "§cOFF"}")
                        }

                        "chunk" -> {
                            val enabled = FusionMCMod.INSTANCE.toggleChunkCulling()
                            sendMessage(player, "§6Chunk Culling: ${if (enabled) "§aON" else "§cOFF"}")
                        }

                        "debug" -> {
                            val enabled = FusionMCMod.INSTANCE.toggleDebugMode()
                            sendMessage(player, "§6Debug Mode: ${if (enabled) "§aON" else "§cOFF"}")
                        }

                        else -> {
                            sendMessage(player, "§cInvalid option! Use: frustum, distance, blockentity, chunk, debug")
                        }
                    }
                }
            }

            "status" -> showStatus(player)
            "stats" -> showStats(player)

            "clear" -> {
                FusionMCMod.INSTANCE.clearCache()
                sendMessage(player, "§aCache cleaned successfully!")
            }

            "reload" -> {
                FusionMCMod.INSTANCE.clearCache()
                sendMessage(player, "§aConfiguration reloaded!")
            }

            else -> {
                sendMessage(player, "§cInvalid Command! Use /fusionmc help for more command info.")
            }
        }

        return true
    }

    /**
     * Displays available commands to the player.
     */
    private fun showHelp(player: PlayerEntity) {
        sendMessage(player, "§6=== FusionMC Commands ===")
        sendMessage(player, "§f/fusionmc help §7- Show this help")
        sendMessage(player, "§f/fusionmc toggle <type> §7- Toggle a functionality")
        sendMessage(player, "§f/fusionmc status §7- Show current status")
        sendMessage(player, "§f/fusionmc stats §7- Show culling statistics")
        sendMessage(player, "§f/fusionmc clear §7- Clear culling cache")
        sendMessage(player, "§f/fusionmc reload §7- Reload configurations")
        sendMessage(player, "§7Types: frustum, distance, blockentity, chunk, debug")
    }

    /**
     * Shows the current configuration state to the player.
     */
    private fun showStatus(player: PlayerEntity) {
        sendMessage(player, "§6=== FusionMC Status ===")
        sendMessage(player, "§fDistance Culling: ${if (FusionMCMod.INSTANCE.enableDistanceCulling) "§aON" else "§cOFF"}")
        sendMessage(player, "§fBlock Entity Culling: ${if (FusionMCMod.INSTANCE.enableBlockEntityCulling) "§aON" else "§cOFF"}")
        sendMessage(player, "§fChunk Culling: ${if (FusionMCMod.INSTANCE.enableChunkCulling) "§aON" else "§cOFF"}")
        sendMessage(player, "§fDebug Mode: ${if (FusionMCMod.INSTANCE.debugMode) "§aON" else "§cOFF"}")
    }

    /**
     * Displays internal statistics like culled objects, visible objects, and cache size.
     */
    private fun showStats(player: PlayerEntity) {
        val stats = FusionMCMod.INSTANCE.cullingManager.getStats()
        val cacheStats = FusionMCMod.INSTANCE.frustumCache.getStats()
        val efficiency = stats.getCullingEfficiency()

        sendMessage(player, "§6=== FusionMC Statistics ===")
        sendMessage(player, "§fBlock Entities: §a${stats.blockEntitiesVisible} §7visible / §c${stats.blockEntitiesCulled} §7culled")
        sendMessage(player, "§fChunks: §a${stats.chunksVisible} §7visible / §c${stats.chunksCulled} §7culled")
        sendMessage(player, "§fEntities: §a${stats.entitiesVisible} §7visible / §c${stats.entitiesCulled} §7culled")
        sendMessage(player, "§fCache: §e${cacheStats.entityCacheSize} §7entities / §e${cacheStats.chunkCacheSize} §7chunks")
        sendMessage(player, "§fEfficiency: §b${"%.1f".format(efficiency * 100)}%")
    }

    /**
     * Utility to send a colored message to the player.
     */
    private fun sendMessage(player: PlayerEntity, message: String) {
        player.sendMessage(Text.literal(message), false)
    }
}