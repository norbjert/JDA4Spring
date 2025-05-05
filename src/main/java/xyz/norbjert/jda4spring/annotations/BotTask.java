package xyz.norbjert.jda4spring.annotations;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * A bot task represents a certain task the bot should be responsible for, and is used as annotation for a class
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
//@Documented
@Component
@SuppressWarnings("unused")
public @interface BotTask {

    /**
     *  a short description of the option and what its for
     * @return a
     */
    @AliasFor(annotation = Component.class)
    @SuppressWarnings("unused")
    String value() default "";

}
