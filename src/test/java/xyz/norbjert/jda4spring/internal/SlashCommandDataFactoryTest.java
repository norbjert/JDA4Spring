package xyz.norbjert.jda4spring.internal;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.junit.jupiter.api.Test;
import xyz.norbjert.jda4spring.annotations.SlashCommand;
import xyz.norbjert.jda4spring.annotations.SlashCommandArg;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class SlashCommandDataFactoryTest {

    // -- Dummy annotated methods to drive the factory --

    @SlashCommand(command = "ping", description = "Pings the bot")
    public void pingMethod(SlashCommandInteractionEvent event) {}

    @SlashCommand // no command= → falls back to method name
    public void myCommand(SlashCommandInteractionEvent event) {}

    @SlashCommand(command = "MixedCase", description = "test")
    public void mixedCaseCommand(SlashCommandInteractionEvent event) {}

    @SlashCommand(command = "toolong", description = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
    public void tooLongDescriptionCommand(SlashCommandInteractionEvent event) {}

    @SlashCommand(command = "multi", description = "has options", options = {
            @SlashCommandArg(name = "arg1", description = "first"),
            @SlashCommandArg(name = "arg2", description = "second")
    })
    public void multiArgCommand(SlashCommandInteractionEvent event) {}

    private Method method(String name) throws NoSuchMethodException {
        for (Method m : getClass().getDeclaredMethods()) {
            if (m.getName().equals(name)) return m;
        }
        throw new NoSuchMethodException(name);
    }

    // -- Tests --

    @Test
    void usesCommandAttributeAsName() throws NoSuchMethodException {
        SlashCommandData data = SlashCommandDataFactory.createSlashCommand(method("pingMethod"));
        assertEquals("ping", data.getName());
    }

    @Test
    void usesDescriptionFromAnnotation() throws NoSuchMethodException {
        SlashCommandData data = SlashCommandDataFactory.createSlashCommand(method("pingMethod"));
        assertEquals("Pings the bot", data.getDescription());
    }

    @Test
    void fallsBackToMethodNameWhenNoCommandSet() throws NoSuchMethodException {
        SlashCommandData data = SlashCommandDataFactory.createSlashCommand(method("myCommand"));
        assertEquals("mycommand", data.getName());
    }

    @Test
    void lowercasesCommandName() throws NoSuchMethodException {
        SlashCommandData data = SlashCommandDataFactory.createSlashCommand(method("mixedCaseCommand"));
        assertEquals("mixedcase", data.getName());
    }

    @Test
    void truncatesDescriptionLongerThan100Chars() throws NoSuchMethodException {
        SlashCommandData data = SlashCommandDataFactory.createSlashCommand(method("tooLongDescriptionCommand"));
        assertTrue(data.getDescription().length() <= 100);
    }

    @Test
    void includesAllOptions() throws NoSuchMethodException {
        SlashCommandData data = SlashCommandDataFactory.createSlashCommand(method("multiArgCommand"));
        assertEquals(2, data.getOptions().size());
        assertEquals("arg1", data.getOptions().get(0).getName());
        assertEquals("arg2", data.getOptions().get(1).getName());
    }
}
