package xyz.norbjert.jda4spring.annotations;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
//@Documented
@Component
public @interface BotTask {

    /**
     *  a short description of the option and what its for
     * @return a short description of the option and what its for
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

}
