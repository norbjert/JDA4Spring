# JDA4Spring

A Spring Boot integration library for [JDA](https://github.com/discord-jda/JDA) (Java Discord API). Define Discord bot event handlers as annotated Spring components — no boilerplate listener setup required.

## Requirements

- Java 17+
- Spring Boot 3.x or 4.x
- A Discord bot token ([Discord Developer Portal](https://discord.com/developers/applications))

## Installation

Add the dependency to your `build.gradle`:

```groovy
implementation 'xyz.norbjert:jda4spring:0.0.9'
```

Or in `pom.xml`:

```xml
<dependency>
    <groupId>xyz.norbjert</groupId>
    <artifactId>jda4spring</artifactId>
    <version>0.0.9</version>
</dependency>
```

## Configuration

All bot properties follow the same format regardless of where you put them. The bot name (e.g. `MyBot`) is arbitrary and just links entries together.

```properties
bots.MyBot.token=your-bot-token-here
bots.MyBot.tasks=MyBotTask

# Optional: visible activity status. Supported suffixes: .playing, .listening, .watching, .competing
bots.MyBot.activity.playing=some activity text

# Required gateway intents for your use case
# See: https://jda.wiki/using-jda/gateway-intents-and-member-cache-policy/
bots.MyBot.intents=GUILD_MESSAGES, DIRECT_MESSAGES, MESSAGE_CONTENT
```

**Multiple bots** are supported by adding additional `bots.<name>.*` blocks with different names.

### Where to put the config

**`application.properties` or `application.yml`** — the standard Spring Boot config file. Works out of the box.

**`jda4spring.properties`, `jda4spring.yml`, or `jda4spring.yaml`** — drop one of these in `src/main/resources/` and JDA4Spring picks it up automatically. Useful for keeping bot config separate from your main application config.

**External file** (recommended for keeping tokens out of source control) — point to any `.properties` or `.yml`/`.yaml` file using `jda4spring.configfile` in `application.properties`:

```properties
# Filesystem path (relative to working directory, or absolute)
jda4spring.configfile=secrets/bots.properties

# Or a classpath resource
jda4spring.configfile=classpath:bots.yml
```

External file in `.properties` format:
```properties
bots.MyBot.token=your-bot-token-here
bots.MyBot.tasks=MyBotTask
```

External file in `.yml` format:
```yaml
bots:
  MyBot:
    token: ${DISCORD_BOT_TOKEN}   # resolved from environment variable
    tasks: MyBotTask
    activity:
      playing: some activity text
    intents: GUILD_MESSAGES, DIRECT_MESSAGES, MESSAGE_CONTENT
```

Add the file to `.gitignore` to avoid accidentally committing credentials.

## Usage

### Defining a bot task

Annotate a Spring component with `@BotTask` using the same task name you listed in `bots.<name>.tasks`:

```java
@BotTask("MyBotTask")
public class MyBot {
    // event handler methods go here
}
```

### Slash commands — `@SlashCommand`

```java
@SlashCommand(command = "ping", description = "Measure bot latency")
public void ping(SlashCommandInteractionEvent event) {
    long start = System.currentTimeMillis();
    event.reply("Pong!")
         .setEphemeral(true)
         .flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - start))
         .queue();
}
```

**With arguments** — use `@SlashCommandArg` inside the `options` array. Argument values are passed as a `List<String>` in declaration order:

```java
@SlashCommand(
    command = "greet",
    description = "Greet a user",
    options = {
        @SlashCommandArg(name = "username", description = "Name to greet"),
        @SlashCommandArg(name = "greeting", description = "Custom greeting text", optionType = OptionType.STRING)
    }
)
public void greet(SlashCommandInteractionEvent event, List<String> args) {
    event.reply(args.get(1) + ", " + args.get(0) + "!").queue();
}
```

**Notes:**
- The first parameter must always be `SlashCommandInteractionEvent`.
- Command names must be lowercase.
- Omitting `command` defaults to the method name.

### Chat message events — `@OnChatMessage`

Called whenever a chat message is received. All filters are optional and combine with logical AND.

```java
// Respond to any message containing "hello" (case-insensitive by default)
@OnChatMessage(ifMsgContains = "hello")
public void onHello(MessageReceivedEvent event) {
    event.getChannel().sendMessage("Hi there!").queue();
}

// Respond to all messages in a specific channel, ignoring bots
@OnChatMessage(inChannelViaChannelId = "123456789012345678", ignoreBots = true)
public void onChannelMessage(MessageReceivedEvent event) {
    System.out.println(event.getAuthor().getName() + ": " + event.getMessage().getContentRaw());
}
```

| Attribute | Description | Default |
|---|---|---|
| `ifMsgContains` | Only trigger if the message contains this substring | `""` (no filter) |
| `onServerViaServerName` | Only trigger for messages from a server with this exact name | `""` (no filter) |
| `onServerViaServerId` | Only trigger for messages from a server with this ID | `""` (no filter) |
| `inChannelViaChannelName` | Only trigger for messages in a channel with this exact name | `""` (no filter) |
| `inChannelViaChannelId` | Only trigger for messages in a channel with this ID | `""` (no filter) |
| `ignoreBots` | Skip messages from bot accounts | `false` |
| `ignoreCase` | Case-insensitive string comparisons (does not affect ID filters) | `true` |

### Button interactions — `@Button` and `@ButtonHandler`

Use `@Button` to handle clicks on a specific button ID, or `@ButtonHandler` to handle all button interactions:

```java
// Handle a specific button by ID
@Button("confirm-action")
public void onConfirm(ButtonInteractionEvent event) {
    event.reply("Confirmed!").setEphemeral(true).queue();
}

// Handle all button interactions
@ButtonHandler
public void onAnyButton(ButtonInteractionEvent event) {
    System.out.println("Button clicked: " + event.getComponentId());
}
```

### Full example

```java
@BotTask("MyBotTask")
public class MyBot {

    @SlashCommand(command = "ping", description = "Calculate bot latency")
    public void ping(SlashCommandInteractionEvent event) {
        long start = System.currentTimeMillis();
        event.reply("Pong!")
             .setEphemeral(true)
             .flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - start))
             .queue();
    }

    @OnChatMessage(ifMsgContains = "hello", ignoreBots = true)
    public void onHello(MessageReceivedEvent event) {
        event.getChannel().sendMessage("Hi there!").queue();
    }

    @Button("some-button-id")
    public void onButtonClick(ButtonInteractionEvent event) {
        event.reply("Button clicked!").setEphemeral(true).queue();
    }
}
```

## Running tests

```bash
# Unit tests
./gradlew test

# Integration tests (requires a configured bot token)
./gradlew integrationTest

# End-to-end tests (requires live Discord bot accounts on a test server)
./gradlew e2eTest
```

## Forcing a specific JDA version

If JDA releases a new version before JDA4Spring is updated, you can override the bundled version:

```groovy
implementation 'net.dv8tion:JDA:6.x.x'
```

Note that breaking changes in JDA may require corresponding updates in JDA4Spring.

## License

[Apache-2.0](LICENSE)

## Support

Questions, issues, or suggestions? Open an issue on [GitHub](https://github.com/norbjert/JDA4Spring/issues) or join the [Discord server](https://discord.gg/dJeKP7Nyup).
