package xyz.norbjert.jda4spring.internal;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.norbjert.jda4spring.annotations.Button;
import xyz.norbjert.jda4spring.annotations.ButtonHandler;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;
import xyz.norbjert.jda4spring.annotations.SlashCommand;

import javax.security.auth.login.LoginException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import static xyz.norbjert.jda4spring.internal.OnChatMessageFilterProcessor.matchesAllFilters;
import static xyz.norbjert.jda4spring.internal.util.ButtonInteractionHandler.invokeButtonInteractionMethod;
import static xyz.norbjert.jda4spring.internal.util.ChatMessageInteractionHandler.invokeChatInteractionMethod;
import static xyz.norbjert.jda4spring.internal.util.SlashCommandInteractionHandler.invokeSlashMethod;

/**
 * Represents a single Discord bot account managed by JDA4Spring.
 * This class extends JDA's {@link ListenerAdapter} to handle incoming Discord events
 * and dispatches them to annotated methods within the provided bot tasks.
 * It manages the lifecycle of the JDA instance, including login, activity setting,
 * and registration of slash commands.
 */
public class DiscordBot extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(DiscordBot.class);
    @Getter
    private final List<Object> botTasks;
    @Getter
    private final JDA jda;
    private final List<Method> slashCommandMethods;
    private final List<Method> chatInteractionMethods;
    private final List<Method> buttonInteractionMethods;

    /**
     * Constructs a new {@code DiscordBot} instance, initializes the JDA client,
     * and registers annotated methods for various Discord interactions.
     *
     * @param apiToken The API token for the Discord bot.
     * @param botTasks A list of Spring-managed beans (classes annotated with {@code @BotTask})
     *                 that contain methods annotated for Discord interactions.
     * @param activity The {@link Activity} to be displayed for the bot (e.g., "Playing a game").
     * @param gatewayIntents A list of {@link GatewayIntent}s specifying which events the bot should receive.
     * @throws LoginException If there are issues authenticating with the provided API token.
     * @throws InterruptedException If the process is interrupted while waiting for the JDA instance to become ready.
     */
    public DiscordBot(String apiToken, List<Object> botTasks, Activity activity, List<GatewayIntent> gatewayIntents) throws LoginException, InterruptedException {
        jda = JDABuilder.createLight(apiToken, gatewayIntents)
                .addEventListeners(this)
                .setActivity(activity)
                .build()
                .awaitReady();

        this.botTasks = botTasks;
        this.chatInteractionMethods = AnnotationProcessor.findChatMsgAnnotations(botTasks);
        this.slashCommandMethods = AnnotationProcessor.findSlashCommands(botTasks);
        this.buttonInteractionMethods = AnnotationProcessor.findButtonAnnotations(botTasks);

        //publishes the slash commands to discord, so they show up in the preview for when you start typing /xyz
        jda.updateCommands().addCommands(slashCommandMethods.stream().map(SlashCommandDataFactory::createSlashCommand).toList()).queue();
    }


    /**
     * Handles incoming slash command interactions from Discord.
     * It iterates through registered methods annotated with {@link SlashCommand}
     * and invokes the matching method based on the command name.
     *
     * @param event The {@link SlashCommandInteractionEvent} received from Discord.
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        logSlashCommandInteractions(event);

        for (Method slashMethod : slashCommandMethods) {

            //if the incoming slash command matches the command="xyz" variable of the @SlashCommand annotation
            if (event.getName().equalsIgnoreCase(slashMethod.getAnnotation(SlashCommand.class).command())
                    //if the incoming slash command matches the java method name, not recommended but works as a secondary option for lazy ppl
                    || event.getName().equals(slashMethod.getName())) {

                invokeSlashMethod(slashMethod, getDeclaringInstance(slashMethod), event);
                return;
            }
        }
        logger.error("SlashCommand {} was called but was never declared properly.", event.getName());
    }

    /**
     * Handles incoming chat messages received by the bot.
     * It iterates through registered methods annotated with {@link OnChatMessage},
     * applies the defined filters, and invokes the matching methods.
     *
     * @param event The {@link MessageReceivedEvent} received from Discord.
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        for (Method method : chatInteractionMethods) {
            OnChatMessage cfg = method.getAnnotation(OnChatMessage.class);
            if (cfg == null) {
                continue;
            }

            if (cfg.ignoreBots() && event.getAuthor().isBot()) {
                continue;
            }

            if (!matchesAllFilters(event, cfg)) {
                continue;
            }

            Object declaringInstance = getDeclaringInstance(method);
            if (declaringInstance == null) {
                logger.error("Could not resolve declaring instance for method: {}", method.getName());
                continue;
            }

            invokeChatInteractionMethod(method, declaringInstance, event);
        }
    }


    /**
     * Handles incoming button interaction events from Discord.
     * It iterates through registered methods annotated with {@link Button} or {@link ButtonHandler}
     * and invokes the appropriate method based on the button's custom ID.
     *
     * @param event The {@link ButtonInteractionEvent} received from Discord.
     */
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        logger.debug("eventID: {}", event.getComponentId());

        for (Method buttonMethod : buttonInteractionMethods) {

            //ButtonHandler -> gets called on every button interaction
            if (buttonMethod.getAnnotation(ButtonHandler.class) != null
                    //if the button event matches the ID of the @Button annotation
                    || (buttonMethod.getAnnotation(Button.class) != null &&
                            event.getComponentId().equals(buttonMethod.getAnnotation(Button.class).value()))) {

                try {

                    Object declaringInstance = getDeclaringInstance(buttonMethod);
                    
                    invokeButtonInteractionMethod(buttonMethod, declaringInstance, event);

                } catch (InvocationTargetException ex) {
                    logger.error("InvocationTargetException:" + ex.getMessage());
                    throw new RuntimeException(ex);
                } catch (IllegalAccessException ex) {
                    logger.error("IllegalAccessException" + ex.getMessage());
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * logs every slash command interaction received by the bot
     * @param event the event that triggered the function
     */
    //todo: consider configuring the logging via log levels
    private void logSlashCommandInteractions(SlashCommandInteractionEvent event){
        if (event.getGuild() == null) {
            logger.info("Received: /" + event.getName()
                    + " " + event.getOptions().stream().map(OptionMapping::getAsString).toList()
                    + " in channel: " + event.getChannel().getName()
                    + " via direct message"
                    + " from user: " + event.getUser().getName());
        } else {
            logger.info("Received: /" + event.getName()
                    + " " + event.getOptions().stream().map(OptionMapping::getAsString).toList()
                    + " in channel: " + event.getChannel().getName()
                    + " on server: " + Objects.requireNonNull(event.getGuild()).getName()
                    + " from user: " + event.getUser().getName());
        }
    }

    /**
     * helper method to get the botTask that declares the method
     * @param method the method to get the declaring instance for
     * @return the botTask that declares the method, or null if not found
     */
    private Object getDeclaringInstance(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        for (Object task : botTasks) {
            if (task.getClass().equals(declaringClass)) {
                return task;
            }
        }
        return null;
    }
}
