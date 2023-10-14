package xyz.norbjert.jda4spring.internal;

import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * simple help class, to allow BotTasks to request their jda instance
 * Todo: get rid of this crap, put the functionality into DiscordBot or remove it entirely
 */
@Component()
public class JDAInstanceManager {

    private static final Logger logger = LoggerFactory.getLogger(JDAInstanceManager.class);

    private static final List<JDA> jdaList = new ArrayList<>();

    private static final List<JDAInstanceTaskMapper> jdaInstanceTasks = new ArrayList<>();

    public static void addJDAWithBotTasks(JDAInstanceTaskMapper element){
        jdaInstanceTasks.add(element);
    }

    /**
     * request the JDA instance for an eventListener
     * @param clazz the eventListener in need of his JDA
     * @return the correct JDA if available
     */
    public static JDA requestJDAInstance(Class<?> clazz) {
        return getJDAInstance(clazz.getName());
    }

    private static JDA getJDAInstance(String className) {

        //new way of storing jda instances and mapping them to their tasks
        for(JDAInstanceTaskMapper jdaInstance: jdaInstanceTasks){
            for(Object task: jdaInstance.getBotTasks()){
                if(task.getClass().getName().equals(className)){
                    return jdaInstance.getJda();
                }
            }
        }

        logger.error("Class " + className + " attempted to get a JDA instance, no matches found");
        return null;
    }



}
