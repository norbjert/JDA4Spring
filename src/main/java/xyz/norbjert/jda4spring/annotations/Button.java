package xyz.norbjert.jda4spring.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SuppressWarnings("unused")
//@Reflective(AnnotationProcessor.class)//Todo
public @interface Button {

    /**
     *  a short description of the option and what its for
     * @return a short description of the option and what its for
     */
    String value();

}
