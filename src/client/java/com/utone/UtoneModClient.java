package com.utone;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.utone.util.CommandInterceptor;
import com.utone.command.TestCommand;
import com.utone.command.GetCommand;
import com.utone.command.ContinueCommand;
import com.utone.util.BaritoneHelper;

public class UtoneModClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("utone-client");
	@Override
	public void onInitializeClient() {
		// Log a message to indicate the mod is running
		LOGGER.info("Utone client mod loaded!");
		
		// Check Baritone availability on first load
		BaritoneHelper.checkBaritoneAvailability();
		
		// Register command interceptor first (for global pause/stop handling)
		CommandInterceptor.register();
		
		// Register chat event listener and commands
		TestCommand.register();
		GetCommand.register();
		ContinueCommand.register();
	}
}
