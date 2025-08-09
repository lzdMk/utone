package com.utone.util;

import com.utone.tasks.MessageHelper;
import com.utone.tasks.TaskManager;
import com.utone.command.ContinueCommand;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

/**
 * Central command handler that intercepts Baritone commands and manages task state
 */
public class CommandInterceptor implements MessageHelper {
    private static final String PREFIX = "Utone";
    private static CommandInterceptor INSTANCE;
    private static boolean isRegistered = false;
    
    private CommandInterceptor() {
        // Private constructor for singleton pattern
    }
    
    /**
     * Register the command interceptor (should be called once during mod initialization)
     */
    public static void register() {
        if (isRegistered) return;
        
        INSTANCE = new CommandInterceptor();
        
        // Register as the first listener with highest priority
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            return INSTANCE.handleCommand(message);
        });
        
        isRegistered = true;
    }
    
    /**
     * Handle incoming commands and check for Baritone control commands
     * @param message The command message
     * @return true to allow the command to continue, false to block it
     */
    private boolean handleCommand(String message) {
        String trimmed = message.trim();
        
        // Check if this is a Baritone stop command
        if (BaritoneHelper.isBaritoneStopCommand(trimmed)) {
            if (TaskManager.hasActiveTask() || TaskManager.getQueuedTaskCount() > 0) {
                TaskManager.cancelAllTasks("Stop command");
                sendInfoMessage(PREFIX, "All tasks stopped");
            }
            return true; // Let the command pass through to Baritone
        }
        
        // Check if this is a Baritone pause command
        if (BaritoneHelper.isBaritonePauseCommand(trimmed)) {
            if (TaskManager.hasActiveTask() || TaskManager.getQueuedTaskCount() > 0) {
                // We fully manage pause state ourselves. Block the command so Baritone doesn't set its own internal paused flag.
                if (TaskManager.isPaused()) {
                    TaskManager.resumeAllTasks();
                } else {
                    TaskManager.pauseAllTasks();
                }
                return false; // Block so Baritone internal pause state doesn't diverge
            }
            // No tasks under our control; allow user to pause native Baritone behavior if they wish
            return true;
        }
        
        // Check if this is a Baritone resume command - intercept and use our continue functionality
        if (BaritoneHelper.isBaritoneResumeCommand(trimmed)) {
            ContinueCommand.handleContinueCommand();
            return false; // Block the command from going to Baritone since we handle it
        }
        
        // Allow all other commands to continue
        return true;
    }
}
