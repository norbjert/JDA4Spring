package xyz.norbjert.jda4spring.internal.invokers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageInteractionInvoker {

    public static void invokeChatInteractionMethod(Method annotatedMethod, Object declaringClass, MessageReceivedEvent event) {
        List<Object> args = new ArrayList<>();
        Parameter[] parameters = annotatedMethod.getParameters();

        for (Parameter param : parameters) {

            if (param.getType().isAssignableFrom(MessageReceivedEvent.class)) {
                args.add(event);
            } else if (param.getType().isAssignableFrom(String.class) && param.getName().equals("content")) { // Example: @OnChatMessage method might want `String content`
                args.add(event.getMessage().getContentDisplay());
            }

            else {
                args.add(null);
            }
        }

        MethodInvoker.invoke(annotatedMethod, declaringClass, args.toArray());
    }
}