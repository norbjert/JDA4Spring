package xyz.norbjert.jda4spring.Examples;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import xyz.norbjert.jda4spring.annotations.BotTask;
import xyz.norbjert.jda4spring.annotations.Button;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;
import xyz.norbjert.jda4spring.annotations.SlashCommand;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;

/**
 * a simple example on how to use jda4spring
 */
@BotTask("ExampleBot")
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

    @SlashCommand(command = "test-buttons", description = "create an embed with buttons to demo their functionality")
    void testButtons(SlashCommandInteractionEvent event){

        MessageEmbed eb = new EmbedBuilder()
                .setColor(new Color(0, 200, 255))
                .addField("Button-Demo", "", false)
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(eb)
                .addComponents(
                        ActionRow.of(
                        net.dv8tion.jda.api.components.buttons.Button.primary("hello", "say hello ;)"),
                        net.dv8tion.jda.api.components.buttons.Button.danger("delete545435", "delete")))
                .queue();
    }


    @Button("hello")
    void helloButton(ButtonInteractionEvent event) {
        event.reply("Hello :D").setEphemeral(true).queue();
    }

    @Button("delete545435")
    void deleteMessageButton(ButtonInteractionEvent event) {
        event.getMessage().delete().queue();
        event.reply("Message deleted").setEphemeral(true).queue();
    }
}
