package xyz.norbjert.jda4spring.internal;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;

/**
 * Utility class for processing and applying filters defined in the {@link OnChatMessage} annotation.
 * This class provides static methods to check if a {@link MessageReceivedEvent} matches the criteria
 * specified by an {@code @OnChatMessage} annotation.
 */
public class OnChatMessageFilterProcessor {

    /**
     * Checks if a given {@link MessageReceivedEvent} matches all the filters
     * specified in the provided {@link OnChatMessage} configuration.
     * All non-empty filters must match for this method to return {@code true}.
     *
     * @param event The {@link MessageReceivedEvent} to check against the filters.
     * @param cfg The {@link OnChatMessage} annotation configuration containing the filters.
     * @return {@code true} if the event matches all specified filters, {@code false} otherwise.
     */
    static boolean matchesAllFilters(@NotNull MessageReceivedEvent event, @NotNull OnChatMessage cfg) {
        return matchesContent(event, cfg)
                && matchesGuildName(event, cfg)
                && matchesGuildId(event, cfg)
                && matchesChannelName(event, cfg)
                && matchesChannelId(event, cfg);
    }

    /**
     * Checks if the message content in the event matches the {@code ifMsgContains} filter.
     *
     * @param event The {@link MessageReceivedEvent}.
     * @param cfg The {@link OnChatMessage} configuration.
     * @return {@code true} if the content filter is empty or matches, {@code false} otherwise.
     */
    private static boolean matchesContent(@NotNull MessageReceivedEvent event, @NotNull OnChatMessage cfg) {
        String filter = cfg.ifMsgContains();
        if (filter.isEmpty()) {
            return true;
        }

        String content = event.getMessage().getContentRaw();
        if (cfg.ignoreCase()) {
            content = content.toLowerCase(java.util.Locale.ROOT);
            filter = filter.toLowerCase(java.util.Locale.ROOT);
        }
        return content.contains(filter);
    }

    /**
     * Checks if the guild name in the event matches the {@code onServerViaServerName} filter.
     *
     * @param event The {@link MessageReceivedEvent}.
     * @param cfg The {@link OnChatMessage} configuration.
     * @return {@code true} if the guild name filter is empty or matches, {@code false} otherwise.
     */
    private static boolean matchesGuildName(@NotNull MessageReceivedEvent event, @NotNull OnChatMessage cfg) {
        String expectedName = cfg.onServerViaServerName();
        if (expectedName.isEmpty()) {
            return true;
        }
        if (!event.isFromGuild()) {
            return false;
        }

        String actualName = event.getGuild().getName();
        return cfg.ignoreCase()
                ? actualName.equalsIgnoreCase(expectedName)
                : actualName.equals(expectedName);
    }

    /**
     * Checks if the guild ID in the event matches the {@code onServerViaServerId} filter.
     *
     * @param event The {@link MessageReceivedEvent}.
     * @param cfg The {@link OnChatMessage} configuration.
     * @return {@code true} if the guild ID filter is empty or matches, {@code false} otherwise.
     */
    private static boolean matchesGuildId(@NotNull MessageReceivedEvent event, @NotNull OnChatMessage cfg) {
        String expectedId = cfg.onServerViaServerId();
        if (expectedId.isEmpty()) {
            return true;
        }
        if (!event.isFromGuild()) {
            return false;
        }
        return event.getGuild().getId().equals(expectedId);
    }

    /**
     * Checks if the channel name in the event matches the {@code inChannelViaChannelName} filter.
     * This filter is only applicable to messages from a guild.
     *
     * @param event The {@link MessageReceivedEvent}.
     * @param cfg The {@link OnChatMessage} configuration.
     * @return {@code true} if the channel name filter is empty or matches, {@code false} otherwise.
     */
    private static boolean matchesChannelName(@NotNull MessageReceivedEvent event, @NotNull OnChatMessage cfg) {
        String expectedName = cfg.inChannelViaChannelName();
        if (expectedName.isEmpty()) {
            return true;
        }
        // Only applicable to guild channels (DMs and other contexts have no comparable "name")
        if (!event.isFromGuild()) {
            return false;
        }

        String actualName = event.getChannel().asGuildMessageChannel().getName();
        return cfg.ignoreCase()
                ? actualName.equalsIgnoreCase(expectedName)
                : actualName.equals(expectedName);
    }

    /**
     * Checks if the channel ID in the event matches the {@code inChannelViaChannelId} filter.
     *
     * @param event The {@link MessageReceivedEvent}.
     * @param cfg The {@link OnChatMessage} configuration.
     * @return {@code true} if the channel ID filter is empty or matches, {@code false} otherwise.
     */
    private static boolean matchesChannelId(@NotNull MessageReceivedEvent event, @NotNull OnChatMessage cfg) {
        String expectedId = cfg.inChannelViaChannelId();
        if (expectedId.isEmpty()) {
            return true;
        }
        return event.getChannel().getId().equals(expectedId);
    }
}