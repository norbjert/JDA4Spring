package xyz.norbjert.jda4spring.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
//@Reflective(AnnotationProcessor.class)//Todo
@SuppressWarnings("unused")
public @interface ButtonHandler {

}
