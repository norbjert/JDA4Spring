package xyz.norbjert.jda4spring.e2e;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.*;
import xyz.norbjert.jda4spring.internal.DiscordBot;

import javax.security.auth.login.LoginException;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end tests for JDA4Spring running against real Discord bots on a test server.
 *
 * Required environment variables:
 *   DISCORD_TEST_BOT_TOKEN   - token for the JDA4Spring example bot (Bot 1)
 *   DISCORD_SENDER_BOT_TOKEN - token for the plain JDA sender bot (Bot 2)
 *   DISCORD_TEST_CHANNEL_ID  - ID of the Discord channel to use for test messages
 *
 * Run via: ./gradlew e2eTest
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DiscordBotE2ETest {

    static DiscordBot exampleBot;
    static JDA senderJda;
    static E2EBotTask botTask;
    static String testChannelId;

    @BeforeAll
    static void setUp() throws LoginException, InterruptedException {
        // Bot 1: the JDA4Spring example bot being tested — create at discord.com/developers/applications
        String botToken = System.getenv("DISCORD_TEST_BOT_TOKEN");
        // Bot 2: plain JDA sender bot that triggers Bot 1's handlers — requires a separate bot account
        String senderToken = System.getenv("DISCORD_SENDER_BOT_TOKEN");
        // Target channel for test messages — enable Developer Mode, then right-click channel → Copy Channel ID
        // Both bots need Send Messages permission in this channel
        testChannelId = System.getenv("DISCORD_TEST_CHANNEL_ID");

        assumeTrue(botToken != null && !botToken.isBlank(),
                "DISCORD_TEST_BOT_TOKEN not set — skipping E2E tests");
        assumeTrue(senderToken != null && !senderToken.isBlank(),
                "DISCORD_SENDER_BOT_TOKEN not set — skipping E2E tests");
        assumeTrue(testChannelId != null && !testChannelId.isBlank(),
                "DISCORD_TEST_CHANNEL_ID not set — skipping E2E tests");

        botTask = new E2EBotTask();
        exampleBot = new DiscordBot(
                botToken,
                List.of(botTask),
                null,
                List.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
        );

        senderJda = JDABuilder.createLight(senderToken,
                        List.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .build()
                .awaitReady();
    }

    @AfterAll
    static void tearDown() {
        if (exampleBot != null) exampleBot.getJda().shutdown();
        if (senderJda != null) senderJda.shutdown();
    }

    @Test
    @Order(1)
    void botConnectsSuccessfully() {
        assertNotNull(exampleBot.getJda());
        assertEquals(JDA.Status.CONNECTED, exampleBot.getJda().getStatus());
    }

    @Test
    @Order(2)
    void slashCommandsRegisteredWithDiscord() throws InterruptedException {
        Thread.sleep(3000);
        var commands = exampleBot.getJda().retrieveCommands().complete();
        assertAll(
                () -> assertTrue(commands.stream().anyMatch(c -> c.getName().equals("ping")),
                        "Expected 'ping' slash command to be registered with Discord"),
                () -> assertTrue(commands.stream().anyMatch(c -> c.getName().equals("echo")),
                        "Expected 'echo' slash command to be registered with Discord")
        );
    }

    @Test
    @Order(3)
    void senderBotMessageCapturedByOnChatMessage() throws InterruptedException {
        String probe = "E2E_PROBE_" + System.currentTimeMillis();

        TextChannel channel = senderJda.getTextChannelById(testChannelId);
        assertNotNull(channel, "Test channel not found — check DISCORD_TEST_CHANNEL_ID and bot permissions");
        channel.sendMessage(probe).queue();

        MessageReceivedEvent event = pollForMessage(botTask.receivedMessages, probe, 10);
        assertNotNull(event, "@OnChatMessage did not capture message within 10s: " + probe);
        assertEquals(probe, event.getMessage().getContentRaw());
    }

    @Test
    @Order(4)
    void filteredOnChatMessageFiresOnMatchingContent() throws InterruptedException {
        String probe = "E2E_FILTER_PROBE_" + System.currentTimeMillis();

        TextChannel channel = senderJda.getTextChannelById(testChannelId);
        assertNotNull(channel, "Test channel not found — check DISCORD_TEST_CHANNEL_ID and bot permissions");
        channel.sendMessage(probe).queue();

        MessageReceivedEvent event = pollForMessage(botTask.filteredMessages, null, 10);
        assertNotNull(event, "@OnChatMessage(ifMsgContains = \"E2E_FILTER_PROBE\") did not fire within 10s");
        assertTrue(event.getMessage().getContentRaw().contains("E2E_FILTER_PROBE"));
    }

    @Test
    @Order(5)
    void embedWithButtonsSentSuccessfully() {
        TextChannel channel = exampleBot.getJda().getTextChannelById(testChannelId);
        assertNotNull(channel, "Test channel not found — check DISCORD_TEST_CHANNEL_ID and bot permissions");

        var embed = new EmbedBuilder()
                .setTitle("JDA4Spring E2E Test")
                .setDescription("Verifying embed and button sending via JDA4Spring")
                .setColor(Color.BLUE)
                .build();

        Message sent = channel.sendMessageEmbeds(embed)
                .addComponents(ActionRow.of(Button.primary("e2e-test-button", "Test Button")))
                .complete();

        assertNotNull(sent, "Message with embed and button was not sent");
        assertFalse(sent.getEmbeds().isEmpty(), "Sent message has no embeds");
        assertFalse(sent.getComponents().isEmpty(), "Sent message has no button components");
    }

    /**
     * Polls a queue until a message matching the given probe string is found, or the timeout expires.
     * If probe is null, returns the first available message.
     */
    private MessageReceivedEvent pollForMessage(
            ConcurrentLinkedQueue<MessageReceivedEvent> queue,
            String probe,
            int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            MessageReceivedEvent event;
            while ((event = queue.poll()) != null) {
                if (probe == null || event.getMessage().getContentRaw().equals(probe)) {
                    return event;
                }
            }
            Thread.sleep(100);
        }
        return null;
    }
}
