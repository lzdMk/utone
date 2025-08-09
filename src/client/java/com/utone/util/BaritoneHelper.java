package com.utone.util;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import com.utone.tasks.MessageHelper;

/**
 * Utility class to handle Baritone-related operations and checks
 */
public class BaritoneHelper implements MessageHelper {
    private static final String PREFIX = "Utone";
    private static boolean baritoneAvailable = false;
    private static boolean hasCheckedBaritone = false;
    private static boolean hasNotifiedUser = false;
    
    /**
     * Check if Baritone is available and notify user on first load if not
     * @return true if Baritone is available, false otherwise
     */
    public static boolean checkBaritoneAvailability() {
        if (hasCheckedBaritone) {
            return baritoneAvailable;
        }
        
        try {
            // Try to get Baritone instance
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (baritone != null) {
                baritoneAvailable = true;
            }
        } catch (Exception | NoClassDefFoundError e) {
            baritoneAvailable = false;
        }
        
        hasCheckedBaritone = true;
        
        // Notify user on first check if Baritone is not available
        if (!baritoneAvailable && !hasNotifiedUser) {
            notifyBaritoneNotAvailable();
            hasNotifiedUser = true;
        }
        
        return baritoneAvailable;
    }
    
    /**
     * Notify the user that Baritone is not available
     */
    private static void notifyBaritoneNotAvailable() {
        BaritoneHelper helper = new BaritoneHelper();
        helper.sendErrorMessage(PREFIX, "Baritone is not installed or not available!");
        helper.sendInfoMessage(PREFIX, "This mod requires Baritone to function. Please install Baritone and restart the game.");
        helper.sendInfoMessage(PREFIX, "Commands will be ignored until Baritone is available.");
    }
    
    /**
     * Get Baritone instance safely
     * @return Baritone instance or null if not available
     */
    public static IBaritone getBaritoneInstance() {
        if (!checkBaritoneAvailability()) {
            return null;
        }
        
        try {
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if a message is a Baritone stop command
     * @param message The message to check
     * @return true if it's a Baritone stop command
     */
    public static boolean isBaritoneStopCommand(String message) {
        if (message == null) return false;
        
        String trimmed = message.trim().toLowerCase();
        
        // Common Baritone stop commands
        return trimmed.equals("#stop") || 
               trimmed.equals("#cancel") || 
               trimmed.equals("##stop") ||
               trimmed.equals("##cancel");
    }
    
    /**
     * Check if a message is a Baritone pause command
     * @param message The message to check
     * @return true if it's a Baritone pause command
     */
    public static boolean isBaritonePauseCommand(String message) {
        if (message == null) return false;
        
        String trimmed = message.trim().toLowerCase();
        
        // Common Baritone pause commands
        return trimmed.equals("#pause") || 
               trimmed.equals("#p") ||
               trimmed.equals("##pause") ||
               trimmed.equals("##p");
    }
    
    /**
     * Check if a message is a Baritone resume command
     * @param message The message to check
     * @return true if it's a Baritone resume command
     */
    public static boolean isBaritoneResumeCommand(String message) {
        if (message == null) return false;
        
        String trimmed = message.trim().toLowerCase();
        
        // Common Baritone resume commands
        return trimmed.equals("#resume") || 
               trimmed.equals("#r") ||
               trimmed.equals("##resume") ||
               trimmed.equals("##r");
    }
    
    /**
     * @return true if Baritone is available
     */
    public static boolean isBaritoneAvailable() {
        return checkBaritoneAvailability();
    }
}
