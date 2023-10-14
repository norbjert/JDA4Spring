package xyz.norbjert.jda4spring.annotations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * scans a BotTask for all defined @SlashCommand and @OnChatMessage annotations
 */
public class AnnotationProcessor
        //extends SimpleReflectiveProcessor implements BeanPostProcessor
{


    private static final Logger logger = LoggerFactory.getLogger(AnnotationProcessor.class);

    //private static final List<Method> slashMethods = new ArrayList<>();

    /*@Override
    public void registerReflectionHints(@NotNull ReflectionHints hints, @NotNull AnnotatedElement element) {

        if (element.isAnnotationPresent(SlashCommand.class) && element instanceof Method method) {
            slashMethods.add(method);
            logger.info("added:"+method.getName());
        }
        logger.info("hi");
        super.registerReflectionHints(hints, element);

    }*/


    //uncommented way to get method list mapped to the object who they belong to
    //public static /*Map<Object,*/List<Method>/*>*/ getSlashCommandMethods(List<Object> botTasks) {

        /*return botTasks.stream()
                .collect(Collectors
                        .toMap(Function.identity(),
                                t -> slashMethods.stream()
                                .filter(m -> m.getDeclaringClass().equals(t.getClass())).toList()));*/
/*
        return botTasks.stream().map(t -> slashMethods.stream()
                        .filter(m -> m.getDeclaringClass().equals(t.getClass())).toList())
                .flatMap(Collection::stream)
                .toList();
    }
*/
    public static List<Method> findSlashCommands(List<Object> botTasks) {

        List<Method> slashCommands = new ArrayList<>();

        for (Object current : botTasks) {

            for (Method method : current.getClass().getDeclaredMethods()) {

                SlashCommand methodAnnotation = method.getAnnotation(SlashCommand.class);
                if (methodAnnotation != null) {
                    slashCommands.add(method);
                }
            }
        }
        return slashCommands;
    }

    public static List<Method> findChatMsgAnnotations(List<Object> botTasks) {

        List<Method> chatMsgAnnotations = new ArrayList<>();

        for (Object current : botTasks) {

            for (Method method : current.getClass().getDeclaredMethods()) {

                OnChatMessage methodAnnotation = method.getAnnotation(OnChatMessage.class);
                if (methodAnnotation != null) {
                    chatMsgAnnotations.add(method);
                }
            }
        }
        return chatMsgAnnotations;
    }
}