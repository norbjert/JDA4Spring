package xyz.norbjert.jda4spring.annotations;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * A bot task represents a certain task the bot should be responsible for and is used as an annotation for a class.
 * {@code BotTask()} automatically registers the class as a Spring component.
 * A bot task can be linked to multiple discord bot accounts via the jda4spring config.
 * Inside a bot task, you can define methods annotated with @SlashCommand @OnChatMessage and @Button (or @ButtonHandler).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
//@Documented
@Component
@SuppressWarnings("unused")
public @interface BotTask {

    /**
     * A descriptive name for the bot task.
     * Use this to identify the bot task in the config file.
     *
     * @return a descriptive name for the bot task.
     */
    @AliasFor(annotation = Component.class)
    @SuppressWarnings("unused")
    String value() default "";

}
