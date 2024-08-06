package xyz.norbjert.jda4spring.annotations;

import java.lang.annotation.*;


/**
 * use this Annotation to define a method as slashCommand, it will automatically get processed and executed whenever called
 * Example:

 * SlashCommand(command = "test")
 * public void testName(SlashCommandInteractionEvent event){
 *     [your code here]
 * }

 * MultiArgsExample:

 * SlashCommand(command = "multiargtest", description = "multiArgTestDescription", options = {
 * SlashCommandArg(arg = "arg1", description = "arg1Description"),
 * SlashCommandArg(arg = "arg2", description = "arg2Description")
 *      })
 * public void testMultiArgs(SlashCommandInteractionEvent event, List String listOfArgs){ }

 * Important Notes to keep in mind:
 * 1. the first argument of any slash command method needs to be of type SlashCommandInteractionEvent (even if you dont use it)
 * 2. command names need to be lower case
 * 3. remember to implement the BotTask interface in the class you want to use @SlashCommand
 * 4. remember to also define the task for the correct bot in the .properties file
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
//@Reflective(AnnotationProcessor.class)//todo
@SuppressWarnings("unused")
public @interface SlashCommand {

    /**
     *  a short description of the option and what its for
     * @return a short description of the option and what its for
     */
    String command() default "<using method name>";
    /**
     *  a short description of the option and what its for
     * @return a short description of the option and what its for
     */
    String description() default "<no description provided>";
    /**
     *  a short description of the option and what its for
     * @return a short description of the option and what its for
     */
    SlashCommandArg[] options() default {};

}
