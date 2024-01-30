package xyz.norbjert.jda4spring.annotations;

import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * a slash command argument, that can be used as a sub-element of the @SlashCommand annotation
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SlashCommandArg {

    /**
     * what data type the option will be (string, int,...) default is string
     * @return what data type the option will be (string, int,...) default is string
     */
    OptionType optionType() default OptionType.STRING;

    /**
     * a name to classify which option is which
     * @return a name to classify which option is which
     */
    String name();

    /**
     *  a short description of the option and what its for
     * @return a short description of the option and what its for
     */
    String description();

}

