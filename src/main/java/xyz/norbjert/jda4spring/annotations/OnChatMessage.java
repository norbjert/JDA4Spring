package xyz.norbjert.jda4spring.annotations;

import java.lang.annotation.*;

/**
 * A method with this annotation will get called whenever the bot receives a discord message.
 * Optionally you can add ifMsgContains = "some filter string", so it only gets called if there's a message containing that string
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
//@Reflective(AnnotationProcessor.class)//Todo
@SuppressWarnings("unused")
public @interface OnChatMessage {

    /**
     *  a short description of the option and what its for
     * @return a short description of the option and what its for
     */
    String ifMsgContains() default "";

}
