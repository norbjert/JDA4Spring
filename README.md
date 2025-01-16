# JDA4Spring

an integration of the [JDA discord API](https://github.com/discord-jda/JDA) for spring boot, with various quality of life improvements.


Both this project and this readme are still actively being worked on, but here's a simple getting started for now:

## Getting started:

Step 0: create a spring boot project if you haven't already

Step 1: add the following line to your build.gradle to import the library:

```
implementation 'xyz.norbjert:jda4spring:0.0.4'
```

Step 2: add the configuration with your discord bot api key, etc... either in the application.properties or (perferably)
in a separate file, and set the config file location in the application.properties, for example:
jda4spring.configFileLocation = src/main/resources/jda4spring.config
for the contents of that config file, look into the example I provided in this repository

Step 3: add a new class with the @BotTask("someUniqueName") annotation. Make sure "someUniqueName" matches
with the Tasks you have specified in your jda4spring.config file

Step 4: create a method with @OnChatMessage if you want it to respond to or process chat messages, or @SlashCommand
if you want to add slash commands to your bot. Here's a little example:


```
@BotTask("ExampleBot")
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
```


Step 5: That's it! That's all you need, enjoy your new discord bot!


If you need help with getting things set up right or have any questions or suggestions, feel free to join my discord:
https://discord.gg/dJeKP7Nyup









if JDA itself updates to a new version and you really want to use the new feature(s) without waiting for me to update the dependencies you can try adding the newest version as a dependency yourself. Keep in mind this might break JDA4Spring if the newest JDA update has some breaking changes, which tends to happen sometimes. Here's an example on how you can instead use a different version of JDA:

`implementation 'net.dv8tion:JDA:5.0.0-beta.${version}'`
(replace ${version} with whatever version you want)




(anything below this are personal notes regarding this project)




Notes for documentation:
-Button Events:
-@Button("id") requires ID, to be defined when defining a button in a message
-if you want to dynamically add and change buttons at runtime use a custom @ButtonManager and write the implementation yourself

@Button("someID") works similar to @OnChatMessage("some msg"), while @ButtonManager represents the equivalent to @OnChatMessage without filter


Notes and future todos for me:
-maybe make a specific @Scheduled for regularly occurring bot tasks?
-Implement ButtonEvents
-Maybe some fancy annotation-based way for slash command auto-completion? https://jda.wiki/using-jda/interactions/#slash-command-autocomplete
-Clean up the DiscordBot.java class, that thing is a mess and needs a smarter implementation (with less copy pasted code)
