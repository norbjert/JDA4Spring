package xyz.norbjert.jda4spring.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * scans BotTasks for annotated methods
 */
public class AnnotationProcessor {

    private AnnotationProcessor() {
    }

    /**
     * internal helper method for initialization of the DiscordBot instance
     *
     * @param botTasks    the tasks that are to be scanned
     * @param annotations the annotations to look for, a method carrying any of them is returned
     * @return a list with all methods that carry at least one of the given annotations
     */
    @SafeVarargs
    static List<Method> find(List<Object> botTasks, Class<? extends Annotation>... annotations) {

        List<Method> found = new ArrayList<>();

        for (Object current : botTasks) {
            for (Method method : current.getClass().getDeclaredMethods()) {
                for (Class<? extends Annotation> annotation : annotations) {
                    if (method.getAnnotation(annotation) != null) {
                        found.add(method);
                        break;
                    }
                }
            }
        }
        return found;
    }
}
