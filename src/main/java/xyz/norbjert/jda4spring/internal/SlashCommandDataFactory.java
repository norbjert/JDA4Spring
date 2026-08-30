package xyz.norbjert.jda4spring.internal;

import xyz.norbjert.jda4spring.annotations.SlashCommand;
import xyz.norbjert.jda4spring.annotations.SlashCommandArg;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * static helper class, constructs a SlashCommandData object from an annotated method, converting the annotation into whatever JDA wants
 */
public class SlashCommandDataFactory {

    private static final Logger logger = LoggerFactory.getLogger(SlashCommandDataFactory.class);

    private SlashCommandDataFactory() {
    }

    /**
     * creates a new
     * @param slashMethod a method with the @SlashCommand annotation
     * @return the SlashCommandData, extracted from the annotation and, if f.e. no command="xyz" has been set, method name
     */
    public static SlashCommandData createSlashCommand(Method slashMethod) {

        SlashCommand annotation = slashMethod.getAnnotation(SlashCommand.class);

        SlashCommandData d = Commands.slash(
                getSlashCommandName(slashMethod, annotation),
                getSlashCommandDescription(annotation));
        for (SlashCommandArg arg : annotation.options()) {
            d.addOption(arg.optionType(), arg.name(), arg.description());
        }
        return d;
    }

    private static String getSlashCommandName(Method slashMethod, SlashCommand annotation) {

        String command = annotation.command();

        //falls back to the method name if no command="xyz" has been set
        if (command.equals(SlashCommand.USING_METHOD_NAME) || command.isEmpty()) {
            logger.debug("no name for slash command with method name \"{}\", using method name instead", slashMethod.getName());
            return slashMethod.getName().toLowerCase();
        }

        //checks if the slash command has capital letters in it (which discord does not allow to be used for slash commands)
        if (!command.toLowerCase().equals(command)) {
            logger.info("Discord does not allow for upper case letters in slash commands, please change {} to lower case", command);
        }
        return command.toLowerCase();
    }

    private static String getSlashCommandDescription(SlashCommand annotation) {

        //discord limits descriptions to 100 characters max
        if (annotation.description().length() > 100) {
            logger.info("Discord does not allow for descriptions longer than 100 characters, please change the description of {} to be shorter",
                    annotation.command());
            return annotation.description().substring(0, 100);
        }
        return annotation.description();
    }

}
