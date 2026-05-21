package xyz.norbjert.jda4spring.annotations;

import java.lang.annotation.*;

/**
 * a button handler
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ButtonHandler {

}
