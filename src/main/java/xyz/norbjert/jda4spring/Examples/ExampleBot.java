package xyz.norbjert.jda4spring.Examples;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import xyz.norbjert.jda4spring.annotations.BotTask;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;
import xyz.norbjert.jda4spring.annotations.SlashCommand;

//@BotTask("ExampleBot")
public class ExampleBot {

    @SlashCommand(command = "ping", description = "Calculate ping of the bot")
    public void ping(SlashCommandInteractionEvent event) {
        //the contents of this method are from JDA's official example
        long time = System.currentTimeMillis();
        event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                .flatMap(v ->
                        event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                ).queue(); // Queue both reply and edit
    }

    @OnChatMessage(ifMsgContains = "hello") //will only call method if the received message contained "hello"
    public void hello(MessageReceivedEvent event){
        event.getChannel().sendMessage("Hi there!").queue();
    }

    @OnChatMessage //will be called on any chat message the bot receives
    public void onAllChatMessages(MessageReceivedEvent event){
        System.out.println(event.getAuthor().getName() + " has sent: " + event.getMessage().getContentRaw());
    }
}
