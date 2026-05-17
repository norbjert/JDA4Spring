package xyz.norbjert.jda4spring.integration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.*;
import xyz.norbjert.jda4spring.annotations.BotTask;
import xyz.norbjert.jda4spring.annotations.Button;
import xyz.norbjert.jda4spring.annotations.ButtonHandler;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;
import xyz.norbjert.jda4spring.annotations.SlashCommand;
import xyz.norbjert.jda4spring.annotations.SlashCommandArg;
import xyz.norbjert.jda4spring.internal.DiscordBot;

import javax.security.auth.login.LoginException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/**
 * Integration tests that run against a real Discord bot account.
 * Requires DISCORD_TEST_BOT_TOKEN environment variable to be set.
 * Run via: ./gradlew integrationTest
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DiscordBotIntegrationTest {

    static DiscordBot bot;
    static TestBotTask task;

    @BotTask("IntegrationTestTask")
    static class TestBotTask {
        boolean pingCalled;
        SlashCommandInteractionEvent lastSlashEvent;
        boolean helloCalled;
        String lastEchoMessage;
        boolean ignoreBotsFired;
        boolean buttonCalled;
        boolean buttonHandlerCalled;
        boolean nonMatchingFired;

        @SlashCommand(command = "ping", description = "Integration test ping command")
        public void ping(SlashCommandInteractionEvent event) {
            pingCalled = true;
            lastSlashEvent = event;
        }

        @SlashCommand(
                command = "echo",
                description = "Integration test echo command",
                options = @SlashCommandArg(name = "message", description = "Message to echo", optionType = OptionType.STRING)
        )
        public void echo(SlashCommandInteractionEvent event, String message) {
            lastEchoMessage = message;
        }

        @OnChatMessage(ifMsgContains = "hello")
        public void onHello(MessageReceivedEvent event) {
            helloCalled = true;
        }

        @OnChatMessage(ifMsgContains = "ignoretest", ignoreBots = true)
        public void onNonBotOnly(MessageReceivedEvent event) {
            ignoreBotsFired = true;
        }

        @OnChatMessage(ifMsgContains = "xq9_will_never_appear")
        public void onNonMatchingContent(MessageReceivedEvent event) {
            nonMatchingFired = true;
        }

        @Button("test-button")
        public void onTestButton(ButtonInteractionEvent event) {
            buttonCalled = true;
        }

        @ButtonHandler
        public void onAnyButton(ButtonInteractionEvent event) {
            buttonHandlerCalled = true;
        }
    }

    @BeforeAll
    static void setUp() throws LoginException, InterruptedException {
        String token = System.getenv("DISCORD_TEST_BOT_TOKEN");
        assumeTrue(token != null && !token.isBlank(), "DISCORD_TEST_BOT_TOKEN not set — skipping integration tests");

        task = new TestBotTask();
        bot = new DiscordBot(
                token,
                List.of(task),
                null,
                List.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
        );
    }

    @AfterAll
    static void tearDown() {
        if (bot != null) {
            bot.getJda().shutdown();
        }
    }

    @Test
    @Order(1)
    void botConnectsSuccessfully() {
        assertNotNull(bot.getJda());
        assertEquals(JDA.Status.CONNECTED, bot.getJda().getStatus());
    }

    @Test
    @Order(2)
    void slashCommandsRegisteredWithDiscord() throws InterruptedException {
        Thread.sleep(3000);
        var commands = bot.getJda().retrieveCommands().complete();
        assertAll(
                () -> assertTrue(commands.stream().anyMatch(c -> c.getName().equals("ping")),
                        "Expected 'ping' slash command to be registered"),
                () -> assertTrue(commands.stream().anyMatch(c -> c.getName().equals("echo")),
                        "Expected 'echo' slash command to be registered")
        );
    }

    @Test
    @Order(3)
    void slashCommandRoutingDispatchesCorrectMethod() {
        SlashCommandInteractionEvent mockEvent = mock(SlashCommandInteractionEvent.class);
        MessageChannelUnion mockChannel = mock(MessageChannelUnion.class);
        User mockUser = mock(User.class);

        when(mockEvent.getName()).thenReturn("ping");
        when(mockEvent.getOptions()).thenReturn(List.of());
        when(mockEvent.getGuild()).thenReturn(null);
        when(mockEvent.getChannel()).thenReturn(mockChannel);
        when(mockChannel.getName()).thenReturn("test-channel");
        when(mockEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getName()).thenReturn("test-user");

        bot.onSlashCommandInteraction(mockEvent);

        assertTrue(task.pingCalled, "Expected ping() to be called when slash command 'ping' is dispatched");
        assertSame(mockEvent, task.lastSlashEvent);
    }

    @Test
    @Order(4)
    void echoCommandWithArgumentDispatchesCorrectly() {
        SlashCommandInteractionEvent mockEvent = mock(SlashCommandInteractionEvent.class);
        MessageChannelUnion mockChannel = mock(MessageChannelUnion.class);
        User mockUser = mock(User.class);
        OptionMapping mockOption = mock(OptionMapping.class);

        when(mockEvent.getName()).thenReturn("echo");
        when(mockEvent.getGuild()).thenReturn(null);
        when(mockEvent.getChannel()).thenReturn(mockChannel);
        when(mockChannel.getName()).thenReturn("test-channel");
        when(mockEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getName()).thenReturn("test-user");
        when(mockEvent.getOptions()).thenReturn(List.of(mockOption));
        when(mockEvent.getOption("message")).thenReturn(mockOption);
        when(mockOption.getType()).thenReturn(OptionType.STRING);
        when(mockOption.getAsString()).thenReturn("hello world");

        bot.onSlashCommandInteraction(mockEvent);

        assertEquals("hello world", task.lastEchoMessage,
                "Expected echo() to receive the argument value injected by name");
    }

    @Test
    @Order(5)
    void chatMessageRoutingDispatchesCorrectMethod() {
        MessageReceivedEvent mockEvent = mock(MessageReceivedEvent.class);
        net.dv8tion.jda.api.entities.Message mockMessage = mock(net.dv8tion.jda.api.entities.Message.class);
        User mockAuthor = mock(User.class);

        when(mockEvent.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getContentRaw()).thenReturn("say hello there");
        when(mockEvent.isFromGuild()).thenReturn(false);
        when(mockEvent.getAuthor()).thenReturn(mockAuthor);
        when(mockAuthor.isBot()).thenReturn(false);

        bot.onMessageReceived(mockEvent);

        assertTrue(task.helloCalled, "Expected onHello() to be called when message contains 'hello'");
    }

    @Test
    @Order(6)
    void chatMessageFilterDoesNotFireOnNonMatchingContent() {
        MessageReceivedEvent mockEvent = mock(MessageReceivedEvent.class);
        net.dv8tion.jda.api.entities.Message mockMessage = mock(net.dv8tion.jda.api.entities.Message.class);
        User mockAuthor = mock(User.class);

        when(mockEvent.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getContentRaw()).thenReturn("a plain message without any filter keywords");
        when(mockEvent.isFromGuild()).thenReturn(false);
        when(mockEvent.getAuthor()).thenReturn(mockAuthor);
        when(mockAuthor.isBot()).thenReturn(false);

        bot.onMessageReceived(mockEvent);

        assertFalse(task.nonMatchingFired,
                "Handler with ifMsgContains filter should not fire when content does not match");
    }

    @Test
    @Order(7)
    void ignoreBotsFlagPreventsHandlerFiringForBotMessages() {
        MessageReceivedEvent mockEvent = mock(MessageReceivedEvent.class);
        net.dv8tion.jda.api.entities.Message mockMessage = mock(net.dv8tion.jda.api.entities.Message.class);
        User mockAuthor = mock(User.class);

        when(mockEvent.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getContentRaw()).thenReturn("ignoretest message from a bot");
        when(mockEvent.isFromGuild()).thenReturn(false);
        when(mockEvent.getAuthor()).thenReturn(mockAuthor);
        when(mockAuthor.isBot()).thenReturn(true);

        bot.onMessageReceived(mockEvent);
        assertFalse(task.ignoreBotsFired,
                "Handler with ignoreBots=true should not fire for bot-authored messages");

        when(mockAuthor.isBot()).thenReturn(false);
        bot.onMessageReceived(mockEvent);
        assertTrue(task.ignoreBotsFired,
                "Handler with ignoreBots=true should fire for non-bot messages");
    }

    @Test
    @Order(8)
    void buttonRoutingDispatchesToSpecificAndCatchAllHandler() {
        ButtonInteractionEvent mockEvent = mock(ButtonInteractionEvent.class);
        when(mockEvent.getComponentId()).thenReturn("test-button");

        bot.onButtonInteraction(mockEvent);

        assertTrue(task.buttonCalled,
                "@Button(\"test-button\") should fire for matching component ID");
        assertTrue(task.buttonHandlerCalled,
                "@ButtonHandler catch-all should also fire for any button interaction");
    }

    @Test
    @Order(9)
    void buttonHandlerCatchAllFiresForUnmatchedButton() {
        task.buttonCalled = false;
        task.buttonHandlerCalled = false;

        ButtonInteractionEvent mockEvent = mock(ButtonInteractionEvent.class);
        when(mockEvent.getComponentId()).thenReturn("some-other-button");

        bot.onButtonInteraction(mockEvent);

        assertFalse(task.buttonCalled,
                "@Button(\"test-button\") should not fire for a different component ID");
        assertTrue(task.buttonHandlerCalled,
                "@ButtonHandler catch-all should fire for any button interaction");
    }
}
