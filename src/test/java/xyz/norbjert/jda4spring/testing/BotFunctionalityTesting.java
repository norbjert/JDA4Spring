package xyz.norbjert.jda4spring.testing;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import xyz.norbjert.jda4spring.annotations.*;
import xyz.norbjert.jda4spring.annotations.Button;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Only actual testing class, does not do the usual unit tests that it should be doing bc im too lazy for that
 * Instead I use this to test specific functionality when I am making changes
 *  You can ignore this
 */
@BotTask("Testing")
public class BotFunctionalityTesting {

    private final Logger logger = LoggerFactory.getLogger(BotFunctionalityTesting.class);

    /**
     *  t
     * @param loggingChannelID t
     */
    @Autowired
    public BotFunctionalityTesting(@Value("${jda4spring.loggingChannelID}") String loggingChannelID) {

    }


    /**
     *
     * @param event t
     */
    @SlashCommand(command = "test")
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {


        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(0, 200, 255));
        eb.setAuthor("author", event.getJDA().getSelfUser().getAvatarUrl());
        eb.setThumbnail(event.getJDA().getSelfUser().getAvatarUrl());

        eb.addField("title", "desc", false);
        eb.addField("title2", "desc2", false);

        MessageEmbed embed = eb.build();

        event.reply("Click the button to say hello")
                .addEmbeds(embed)
                .addComponents(
                        ActionRow.of(
                        net.dv8tion.jda.api.components.buttons.Button.primary("hello", "Click Me"), // Button with only a label
                        net.dv8tion.jda.api.components.buttons.Button.success("emoji", Emoji.fromFormatted("<:minn:245267426227388416>")))) // Button with only an emoji
                .queue();

    }

    /**
     *
     * @param event t
     */
    @Button("hello")
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("hello")) {
            event.reply("Hello :)").queue(); // send a message in the channel
        } else if (event.getComponentId().equals("emoji")) {
            event.editMessage("That button didn't say click me").queue(); // update the message
        }
    }

    /**
     *
     * @param event t
     */
    @ButtonHandler
    public void buttonHandlerTesting(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("hello")){
            event.getChannel().sendMessage("Hello :)").queue(); // send a message in the channel

        } else if (event.getComponentId().equals("emoji")) {
            event.editMessage("That button didn't say click me").queue(); // update the message
        }
    }

    /**
     *
     * @param event t
     */
    @OnChatMessage
    public void onMsg(MessageReceivedEvent event){
        if(!event.getAuthor().equals(event.getJDA().getSelfUser())
            && !event.isFromGuild())
            event.getChannel().sendMessage("received msg").queue();
    }

    /**
     *
     * @param event t
     */
    @SlashCommand//(command = "console", description = "provides the console output")
    public void console(SlashCommandInteractionEvent event) {

        if (isMaster(event.getUser())) {
            try (Scanner scan = new Scanner(new File("logs/spring.log"))) {
                StringBuilder sb = new StringBuilder();
                while (scan.hasNextLine()) {
                    String nextLine = scan.nextLine();
                    sb.append(nextLine).append("\n");
                }
                String re = sb.substring(sb.length() - 2001, sb.length() - 1);
                event.reply(re).setEphemeral(true).queue();

            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
                event.reply("couldnt access console file?").setEphemeral(true).queue();
            }
        } else {
            event.reply("unauthorized").setEphemeral(true).queue();
        }
    }

    /**
     *
     * @param user t
     */
    private boolean isMaster(User user) {
        return user.getName().equals("norbjert");
    }

}
