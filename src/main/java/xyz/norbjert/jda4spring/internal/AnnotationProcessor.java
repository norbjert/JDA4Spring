package xyz.norbjert.jda4spring.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.norbjert.jda4spring.annotations.Button;
import xyz.norbjert.jda4spring.annotations.ButtonHandler;
import xyz.norbjert.jda4spring.annotations.OnChatMessage;
import xyz.norbjert.jda4spring.annotations.SlashCommand;

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

    /**
     * static class, don't instantiate it pls thank you
     */
    private AnnotationProcessor(){
        logger.error("static class, not to be instanced");
        throw new RuntimeException("AnnotationProcessor is a static class and cannot be instanced");
    }

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

    /**
     * internal helper method for initialisation of the DiscordBot instance
     * @param botTasks the tasks that are to be scanned for @SlashCommand Annotation
     * @return a list with all methods that have the @SlashCommand Annotation
     */
    static List<Method> findSlashCommands(List<Object> botTasks) {

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


    /**
     * internal helper method for initialisation of the DiscordBot instance
     * @param botTasks the tasks that are to be scanned for @OnChatMessage Annotation
     * @return a list with all methods that have the @OnChatMessage Annotation
     */
    static List<Method> findChatMsgAnnotations(List<Object> botTasks) {

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


    /**
     * internal helper method for initialisation of the DiscordBot instance
     * @param botTasks the tasks that are to be scanned for @Button or @ButtonHandler Annotation
     * @return a list with all methods that have the @Button or @ButtonHandler Annotation
     */
    static List<Method> findButtonAnnotations(List<Object> botTasks) {

        List<Method> buttonAnnotations = new ArrayList<>();

        for (Object current : botTasks) {

            for (Method method : current.getClass().getDeclaredMethods()) {

                Button methodAnnotation = method.getAnnotation(Button.class);
                if (methodAnnotation != null) {
                    buttonAnnotations.add(method);
                }
                ButtonHandler methodAnnotation2 = method.getAnnotation(ButtonHandler.class);
                if (methodAnnotation2 != null) {
                    buttonAnnotations.add(method);
                }
            }
        }
        return buttonAnnotations;
    }
}