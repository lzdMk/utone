package com.utone;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.utone.command.TestCommand;

public class UtoneModClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("utone-client");
	@Override
	public void onInitializeClient() {
		// Log a message to indicate the mod is running
		LOGGER.info("Utone client mod loaded!");
		// Register chat event listener and test command
		TestCommand.register();
	}
}
