package xyz.norbjert.jda4spring.internal;

import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import xyz.norbjert.jda4spring.annotations.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * represents a single discord bot account
 */
public class DiscordBot extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Getter
    private final List<Object> botTasks;
    @Getter
    private final JDA jda;
    private final List<Method> slashCommandMethods;
    private final List<Method> chatInteractionMethods;
    private final List<Method> buttonInteractionMethods;

    public DiscordBot(String apiToken, List<Object> botTasks, Activity activity, GatewayIntent... gatewayIntents) throws LoginException, InterruptedException {
        jda = JDABuilder.createLight(apiToken, Arrays.asList(gatewayIntents))
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
     * gets called once the bot received a slash command and calls the respective method associated with that slash command
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

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


        for (Method slashMethod : slashCommandMethods) {

            //if the incoming slash command matches the command="xyz" variable of the @SlashCommand annotation
            if (event.getName().equalsIgnoreCase(slashMethod.getAnnotation(SlashCommand.class).command())
                    //if the incoming slash command matches the java method name, not recommended but works a secondary option for lazy ppl
                    || event.getName().equals(slashMethod.getName())) {
                try {

                    //gets the botTask that declares the method
                    Object declaringClass = botTasks.stream().filter(e -> e.getClass().equals(slashMethod.getDeclaringClass())).toList().get(0);
                    invokeSlashMethod(slashMethod, declaringClass, event);
                    return;

                } catch (InvocationTargetException ex) {
                    logger.error("InvocationTargetException:" + ex.getMessage());
                    throw new RuntimeException(ex);
                } catch (IllegalAccessException ex) {
                    logger.error("IllegalAccessException" + ex.getMessage());
                    throw new RuntimeException(ex);
                }
            }
        }
        logger.error("SlashCommand " + event.getName() + " was called but could not found");
    }

    private void invokeSlashMethod(Method slashMethod, Object declaringClass, SlashCommandInteractionEvent event) throws InvocationTargetException, IllegalAccessException {

        if (slashMethod.trySetAccessible()) {
            switch (slashMethod.getParameterCount()) {
                case 0 -> slashMethod.invoke(declaringClass);
                case 1 -> {
                    //System.out.println(slashMethod.getParameterTypes()[0]);
                    if (slashMethod.getParameterTypes()[0].getTypeName().contains("SlashCommandInteractionEvent")) {
                        slashMethod.invoke(declaringClass, event);
                    } else {
                        slashMethod.invoke(declaringClass, event.getOptions().stream().map(OptionMapping::getAsString).toList());
                    }
                }
                default ->
                    //ToDo: smart implementation that automatically maps correct variables to the method
                        slashMethod.invoke(declaringClass, event, event.getOptions().stream().map(OptionMapping::getAsString).toList());
            }
        } else {
            logger.error("slashMethod \"" + slashMethod.getName() + "\" is not public and Java language access control cannot be suppressed");
        }
    }


    /**
     * gets called once the bot received a chat message and calls the respective method(s) to handle it
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        for (Method current : chatInteractionMethods) {

            //if "" -> gets called on every msg
            if (current.getAnnotation(OnChatMessage.class).ifMsgContains().isEmpty()
                    //if msg contained the filter word
                    || event.getMessage().getContentRaw().contains(current.getAnnotation(OnChatMessage.class).ifMsgContains())) {

                try {

                    Object declaringClass = botTasks.stream().filter(e -> e.getClass().equals(current.getDeclaringClass())).toList().get(0);
                    invokeChatInteractionMethod(current, declaringClass, event);

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

    private void invokeChatInteractionMethod(Method annotatedMethod, Object declaringClass, MessageReceivedEvent event) throws InvocationTargetException, IllegalAccessException {

        if (annotatedMethod.trySetAccessible()) {
            switch (annotatedMethod.getParameterCount()) {
                case 0 -> annotatedMethod.invoke(declaringClass);
                case 1 -> {
                    //System.out.println(annotatedMethod.getParameterTypes()[0]);
                    if (annotatedMethod.getParameterTypes()[0].getTypeName().contains("MessageReceivedEvent")) {
                        annotatedMethod.invoke(declaringClass, event);
                    } else {
                        logger.error("ERROR INVOKING @OnChatMessage annotation");
                    }
                }
                default ->
                    //ToDo: smart implementation that automatically maps correct variables to the method
                        annotatedMethod.invoke(declaringClass, event);
            }
        } else {
            logger.error("annotatedMethod \"" + annotatedMethod.getName() + "\" is not public and Java language access control cannot be suppressed");
        }
    }


    /**
     * gets called once the bot received a Button Interaction and calls the respective method(s) to handle it
     */
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        System.out.println("eventID:"+event.getComponentId());

        for (Method current : buttonInteractionMethods) {

            System.out.println("buttonhandler:"+current.getAnnotation(ButtonHandler.class));
            System.out.println("button:"+current.getAnnotation(Button.class));

            //ButtonHandler -> gets called on every button interaction
            if (current.getAnnotation(ButtonHandler.class) != null
                    //if button event matches the ID of the @Button annotation
                    || (current.getAnnotation(Button.class) != null &&
                            event.getComponentId().equals(current.getAnnotation(Button.class).value()))) {

                try {

                    Object declaringClass = botTasks.stream().filter(e -> e.getClass().equals(current.getDeclaringClass())).toList().get(0);
                    invokeButtonInteractionMethod(current, declaringClass, event);

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

    private void invokeButtonInteractionMethod(Method annotatedMethod, Object declaringClass, ButtonInteractionEvent event) throws InvocationTargetException, IllegalAccessException {

        if (annotatedMethod.trySetAccessible()) {
            switch (annotatedMethod.getParameterCount()) {
                case 0 -> annotatedMethod.invoke(declaringClass);
                case 1 -> {
                    //System.out.println(annotatedMethod.getParameterTypes()[0]);
                    if (annotatedMethod.getParameterTypes()[0].getTypeName().contains("ButtonInteractionEvent")) {
                        annotatedMethod.invoke(declaringClass, event);
                    } else {
                        logger.error("ERROR INVOKING @Button or @ButtonHandler annotation");
                    }
                }
                default ->
                    //ToDo: smart implementation that automatically maps correct variables to the method
                        annotatedMethod.invoke(declaringClass, event);
            }
        } else {
            logger.error("annotatedMethod \"" + annotatedMethod.getName() + "\" is not public and Java language access control cannot be suppressed");
        }
    }

}
