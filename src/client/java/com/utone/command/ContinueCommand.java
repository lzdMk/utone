package com.utone.command;

import com.utone.tasks.MessageHelper;
import com.utone.tasks.TaskManager;
import com.utone.util.BaritoneHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.regex.Pattern;

/**
 * Command handler for the #continue command which resumes paused tasks
 */
public class ContinueCommand implements MessageHelper {
    private static final String PREFIX = "Utone";
    private static final Pattern CONTINUE_PATTERN = Pattern.compile("#continue");
    private static ContinueCommand INSTANCE;

    private ContinueCommand() {
        // Private constructor for singleton pattern
    }

    public static void register() {
        // Create the singleton instance
        INSTANCE = new ContinueCommand();
        
        // Listen for chat messages and handle #continue command
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            String trimmed = message.trim();
            
            if (CONTINUE_PATTERN.matcher(trimmed).matches()) {
                // Handle the continue command
                ContinueCommand.handleContinueCommand();
                
                // Return false to prevent the message from being sent to the server
                return false;
            }
            
            return true;
        });
    }
    
    /**
     * Handle the #continue command (also used by resume interceptor)
     */
    public static void handleContinueCommand() {
        if (INSTANCE == null) {
            INSTANCE = new ContinueCommand();
        }
        INSTANCE.executeContinue();
    }
    
    /**
     * Execute the continue/resume functionality
     */
    private void executeContinue() {
        // Check if Baritone is available before processing any commands
        if (!BaritoneHelper.isBaritoneAvailable()) {
            sendErrorMessage(PREFIX, "Baritone is not available. Cannot resume tasks.");
            return;
        }
        
        // Check if there are any tasks to resume
        if (!TaskManager.hasActiveTask() && TaskManager.getQueuedTaskCount() == 0) {
            sendInfoMessage(PREFIX, "No tasks to resume.");
            return;
        }
        
        // Use force resume to handle all cases
        TaskManager.forceResume();
    }
}
