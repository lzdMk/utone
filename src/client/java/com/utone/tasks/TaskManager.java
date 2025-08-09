package com.utone.tasks;

import com.utone.util.BaritoneHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Central task manager that handles queueing, tracking, and managing all automation tasks.
 * Handles global events like player death or game exit.
 */
public class TaskManager implements MessageHelper {
    private static final String PREFIX = "Utone";
    private static final int MAX_QUEUED_TASKS = 5;
    private static boolean isInitialized = false;
    private static TaskManager INSTANCE;
    
    // Task queue for sequential processing
    private static final Queue<MiningTask> taskQueue = new LinkedList<>();
    private static MiningTask activeTask = null;
    private static boolean isPaused = false;
    
    private TaskManager() {
        // Private constructor for singleton pattern
    }
    
    /**
     * Initialize the task manager
     */
    public static void init() {
        if (isInitialized) return;
        
        // Create the singleton instance
        INSTANCE = new TaskManager();
        
        // Register client lifecycle events
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            cancelAllTasks("Game closing");
        });
        
        // Register tick event for checking player state and processing tasks
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            // Check if player died
            if (client.player.isDead()) {
                cancelAllTasks("Player died");
                // Force stop all Baritone processes when player dies
                try {
                    if (BaritoneHelper.isBaritoneAvailable()) {
                        BaritoneHelper.getBaritoneInstance().getPathingBehavior().forceCancel();
                        BaritoneHelper.getBaritoneInstance().getPathingBehavior().cancelEverything();
                    }
                } catch (Exception e) {
                    // Ignore exceptions here
                }
                return;
            }
            
            // Process task queue
            processTaskQueue(client);
        });
        
        isInitialized = true;
    }
    
    /**
     * Queue a new mining task
     * @param task The mining task to queue
     * @return true if task was queued, false if queue is full
     */
    public static boolean queueTask(MiningTask task) {
        if (taskQueue.size() >= MAX_QUEUED_TASKS) {
            return false;
        }
        
        taskQueue.add(task);
        
        // If no active task, start this one immediately
        if (activeTask == null) {
            processTaskQueue(MinecraftClient.getInstance());
        }
        
        return true;
    }
    
    /**
     * Cancel all current and queued tasks
     * @param reason The reason for cancellation
     */
    public static void cancelAllTasks(String reason) {
        if (activeTask != null) {
            activeTask.cancel();
            INSTANCE.sendInfoMessage(PREFIX, "Cancelled active task: " + reason);
            activeTask = null;
        }
        
        if (!taskQueue.isEmpty()) {
            taskQueue.clear();
            INSTANCE.sendInfoMessage(PREFIX, "Cleared task queue: " + reason);
        }
        
        // Reset pause state
        isPaused = false;
        
        // Aggressively cancel any Baritone processes
        try {
            if (BaritoneHelper.isBaritoneAvailable()) {
                BaritoneHelper.getBaritoneInstance().getPathingBehavior().forceCancel();
                BaritoneHelper.getBaritoneInstance().getPathingBehavior().cancelEverything();
                BaritoneHelper.getBaritoneInstance().getMineProcess().cancel();
                BaritoneHelper.getBaritoneInstance().getFollowProcess().cancel();
            }
        } catch (Exception e) {
            // Ignore exceptions here
        }
    }
    
    /**
     * Pause all current tasks
     */
    public static void pauseAllTasks() {
        if (activeTask != null && activeTask.isActive()) {
            // Use the task's own pause method to track progress
            activeTask.pause();
            isPaused = true;
            INSTANCE.sendInfoMessage(PREFIX, "Paused Mining Task");
            sendQueueStatus();
        }
    }
    
    /**
     * Resume all paused tasks
     */
    public static void resumeAllTasks() {
        if (isPaused) {
            isPaused = false;
            
            // Resume the active task if any
            if (activeTask != null && !activeTask.isCompleted() && !activeTask.isCancelled()) {
                int progress = activeTask.getCurrentProgress();
                int target = activeTask.getTargetAmount();
                String blockName = activeTask.getBlockName();
                int remaining = target - progress;
                
                INSTANCE.sendInfoMessage(PREFIX, "Resuming mining " + blockName + " (collected: " + progress + "/" + target + ", remaining: " + remaining + ")");
                sendQueueStatus();
                
                // Use the new resume method instead of restart
                activeTask.resume();
            }
        } else {
            // If not paused, try to start processing queue
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                processTaskQueue(client);
            }
        }
    }
    
    /**
     * Force resume - for continue command when tasks might be stuck
     */
    public static void forceResume() {
        isPaused = false;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // If there's an active task (including paused ones), resume it
        if (activeTask != null && !activeTask.isCompleted() && !activeTask.isCancelled()) {
            int progress = activeTask.getCurrentProgress();
            int target = activeTask.getTargetAmount();
            String blockName = activeTask.getBlockName();
            int remaining = target - progress;
            
            INSTANCE.sendInfoMessage(PREFIX, "Resuming mining " + blockName + " (collected: " + progress + "/" + target + ", remaining: " + remaining + ")");
            sendQueueStatus();
            activeTask.resume();
            return;
        }
        
        // If no active task but there's a queue, process it
        if (activeTask == null && !taskQueue.isEmpty()) {
            INSTANCE.sendInfoMessage(PREFIX, "Starting next queued task");
            sendQueueStatus();
            processTaskQueue(client);
            return;
        }
        
        INSTANCE.sendInfoMessage(PREFIX, "No tasks to resume");
    }
    
    /**
     * Process the next task in the queue if no task is active
     */
    public static void processTaskQueue(MinecraftClient client) {
        // Don't process tasks if paused
        if (isPaused) return;
        
        // Check if active task is complete
        if (activeTask != null) {
            if (activeTask.isCompleted()) {
                activeTask = null;
            } else if (activeTask.isCancelled()) {
                activeTask = null;
            } else if (activeTask.isPaused()) {
                // Task is paused, don't start a new one
                return;
            } else if (activeTask.isActive()) {
                // Task still running
                return;
            }
        }
        
        // Start next task if queue is not empty
        if (!taskQueue.isEmpty() && client.player != null) {
            activeTask = taskQueue.poll();
            activeTask.start();
        }
    }
    
    /**
     * @return The number of tasks currently in the queue (including active task)
     */
    public static int getQueuedTaskCount() {
        return taskQueue.size() + (activeTask != null ? 1 : 0);
    }
    
    /**
     * @return true if tasks are currently paused
     */
    public static boolean isPaused() {
        return isPaused;
    }
    
    /**
     * @return true if there is an active task (including paused ones)
     */
    public static boolean hasActiveTask() {
        return activeTask != null && (activeTask.isActive() || activeTask.isPaused());
    }
    
    /**
     * @return the current active task (may be paused)
     */
    public static MiningTask getActiveTask() {
        return activeTask;
    }
    
    /**
     * Generate a formatted queue status string showing progress for all tasks
     * @return formatted string like "[1/6] oak_log, [0/4] acacia_log"
     */
    private static String getQueueStatusString() {
        StringBuilder status = new StringBuilder();
        
        // Add active task first (if any)
        if (activeTask != null) {
            int progress = activeTask.getCurrentProgress();
            int target = activeTask.getTargetAmount();
            String blockName = activeTask.getBlockName();
            status.append("[").append(progress).append("/").append(target).append("] ").append(blockName);
        }
        
        // Add queued tasks
        for (MiningTask task : taskQueue) {
            if (status.length() > 0) {
                status.append(", ");
            }
            status.append("[0/").append(task.getTargetAmount()).append("] ").append(task.getBlockName());
        }
        
        return status.toString();
    }
    
    /**
     * Send queue status message to user
     */
    private static void sendQueueStatus() {
        String queueStatus = getQueueStatusString();
        if (!queueStatus.isEmpty()) {
            INSTANCE.sendInfoMessage(PREFIX, "Queue Status: " + queueStatus);
        }
    }
}
