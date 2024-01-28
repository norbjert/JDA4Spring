**JDA4Spring**

an integration of the JDA discord API (https://github.com/discord-jda/JDA) for spring boot, with various quality of life improvements.
This project is still in active development, sufficiant documentation and ussage instructions will be added soon.





Notes for documentation:
-Button Events:
-@Button("id") requires ID, to be defined when defining a button in a message
-if you want to dynamically add and change buttons at runtime use a custom @ButtonManager and write the implementation yourself

@Button("someid") works simular to @OnChatMessage("some msg"), while @ButtonManager represents the equivalent to @OnChatMessage without filter


Notes and future todos for me:
-maybe make a specific @Scheduled for regularly occuring bot tasks?
-Implement ButtonEvents
-Maybe some fancy annotation-based way for slash command auto completion? https://jda.wiki/using-jda/interactions/#slash-command-autocomplete
-Clean up the DiscordBot.java class, that thing is a mess and needs a smarter implementation (with less copy pasted code)