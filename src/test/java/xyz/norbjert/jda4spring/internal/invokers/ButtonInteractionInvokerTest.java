package xyz.norbjert.jda4spring.internal.invokers;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
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
    void invokesNoArgMethod() throws InvocationTargetException, IllegalAccessException {
        Target target = new Target();
        ButtonInteractionInvoker.invokeButtonInteractionMethod(method("noArgs"), target, event);
        assertTrue(target.noArgCalled);
    }

    @Test
    void injectsEventForSingleParamMethod() throws InvocationTargetException, IllegalAccessException {
        Target target = new Target();
        ButtonInteractionInvoker.invokeButtonInteractionMethod(method("withEvent"), target, event);
        assertSame(event, target.capturedEvent);
    }

    @Test
    void invokesMultiParamMethodWithEvent() throws InvocationTargetException, IllegalAccessException {
        // Default case: passes event as first argument, which causes IllegalArgumentException
        // since the method signature expects (ButtonInteractionEvent, String) but only event is passed.
        // This is a known limitation captured in the existing TODO in ButtonInteractionInvoker.
        Target target = new Target();
        assertThrows(IllegalArgumentException.class,
                () -> ButtonInteractionInvoker.invokeButtonInteractionMethod(method("withEventAndExtra"), target, event));
    }
}
