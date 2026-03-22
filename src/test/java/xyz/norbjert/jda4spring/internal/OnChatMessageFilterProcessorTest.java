package xyz.norbjert.jda4spring.internal;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnChatMessageFilterProcessorTest {

    @Mock MessageReceivedEvent event;
    @Mock Message message;
    @Mock Guild guild;
    @Mock MessageChannelUnion channelUnion;
    @Mock GuildMessageChannel guildChannel;

    // -- Dummy annotated methods to obtain @OnChatMessage instances via reflection --

    @OnChatMessage
    private void noFilters() {}

    @OnChatMessage(ifMsgContains = "hello")
    private void contentFilter() {}

    @OnChatMessage(ifMsgContains = "HELLO", ignoreCase = false)
    private void contentFilterCaseSensitive() {}

    @OnChatMessage(onServerViaServerName = "MyServer")
    private void serverNameFilter() {}

    @OnChatMessage(onServerViaServerName = "MyServer", ignoreCase = false)
    private void serverNameFilterCaseSensitive() {}

    @OnChatMessage(onServerViaServerId = "123456789")
    private void serverIdFilter() {}

    @OnChatMessage(inChannelViaChannelName = "general")
    private void channelNameFilter() {}

    @OnChatMessage(inChannelViaChannelId = "987654321")
    private void channelIdFilter() {}

    @OnChatMessage(ifMsgContains = "hello", onServerViaServerName = "MyServer")
    private void combinedFilters() {}

    private OnChatMessage cfg(String methodName) {
        return Arrays.stream(getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Method not found: " + methodName))
                .getAnnotation(OnChatMessage.class);
    }

    // -- Tests --

    @Test
    void noFilters_alwaysMatches() {
        assertTrue(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("noFilters")));
    }

    @Test
    void contentFilter_matchesWhenContains() {
        when(event.getMessage()).thenReturn(message);
        when(message.getContentRaw()).thenReturn("say hello world");
        assertTrue(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("contentFilter")));
    }

    @Test
    void contentFilter_failsWhenNotContains() {
        when(event.getMessage()).thenReturn(message);
        when(message.getContentRaw()).thenReturn("goodbye world");
        assertFalse(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("contentFilter")));
    }

    @Test
    void contentFilter_isCaseInsensitiveByDefault() {
        when(event.getMessage()).thenReturn(message);
        when(message.getContentRaw()).thenReturn("say HELLO world");
        assertTrue(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("contentFilter")));
    }

    @Test
    void contentFilter_respectsCaseSensitiveFlag() {
        when(event.getMessage()).thenReturn(message);
        when(message.getContentRaw()).thenReturn("say hello world"); // lowercase, filter expects "HELLO"
        assertFalse(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("contentFilterCaseSensitive")));
    }

    @Test
    void serverNameFilter_matchesCorrectServer() {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getName()).thenReturn("MyServer");
        assertTrue(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("serverNameFilter")));
    }

    @Test
    void serverNameFilter_isCaseInsensitiveByDefault() {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getName()).thenReturn("myserver");
        assertTrue(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("serverNameFilter")));
    }

    @Test
    void serverNameFilter_respectsCaseSensitiveFlag() {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getName()).thenReturn("myserver");
        assertFalse(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("serverNameFilterCaseSensitive")));
    }

    @Test
    void serverNameFilter_failsForDM() {
        when(event.isFromGuild()).thenReturn(false);
        assertFalse(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("serverNameFilter")));
    }

    @Test
    void serverNameFilter_failsForWrongServer() {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getName()).thenReturn("OtherServer");
        assertFalse(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("serverNameFilter")));
    }

    @Test
    void serverIdFilter_matchesCorrectId() {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn("123456789");
        assertTrue(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("serverIdFilter")));
    }

    @Test
    void serverIdFilter_failsForDM() {
        when(event.isFromGuild()).thenReturn(false);
        assertFalse(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("serverIdFilter")));
    }

    @Test
    void channelNameFilter_matchesCorrectChannel() {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getChannel()).thenReturn(channelUnion);
        when(channelUnion.asGuildMessageChannel()).thenReturn(guildChannel);
        when(guildChannel.getName()).thenReturn("general");
        assertTrue(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("channelNameFilter")));
    }

    @Test
    void channelNameFilter_failsForDM() {
        when(event.isFromGuild()).thenReturn(false);
        assertFalse(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("channelNameFilter")));
    }

    @Test
    void channelIdFilter_matchesCorrectId() {
        when(event.getChannel()).thenReturn(channelUnion);
        when(channelUnion.getId()).thenReturn("987654321");
        assertTrue(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("channelIdFilter")));
    }

    @Test
    void channelIdFilter_failsForWrongId() {
        when(event.getChannel()).thenReturn(channelUnion);
        when(channelUnion.getId()).thenReturn("000000000");
        assertFalse(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("channelIdFilter")));
    }

    @Test
    void combinedFilters_failsIfAnyFilterFails() {
        when(event.getMessage()).thenReturn(message);
        when(message.getContentRaw()).thenReturn("say hello");
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getName()).thenReturn("WrongServer");
        assertFalse(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("combinedFilters")));
    }

    @Test
    void combinedFilters_matchesWhenAllPass() {
        when(event.getMessage()).thenReturn(message);
        when(message.getContentRaw()).thenReturn("say hello");
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getName()).thenReturn("MyServer");
        assertTrue(OnChatMessageFilterProcessor.matchesAllFilters(event, cfg("combinedFilters")));
    }
}
