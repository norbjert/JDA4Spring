package xyz.norbjert.jda4spring.e2e;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import xyz.norbjert.jda4spring.annotations.BotTask;
import xyz.norbjert.jda4spring.annotations.Button;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;
import xyz.norbjert.jda4spring.annotations.SlashCommand;
import xyz.norbjert.jda4spring.annotations.SlashCommandArg;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Example JDA4Spring bot task used for end-to-end testing.
 * Demonstrates slash commands, message filtering, and button handling.
 */
@BotTask("E2EExampleBot")
public class E2EBotTask {

    final ConcurrentLinkedQueue<MessageReceivedEvent> receivedMessages = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<MessageReceivedEvent> filteredMessages = new ConcurrentLinkedQueue<>();

    @SlashCommand(command = "ping", description = "Responds with pong")
    public void ping(SlashCommandInteractionEvent event) {
        event.reply("Pong!").queue();
    }

    @SlashCommand(
            command = "echo",
            description = "Echoes a message back",
            options = @SlashCommandArg(name = "message", description = "The message to echo", optionType = OptionType.STRING)
    )
    public void echo(SlashCommandInteractionEvent event, String message) {
        event.reply(message).queue();
    }

    @OnChatMessage
    public void onAnyMessage(MessageReceivedEvent event) {
        receivedMessages.offer(event);
    }

    @OnChatMessage(ifMsgContains = "E2E_FILTER_PROBE")
    public void onFilteredMessage(MessageReceivedEvent event) {
        filteredMessages.offer(event);
    }

    @Button("e2e-test-button")
    public void onTestButton(ButtonInteractionEvent event) {
        event.reply("Button received!").queue();
    }
}
