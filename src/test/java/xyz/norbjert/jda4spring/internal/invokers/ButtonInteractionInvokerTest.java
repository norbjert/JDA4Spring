package xyz.norbjert.jda4spring.internal.invokers;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ButtonInteractionInvokerTest {

    @Mock ButtonInteractionEvent event;

    // -- Dummy target class --

    static class Target {
        boolean noArgCalled;
        ButtonInteractionEvent capturedEvent;

        public void noArgs() {
            this.noArgCalled = true;
        }

        public void withEvent(ButtonInteractionEvent event) {
            this.capturedEvent = event;
        }

        public void withEventAndExtra(ButtonInteractionEvent event, String extra) {
            this.capturedEvent = event;
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
    void invokesNoArgMethod() {
        Target target = new Target();
        ButtonInteractionInvoker.invokeButtonInteractionMethod(method("noArgs"), target, event);
        assertTrue(target.noArgCalled);
    }

    @Test
    void injectsEventForSingleParamMethod() {
        Target target = new Target();
        ButtonInteractionInvoker.invokeButtonInteractionMethod(method("withEvent"), target, event);
        assertSame(event, target.capturedEvent);
    }

    @Test
    void invokesMultiParamMethodWithEvent() {
        // Default case: passes event as first argument, which causes an argument mismatch since the
        // method expects (ButtonInteractionEvent, String) but only event is passed.
        // This is a known limitation captured in the existing TODO in ButtonInteractionInvoker.
        // MethodInvoker wraps the underlying IllegalArgumentException in a RuntimeException.
        Target target = new Target();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ButtonInteractionInvoker.invokeButtonInteractionMethod(method("withEventAndExtra"), target, event));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }
}
