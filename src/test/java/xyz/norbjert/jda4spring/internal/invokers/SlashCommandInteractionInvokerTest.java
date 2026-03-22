package xyz.norbjert.jda4spring.internal.invokers;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.norbjert.jda4spring.annotations.SlashCommand;
import xyz.norbjert.jda4spring.annotations.SlashCommandArg;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlashCommandInteractionInvokerTest {

    @Mock SlashCommandInteractionEvent event;
    @Mock OptionMapping stringOption;
    @Mock OptionMapping booleanOption;

    // -- Dummy target class with public methods for invocation --

    static class Target {
        SlashCommandInteractionEvent capturedEvent;
        String capturedString;
        List<String> capturedList;
        Boolean capturedBool;
        boolean called;

        @SlashCommand(command = "test")
        public void withEvent(SlashCommandInteractionEvent event) {
            this.capturedEvent = event;
            this.called = true;
        }

        @SlashCommand(command = "test", options = {@SlashCommandArg(name = "message", description = "text")})
        public void withStringOption(SlashCommandInteractionEvent event, String message) {
            this.capturedString = message;
        }

        @SlashCommand(command = "test")
        public void withStringList(SlashCommandInteractionEvent event, List<String> strings) {
            this.capturedList = strings;
        }

        @SlashCommand(command = "test")
        public void withMissingObjectOption(SlashCommandInteractionEvent event, String missing) {
            this.capturedString = missing;
        }

        @SlashCommand(command = "test")
        public void withMissingPrimitiveOption(SlashCommandInteractionEvent event, int count) {}

        @SlashCommand(command = "test", options = {@SlashCommandArg(name = "enabled", description = "flag", optionType = OptionType.BOOLEAN)})
        public void withBooleanOption(SlashCommandInteractionEvent event, Boolean enabled) {
            this.capturedBool = enabled;
        }
    }

    private Method method(String name) {
        for (Method m : Target.class.getDeclaredMethods()) {
            if (m.getName().equals(name)) return m;
        }
        throw new RuntimeException("Method not found: " + name);
    }

    // -- Tests --

    @Test
    void injectsSlashCommandEvent() {
        Target target = new Target();
        SlashCommandInteractionInvoker.invokeSlashMethod(method("withEvent"), target, event);
        assertSame(event, target.capturedEvent);
        assertTrue(target.called);
    }

    @Test
    void injectsStringListOfAllStringOptions() {
        when(event.getOptions()).thenReturn(List.of(stringOption));
        when(stringOption.getType()).thenReturn(OptionType.STRING);
        when(stringOption.getAsString()).thenReturn("hello");
        Target target = new Target();
        SlashCommandInteractionInvoker.invokeSlashMethod(method("withStringList"), target, event);
        assertEquals(List.of("hello"), target.capturedList);
    }

    @Test
    void injectsStringOptionByParameterName() {
        when(event.getOption("message")).thenReturn(stringOption);
        when(stringOption.getType()).thenReturn(OptionType.STRING);
        when(stringOption.getAsString()).thenReturn("test message");
        Target target = new Target();
        SlashCommandInteractionInvoker.invokeSlashMethod(method("withStringOption"), target, event);
        assertEquals("test message", target.capturedString);
    }

    @Test
    void injectsBooleanOption() {
        when(event.getOption("enabled")).thenReturn(booleanOption);
        when(booleanOption.getType()).thenReturn(OptionType.BOOLEAN);
        when(booleanOption.getAsBoolean()).thenReturn(true);
        Target target = new Target();
        SlashCommandInteractionInvoker.invokeSlashMethod(method("withBooleanOption"), target, event);
        assertEquals(true, target.capturedBool);
    }

    @Test
    void injectsNullForMissingObjectOption() {
        when(event.getOption("missing")).thenReturn(null);
        Target target = new Target();
        SlashCommandInteractionInvoker.invokeSlashMethod(method("withMissingObjectOption"), target, event);
        assertNull(target.capturedString);
    }

    @Test
    void throwsForMissingPrimitiveOption() {
        when(event.getOption("count")).thenReturn(null);
        Target target = new Target();
        assertThrows(IllegalArgumentException.class,
                () -> SlashCommandInteractionInvoker.invokeSlashMethod(method("withMissingPrimitiveOption"), target, event));
    }
}
