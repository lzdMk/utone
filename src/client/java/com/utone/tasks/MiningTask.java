package com.utone.tasks;

import baritone.api.IBaritone;
import baritone.api.process.IMineProcess;
import com.utone.util.BaritoneHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * A task for mining a specified amount of a specific block
 */
public class MiningTask implements MessageHelper {
    private static final String PREFIX = "Utone";
    private final String blockName;
    private final int targetAmount;
    private int initialCount;
    private int collectedSoFar = 0; // Track how much we've actually collected
    private boolean isActive = false;
    private boolean isCompleted = false;
    private boolean isCancelled = false;
    private boolean isPaused = false;
    private int ticksSinceLastCheck = 0;
    private final int CHECK_INTERVAL = 5; // Check inventory every 5 ticks
    
    private Block targetBlock;
    
    public MiningTask(String blockName, int targetAmount) {
        this.blockName = blockName;
        this.targetAmount = targetAmount;
    }
    
    /**
     * Start the mining task
     */
    public void start() {
        if (isActive) return;
        
        try {
            // Check if Baritone is available
            if (!BaritoneHelper.isBaritoneAvailable()) {
                sendErrorMessage(PREFIX, "Baritone is not available. Cannot start mining task.");
                isCancelled = true;
                return;
            }
            
            // Get the Baritone instance
            IBaritone baritone = BaritoneHelper.getBaritoneInstance();
            if (baritone == null) {
                sendErrorMessage(PREFIX, "Failed to get Baritone instance.");
                isCancelled = true;
                return;
            }
            
            IMineProcess mineProcess = baritone.getMineProcess();
            
            // Resolve the block
            targetBlock = resolveBlock(blockName);
            if (targetBlock == null) {
                sendErrorMessage(PREFIX, "Unknown block: " + blockName);
                isCancelled = true;
                return;
            }
            
            // Count the current number of matching items in inventory
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                initialCount = countItemsInInventory(client, targetBlock);
            }
            
            // Start mining
            mineProcess.mine(targetBlock);
            isActive = true;
            
            // Register a tick event for checking progress
            ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
            
            // Inform the user that the process has started
            sendSuccessMessage(PREFIX, "Now mining " + blockName + " (requested: " + targetAmount + ")");
        } catch (Exception e) {
            sendErrorMessage(PREFIX, "Failed to start mining process: " + e.getMessage());
            isCancelled = true;
        }
    }
    
    /**
     * Cancel the mining task
     */
    public void cancel() {
        if (isActive) {
            try {
                // Get the Baritone instance and cancel the current process
                IBaritone baritone = BaritoneHelper.getBaritoneInstance();
                if (baritone != null) {
                    baritone.getPathingBehavior().cancelEverything();
                }
            } catch (Exception e) {
                sendErrorMessage(PREFIX, "Failed to stop mining: " + e.getMessage());
            }
        }
        
        isActive = false;
        isCancelled = true;
    }
    
    /**
     * Pause the mining task and track current progress
     */
    public void pause() {
        if (isActive) {
            // Track current inventory count when pausing
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && targetBlock != null) {
                int currentCount = countItemsInInventory(client, targetBlock);
                collectedSoFar = currentCount - initialCount;
            }
            
            // Stop Baritone processes
            try {
                IBaritone baritone = BaritoneHelper.getBaritoneInstance();
                if (baritone != null) {
                    baritone.getPathingBehavior().cancelEverything();
                }
            } catch (Exception e) {
                // Ignore errors when pausing
            }
            
            isActive = false;
            isPaused = true;
            // TaskManager will handle the pause message
        }
    }
    
    /**
     * Restart the mining task (used when resuming from pause)
     */
    public void restart() {
        if (isCancelled || isCompleted) {
            return;
        }
        try {
            // Check if Baritone is available
            if (!BaritoneHelper.isBaritoneAvailable()) {
                sendErrorMessage(PREFIX, "Baritone is not available. Cannot restart mining task.");
                isCancelled = true;
                return;
            }
            // Get the Baritone instance
            IBaritone baritone = BaritoneHelper.getBaritoneInstance();
            if (baritone == null) {
                sendErrorMessage(PREFIX, "Failed to get Baritone instance.");
                isCancelled = true;
                return;
            }
            // Aggressively cancel any existing processes to ensure clean state
            baritone.getPathingBehavior().forceCancel();
            baritone.getPathingBehavior().cancelEverything();
            IMineProcess mineProcess = baritone.getMineProcess();
            // Restart mining
            mineProcess.mine(targetBlock);
            isActive = true;
            try {
                ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
            } catch (Exception e) {
                // Ignore duplicate registration
            }
            sendSuccessMessage(PREFIX, "Resumed mining " + blockName);
        } catch (Exception e) {
            sendErrorMessage(PREFIX, "Failed to restart mining process: " + e.getMessage());
            isCancelled = true;
        }
    }
    
    /**
     * Resume the mining task (custom implementation that continues from where we left off)
     */
    public void resume() {
        if (isCancelled || isCompleted) {
            return;
        }
        
        try {
            // Check if Baritone is available
            if (!BaritoneHelper.isBaritoneAvailable()) {
                sendErrorMessage(PREFIX, "Baritone is not available. Cannot resume mining task.");
                isCancelled = true;
                return;
            }
            
            // Get the Baritone instance
            IBaritone baritone = BaritoneHelper.getBaritoneInstance();
            if (baritone == null) {
                sendErrorMessage(PREFIX, "Failed to get Baritone instance.");
                isCancelled = true;
                return;
            }
            
            IMineProcess mineProcess = baritone.getMineProcess();
            
            // Check current progress - use our tracked progress if paused
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && targetBlock != null) {
                int collected = collectedSoFar; // Use the tracked progress
                int remaining = targetAmount - collected;
                
                if (remaining <= 0) {
                    // Task is already complete
                    sendSuccessMessage(PREFIX, "Mining task already completed! Collected " + collected + " " + blockName);
                    complete();
                    return;
                }
                
                // Start mining for the remaining amount
                mineProcess.mine(targetBlock);
                isActive = true;
                isPaused = false;
                
                // TaskManager handles the resume message, so we don't need to send one here
                
                // Ensure tick listener is registered
                try {
                    ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
                } catch (Exception e) {
                    // Ignore duplicate registration
                }
            }
            
        } catch (Exception e) {
            sendErrorMessage(PREFIX, "Failed to resume mining process: " + e.getMessage());
            isCancelled = true;
        }
    }
    
    /**
     * Handle tick events to check progress
     */
    private void onClientTick(MinecraftClient client) {
        if (!isActive || isCompleted || isCancelled) return;
        if (client.player == null) return;
        // Only check every few ticks to improve performance
        ticksSinceLastCheck++;
        if (ticksSinceLastCheck < CHECK_INTERVAL) return;
        ticksSinceLastCheck = 0;
        // Count current items
        int newCount = countItemsInInventory(client, targetBlock);
        // Check if we've collected enough
        int collected = newCount - initialCount;
        collectedSoFar = collected; // Update tracked progress
        if (collected >= targetAmount) {
            sendSuccessMessage(PREFIX, "Collected " + collected + " " + blockName + " (goal: " + targetAmount + "). Mining complete!");
            complete();
        }
    }
    
    /**
     * Mark the task as completed
     */
    private void complete() {
        try {
            // Get the Baritone instance and cancel the current process
            IBaritone baritone = BaritoneHelper.getBaritoneInstance();
            if (baritone != null) {
                baritone.getPathingBehavior().cancelEverything();
            }
        } catch (Exception e) {
            // Ignore any errors when cancelling
        }
        
        isActive = false;
        isCompleted = true;
    }
    
    /**
     * Count the number of a specific block/item in the player's inventory
     */
    private int countItemsInInventory(MinecraftClient client, Block block) {
        int count = 0;
        if (client.player == null) return 0;
        
        // Check all inventory slots (0-35 for main inventory)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            
            // Check if the item matches the block we're looking for
            if (stack.getItem().equals(block.asItem())) {
                count += stack.getCount();
            }
        }
        
        return count;
    }
    
    /**
     * Resolve block name to actual Block
     */
    private Block resolveBlock(String blockName) {
        // Split namespace and path if the block name includes a colon
        String namespace, path;
        if (blockName.contains(":")) {
            String[] parts = blockName.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        } else {
            namespace = "minecraft";
            path = blockName;
        }
        
        Identifier blockId = Identifier.of(namespace, path);
        Block block = Registries.BLOCK.get(blockId);
        
        // Check if the block exists
        if (block == Registries.BLOCK.get(Registries.BLOCK.getDefaultId())) {
            return null;
        }
        
        return block;
    }
    
    /**
     * @return true if the task is completed
     */
    public boolean isCompleted() {
        return isCompleted;
    }
    
    /**
     * @return true if the task was cancelled
     */
    public boolean isCancelled() {
        return isCancelled;
    }
    
    /**
     * @return true if the task is currently active
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * @return true if the task is currently paused
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * @return the name of the block being mined
     */
    public String getBlockName() {
        return blockName;
    }
    
    /**
     * @return the target amount of blocks to mine
     */
    public int getTargetAmount() {
        return targetAmount;
    }
    
    /**
     * @return the current progress (collected amount)
     */
    public int getCurrentProgress() {
        if (isPaused) {
            // If paused, return the tracked progress
            return collectedSoFar;
        }
        
        // If active, calculate current progress
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && targetBlock != null) {
            int currentCount = countItemsInInventory(client, targetBlock);
            return currentCount - initialCount;
        }
        return collectedSoFar;
    }
    
    // MessageHelper methods are now used directly
}
