package xyz.norbjert.jda4spring.internal.invokers;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Handles the invocation of methods annotated with {@code @Button}.
 */
public class ButtonInteractionInvoker {

    private static final Logger logger = LoggerFactory.getLogger(ButtonInteractionInvoker.class);

    /**
     * Invokes the given {@code annotatedMethod} on the {@code declaringClass}
     * @param annotatedMethod the method to invoke
     * @param declaringClass the class on which the method should be invoked
     * @param event the event that triggered the method invocation
     */
    public static void invokeButtonInteractionMethod(Method annotatedMethod, Object declaringClass, ButtonInteractionEvent event) {
            switch (annotatedMethod.getParameterCount()) {
                case 0 -> MethodInvoker.invoke(annotatedMethod, declaringClass);
                case 1 -> {
                    if (annotatedMethod.getParameterTypes()[0].getTypeName().contains("ButtonInteractionEvent")) {
                        MethodInvoker.invoke(annotatedMethod, declaringClass, event);
                    } else {
                        logger.error("ERROR INVOKING @Button or @ButtonHandler annotation");
                    }
                }
                //ToDo: smart implementation that automatically maps correct variables to the method
                default -> MethodInvoker.invoke(annotatedMethod, declaringClass, event);
            }
    }
}
