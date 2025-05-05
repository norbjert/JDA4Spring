package xyz.norbjert.jda4spring.annotations;

import java.lang.annotation.*;

/**
 *  allows for the creation of embed buttons on a discord bot
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SuppressWarnings("unused")
//@Reflective(AnnotationProcessor.class)//Todo
public @interface Button {

    /**
     *  a short description of the option and what its for
     */
    String value();

}
