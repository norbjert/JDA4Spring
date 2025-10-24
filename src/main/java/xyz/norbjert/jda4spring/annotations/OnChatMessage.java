package xyz.norbjert.jda4spring.annotations;

import java.lang.annotation.*;

/**
 * Marks a method to be invoked when a chat message is received in any channel the bot can read.
 * You can specify optional filters to restrict invocation by message content, server (guild), and channel.
 * Notes:
 * - An empty string ("") means "no filter" for that field.
 * - All non-empty filters are combined with logical AND (all must match).
 * - Case sensitivity for string-based filters is controlled by {@link #ignoreCase()}.
 * - ID-based filters are not affected by case sensitivity.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
//@Reflective(AnnotationProcessor.class)//Todo
@SuppressWarnings("unused")
public @interface OnChatMessage {

    /**
     * Message-content substring filter.
     * - If non-empty, the method is invoked only if the message content contains this substring.
     * - Comparison respects {@link #ignoreCase()} (IDs are unaffected).
     * Default: "" (no content filter).
     * @return the substring filter
     */
    String ifMsgContains() default "";

    /**
     * Server (guild) name filter.
     * - If non-empty, the method is invoked only for messages from a server whose name matches exactly.
     * - Comparison respects {@link #ignoreCase()}.
     * Default: "" (no server-name filter).
     * @return the server name filter
     */
    String onServerViaServerName() default "";

    /**
     * Server (guild) ID filter.
     * - If non-empty, the method is invoked only for messages from the server with this exact ID.
     * - a Case-insensitive flag does not apply to IDs.
     * Default: "" (no server-ID filter).
     * @return the server ID filter
     */
    String onServerViaServerId() default "";

    /**
     * Channel name filter.
     * - If non-empty, the method is invoked only for messages from a channel whose name matches exactly.
     * - Comparison respects {@link #ignoreCase()}.
     * Default: "" (no channel-name filter).
     * @return the channel name filter
     */
    String inChannelViaChannelName() default "";

    /**
     * Channel ID filter.
     * - If non-empty, the method is invoked only for messages from the channel with this exact ID.
     * - a Case-insensitive flag does not apply to IDs.
     * Default: "" (no channel-ID filter).
     * @return the channel ID filter
     */
    String inChannelViaChannelId() default "";

    /**
     * Whether to ignore messages authored by bot users.
     * - true: skip messages from bots
     * - false: process messages from both humans and bots
     * Default: false.
     * @return whether to ignore bot users
     */
    boolean ignoreBots() default false;

    /**
     * Whether string-based comparisons are case-insensitive.
     * Applies to {@link #ifMsgContains()}, {@link #onServerViaServerName()}, and {@link #inChannelViaChannelName()}.
     * Does not affect ID-based filters.
     * Default: true.
     * @return whether to ignore upper/lower case
     */
    boolean ignoreCase() default true;

}