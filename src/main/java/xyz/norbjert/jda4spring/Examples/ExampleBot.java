package xyz.norbjert.jda4spring.Examples;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import xyz.norbjert.jda4spring.annotations.BotTask;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;
import xyz.norbjert.jda4spring.annotations.SlashCommand;

/**
 * a simple example on how to use jda4spring
 */
//@BotTask("ExampleBot")
public class ExampleBot {

    /**
     * a slash command demo function that calculates the ping of the bot
     * @param event the event that triggered the function
     */
    @SlashCommand(command = "ping", description = "Calculate ping of the bot")
    public void ping(SlashCommandInteractionEvent event) {
        //the contents of this method are from JDA's official example
        long time = System.currentTimeMillis();
        event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                .flatMap(v ->
                        event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                ).queue(); // Queue both reply and edit
    }

    /**
     * a demo function that gets called every time someone sends a message containing "hello" in a chat that the bot can read
     * @param event the event that triggered the function
     */
    @OnChatMessage(ifMsgContains = "hello") //will only call method if the received message contained "hello"
    public void hello(MessageReceivedEvent event){
        event.getChannel().sendMessage("Hi there!").queue();
    }

    /**
     * a demo function that gets called on every message sent
     * @param event the event that triggered the function
     */
    @OnChatMessage //will be called on any chat message the bot receives
    public void onAllChatMessages(MessageReceivedEvent event){
        System.out.println(event.getAuthor().getName() + " has sent: " + event.getMessage().getContentRaw());
    }
}
