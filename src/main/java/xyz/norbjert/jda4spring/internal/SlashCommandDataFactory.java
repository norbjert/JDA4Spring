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

    /**
     * static class, don't instantiate it pls thank you
     */
    private SlashCommandDataFactory(){
        logger.error("static class, not to be instanced");
        throw new RuntimeException("SlashCommandDataFactory is a static class and cannot be instanced");
    }

    /**
     * creates a new
     * @param slashMethod a method with the @SlashCommand annotation
     * @return the SlashCommandData, extracted from the annotation and, if f.e. no command="xyz" has been set, method name
     */
    public static SlashCommandData createSlashCommand(Method slashMethod) {

        SlashCommandData d = Commands.slash(
                getSlashCommandName(slashMethod),
                getSlashCommandDescription(slashMethod));
        for (SlashCommandArg arg : slashMethod.getAnnotation(SlashCommand.class).options()) {
            d.addOption(arg.optionType(), arg.name(), arg.description());
        }
        return d;
    }

    private static String getSlashCommandName(Method slashMethod) {

        //checks if the slash command has capital letters in it (which discord does not allow to be used for slash commands)
        if (!slashMethod.getAnnotation(SlashCommand.class).command().toLowerCase().equals(slashMethod.getAnnotation(SlashCommand.class).command())) {
            logger.info("Discord does not allow for upper case letters in slash commands, please change {} to lower case",
                    slashMethod.getAnnotation(SlashCommand.class).command());
        }

        //replaces the default name implementation with method name
        if (slashMethod.getAnnotation(SlashCommand.class).command().equals("<using method name>")) {
            logger.debug("no name for slash command with method name\"{}\", using method name instead",
                    slashMethod.getName());
            return slashMethod.getName().toLowerCase();
        }
        return slashMethod.getAnnotation(SlashCommand.class).command().toLowerCase();
    }

    private static String getSlashCommandDescription(Method slashMethod) {

        //discord limits descriptions to 100 characters max
        if (slashMethod.getAnnotation(SlashCommand.class).description().length() > 100) {
            logger.info("Discord does not allow for descriptions longer than 100 characters, please change the description of {} to be shorter",
                    slashMethod.getAnnotation(SlashCommand.class).command());
            return slashMethod.getAnnotation(SlashCommand.class).description().subSequence(0, 99).toString();
        }
        return slashMethod.getAnnotation(SlashCommand.class).description();
    }

}
