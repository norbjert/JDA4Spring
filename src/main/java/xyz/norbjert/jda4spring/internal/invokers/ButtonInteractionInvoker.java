package xyz.norbjert.jda4spring.internal.invokers;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Handles the invocation of methods annotated with {@code @Button}.
 */
public class ButtonInteractionInvoker {

    private static final Logger logger = LoggerFactory.getLogger(ButtonInteractionInvoker.class);

    public static void invokeButtonInteractionMethod(Method annotatedMethod, Object declaringClass, ButtonInteractionEvent event) throws InvocationTargetException, IllegalAccessException {

            switch (annotatedMethod.getParameterCount()) {
                case 0 -> annotatedMethod.invoke(declaringClass);
                case 1 -> {
                    if (annotatedMethod.getParameterTypes()[0].getTypeName().contains("ButtonInteractionEvent")) {
                        annotatedMethod.invoke(declaringClass, event);
                    } else {
                        logger.error("ERROR INVOKING @Button or @ButtonHandler annotation");
                    }
                }
                default ->
                    //ToDo: smart implementation that automatically maps correct variables to the method
                        annotatedMethod.invoke(declaringClass, event);
            }
    }
}
