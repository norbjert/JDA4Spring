package xyz.norbjert.jda4spring.internal;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.Test;
import xyz.norbjert.jda4spring.annotations.*;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationProcessorTest {

    // -- Dummy BotTask classes --

    static class TaskWithSlashCommand {
        @SlashCommand(command = "ping", description = "test")
        public void ping(SlashCommandInteractionEvent event) {}

        public void notAnnotated() {}
    }

    static class TaskWithChatMessage {
        @OnChatMessage(ifMsgContains = "hello")
        public void onHello(MessageReceivedEvent event) {}
    }

    static class TaskWithButton {
        @Button("my-button")
        public void onButton(ButtonInteractionEvent event) {}
    }

    static class TaskWithButtonHandler {
        @ButtonHandler
        public void onAnyButton(ButtonInteractionEvent event) {}
    }

    static class TaskWithBothButtonTypes {
        @Button("specific")
        public void onSpecific(ButtonInteractionEvent event) {}

        @ButtonHandler
        public void onAny(ButtonInteractionEvent event) {}
    }

    static class TaskWithNoAnnotations {
        public void doSomething() {}
    }

    // -- Tests --

    @Test
    void findSlashCommands_returnsAnnotatedMethod() {
        List<Method> methods = AnnotationProcessor.findSlashCommands(List.of(new TaskWithSlashCommand()));
        assertEquals(1, methods.size());
        assertEquals("ping", methods.get(0).getName());
    }

    @Test
    void findSlashCommands_ignoresUnannotatedMethods() {
        List<Method> methods = AnnotationProcessor.findSlashCommands(List.of(new TaskWithNoAnnotations()));
        assertTrue(methods.isEmpty());
    }

    @Test
    void findSlashCommands_scansMultipleBotTasks() {
        List<Method> methods = AnnotationProcessor.findSlashCommands(
                List.of(new TaskWithSlashCommand(), new TaskWithSlashCommand()));
        assertEquals(2, methods.size());
    }

    @Test
    void findChatMsgAnnotations_returnsAnnotatedMethod() {
        List<Method> methods = AnnotationProcessor.findChatMsgAnnotations(List.of(new TaskWithChatMessage()));
        assertEquals(1, methods.size());
        assertEquals("onHello", methods.get(0).getName());
    }

    @Test
    void findChatMsgAnnotations_ignoresUnannotatedMethods() {
        List<Method> methods = AnnotationProcessor.findChatMsgAnnotations(List.of(new TaskWithNoAnnotations()));
        assertTrue(methods.isEmpty());
    }

    @Test
    void findButtonAnnotations_returnsButtonAnnotatedMethod() {
        List<Method> methods = AnnotationProcessor.findButtonAnnotations(List.of(new TaskWithButton()));
        assertEquals(1, methods.size());
        assertNotNull(methods.get(0).getAnnotation(Button.class));
    }

    @Test
    void findButtonAnnotations_returnsButtonHandlerAnnotatedMethod() {
        List<Method> methods = AnnotationProcessor.findButtonAnnotations(List.of(new TaskWithButtonHandler()));
        assertEquals(1, methods.size());
        assertNotNull(methods.get(0).getAnnotation(ButtonHandler.class));
    }

    @Test
    void findButtonAnnotations_returnsBothButtonAndButtonHandler() {
        List<Method> methods = AnnotationProcessor.findButtonAnnotations(List.of(new TaskWithBothButtonTypes()));
        assertEquals(2, methods.size());
    }
}
