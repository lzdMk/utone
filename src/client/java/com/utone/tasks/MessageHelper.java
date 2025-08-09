package com.utone.tasks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Provides standardized message methods for task-related notifications
 */
public interface MessageHelper {
    /**
     * Send an informational message to the chat
     */
    default void sendInfoMessage(String prefix, String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
            Text.literal("[" + prefix + "] " + message).styled(style -> style.withColor(Formatting.AQUA))
        );
    }
    
    /**
     * Send a success message to the chat
     */
    default void sendSuccessMessage(String prefix, String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
            Text.literal("[" + prefix + "] " + message).styled(style -> style.withColor(Formatting.GREEN))
        );
    }
    
    /**
     * Send an error message to the chat
     */
    default void sendErrorMessage(String prefix, String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
            Text.literal("[" + prefix + "] Error: " + message).styled(style -> style.withColor(Formatting.RED))
        );
    }
}
