package com.utone.command;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class TestCommand {
    public static void register() {
        // Listen for chat messages and handle #test
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            String trimmed = message.trim();
            if (trimmed.equalsIgnoreCase("#test")) {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal("Test Completed").styled(style -> style.withColor(0x00FF00))
                );
                return false;
            } else if (trimmed.equalsIgnoreCase("#hello")) {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal("hi").styled(style -> style.withColor(0x00FF00))
                );
                return false;
            }
            return true;
        });
    }
}
