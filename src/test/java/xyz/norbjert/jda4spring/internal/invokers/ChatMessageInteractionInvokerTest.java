package xyz.norbjert.jda4spring.internal.invokers;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageInteractionInvokerTest {

    @Mock MessageReceivedEvent event;
    @Mock Message message;

    // -- Dummy target class --

    static class Target {
        MessageReceivedEvent capturedEvent;
        String capturedContent;
        Object capturedUnknown;
        boolean noArgCalled;

        public void withEvent(MessageReceivedEvent event) {
            this.capturedEvent = event;
        }

        public void withContent(MessageReceivedEvent event, String content) {
            this.capturedContent = content;
        }

        public void noArgs() {
            this.noArgCalled = true;
        }

        public void withUnknownParam(MessageReceivedEvent event, Integer unknown) {
            this.capturedUnknown = unknown;
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
    void injectsMessageReceivedEvent() {
        Target target = new Target();
        ChatMessageInteractionInvoker.invokeChatInteractionMethod(method("withEvent"), target, event);
        assertSame(event, target.capturedEvent);
    }

    @Test
    void injectsMessageContentForContentParam() {
        when(event.getMessage()).thenReturn(message);
        when(message.getContentDisplay()).thenReturn("Hello world");
        Target target = new Target();
        ChatMessageInteractionInvoker.invokeChatInteractionMethod(method("withContent"), target, event);
        assertEquals("Hello world", target.capturedContent);
    }

    @Test
    void handlesNoArgMethod() {
        Target target = new Target();
        ChatMessageInteractionInvoker.invokeChatInteractionMethod(method("noArgs"), target, event);
        assertTrue(target.noArgCalled);
    }

    @Test
    void injectsNullForUnknownParameterType() {
        Target target = new Target();
        ChatMessageInteractionInvoker.invokeChatInteractionMethod(method("withUnknownParam"), target, event);
        assertNull(target.capturedUnknown);
    }
}
