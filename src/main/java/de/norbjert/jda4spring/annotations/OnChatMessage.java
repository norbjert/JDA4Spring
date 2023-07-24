package de.norbjert.jda4spring.annotations;

import java.lang.annotation.*;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
//@Reflective(AnnotationProcessor.class)//Todo
public @interface OnChatMessage {

    String ifMsgContains() default "";

}
