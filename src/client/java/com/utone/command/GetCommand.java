package com.utone.command;

import com.utone.tasks.MessageHelper;
import com.utone.tasks.MiningTask;
import com.utone.tasks.TaskManager;
import com.utone.util.BaritoneHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command handler for the #get command which mines a specified amount of blocks
 */
public class GetCommand implements MessageHelper {
    private static final String PREFIX = "Utone";
    private static final Pattern GET_PATTERN = Pattern.compile("#get\\s+(\\d+)\\s+(.+)");
    private static final int MAX_TASKS = 5;
    private static GetCommand INSTANCE;

    private GetCommand() {
        // Private constructor for singleton pattern
    }

    public static void register() {
        // Initialize the task manager
        TaskManager.init();
        
        // Create the singleton instance
        INSTANCE = new GetCommand();
        
        // Listen for chat messages and handle #get command
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            String trimmed = message.trim();
            Matcher matcher = GET_PATTERN.matcher(trimmed);
            
            if (matcher.matches()) {
                try {
                    // Extract amount and item name from the command
                    int amount = Integer.parseInt(matcher.group(1));
                    String blockName = matcher.group(2);
                    
                    // Handle the get command
                    INSTANCE.handleGetCommand(amount, blockName);
                    
                    // Return false to prevent the message from being sent to the server
                    return false;
                } catch (NumberFormatException e) {
                    INSTANCE.sendErrorMessage(PREFIX, "Invalid number format in #get command");
                    return false;
                } catch (Exception e) {
                    INSTANCE.sendErrorMessage(PREFIX, "Error processing #get command: " + e.getMessage());
                    return false;
                }
            }
            
            return true;
        });
    }
    
    /**
     * Handle the #get command
     */
    private void handleGetCommand(int amount, String blockName) {
        // Check if Baritone is available before processing any commands
        if (!BaritoneHelper.isBaritoneAvailable()) {
            sendErrorMessage(PREFIX, "Baritone is not available. Please install Baritone and restart the game.");
            return;
        }
        
        // Check if the task queue is full
        if (TaskManager.getQueuedTaskCount() >= MAX_TASKS) {
            sendErrorMessage(PREFIX, "Task queue is full (max " + MAX_TASKS + "). Please wait for current tasks to complete.");
            return;
        }
        
        // Create a new mining task
        MiningTask task = new MiningTask(blockName, amount);
        
        // Queue the task
        if (TaskManager.queueTask(task)) {
            if (TaskManager.getQueuedTaskCount() > 1) {
                sendInfoMessage(PREFIX, "Queued mining task for " + amount + " " + blockName + 
                               " (position: " + TaskManager.getQueuedTaskCount() + " in queue)");
            } else {
                sendInfoMessage(PREFIX, "Starting to collect " + amount + " " + blockName);
            }
        } else {
            sendErrorMessage(PREFIX, "Failed to queue mining task");
        }
    }
    
    // MessageHelper interface methods are used directly
}
