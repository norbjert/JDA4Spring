package xyz.norbjert.jda4spring.integration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.*;
import xyz.norbjert.jda4spring.annotations.BotTask;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;
import xyz.norbjert.jda4spring.annotations.SlashCommand;
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

        @SlashCommand(command = "ping", description = "Integration test ping command")
        public void ping(SlashCommandInteractionEvent event) {
            pingCalled = true;
            lastSlashEvent = event;
        }

        @OnChatMessage(ifMsgContains = "hello")
        public void onHello(MessageReceivedEvent event) {
            helloCalled = true;
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
        // Allow time for command registration to propagate to Discord's API
        Thread.sleep(3000);
        var commands = bot.getJda().retrieveCommands().complete();
        assertTrue(commands.stream().anyMatch(c -> c.getName().equals("ping")),
                "Expected 'ping' slash command to be registered with Discord");
    }

    @Test
    @Order(3)
    void slashCommandRoutingDispatchesCorrectMethod() {
        SlashCommandInteractionEvent mockEvent = mock(SlashCommandInteractionEvent.class);
        MessageChannelUnion mockChannel = mock(MessageChannelUnion.class);
        User mockUser = mock(User.class);

        when(mockEvent.getName()).thenReturn("ping");
        when(mockEvent.getOptions()).thenReturn(List.of());
        when(mockEvent.getGuild()).thenReturn(null); // simulate DM
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
}
