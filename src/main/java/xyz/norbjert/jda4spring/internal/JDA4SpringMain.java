package xyz.norbjert.jda4spring.internal;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import xyz.norbjert.jda4spring.annotations.BotTask;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

/**
 * handles the initialisation process of the individual DiscordBots and configures them with their tasks, activities and API tokens (and soon gateway intents)
 */
@Component
public class JDA4SpringMain {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ApplicationContext appContext;
    private final String configFileLocation;

    public JDA4SpringMain(
            ApplicationContext appContext,
            @Value("${jda4spring.configFileLocation:src/main/resources/application.properties}") String configFileLocation) {
        JDA4SpringMain.instance = this;
        this.appContext = appContext;
        this.configFileLocation = configFileLocation;
        this.botTaskBeans = appContext.getBeansWithAnnotation(BotTask.class);


        try {
            List<BotConfigDataMapper> botConfigData = getBotConfigData().stream().filter(t -> t.getType().equals("token")).toList();
            for (BotConfigDataMapper dataEntry : botConfigData) {

                //basically a list with all entries for a bot with the name {BOTNAME}, so f.e. bots.{BOTNAME}.token = xyz
                List<BotConfigDataMapper> allEntriesForThisBot = getBotConfigData()
                        .stream().filter(t -> t.getName().equals(dataEntry.getName())).toList();

                String apiToken =
                        allEntriesForThisBot.stream().filter(t -> t.getType().equals("token")).toList().get(0).getValue()
                                .replace(" ", "");
                List<Object> botTasks =
                        getEventListenersForBotAsBotTasks(
                                allEntriesForThisBot.stream().filter(t -> t.getType().equals("tasks")).toList().get(0).getValue());
                Activity activity =
                        getActivity(
                                allEntriesForThisBot.stream().filter(t -> t.getType().contains("activity")).toList().get(0));
                List<GatewayIntent> gatewayIntents =
                        getGatewayIntents(
                                allEntriesForThisBot.stream().filter(t -> t.getType().equals("intents")).toList().get(0).getValue());

                DiscordBot newDiscordBotAccountInstance = new DiscordBot(apiToken, botTasks, activity, gatewayIntents);
                bots.add(newDiscordBotAccountInstance);
            }

        } catch (LoginException e) {
            logger.error("CANT LOG INTO DISCORD BOT ACC");
            System.exit(-1);
        } catch (InterruptedException e) {
            logger.error("COULD NOT WAIT FOR JDA TO FINISH INITIALISATION!");
            throw new RuntimeException(e);
        }
    }

    /**
     * manually reads the application.properties (or other defined config file) for all bot configurations
     */
    private List<BotConfigDataMapper> getBotConfigData() {
        List<BotConfigDataMapper> re = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(configFileLocation))) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("bots.")) {
                    re.add(new BotConfigDataMapper(
                            line.split("\\.")[1],
                            line.split("\\.")[2].split("=")[0].replace(" ", ""),
                            line.split("=")[1]));
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("BOT CONFIG FILE NOT FOUND!");
            throw new RuntimeException(e);
        } catch (IndexOutOfBoundsException e) {
            logger.error("Unexpected Error while reading the config file, a missing value for a key:" + e.getMessage());
        }
        return re;
    }

    private List<Object> getEventListenersForBotAsBotTasks(String tasks) {
        tasks = tasks.replace(" ", "");
        if (tasks.isEmpty()) {
            logger.warn("no tasks defined for bot, are you missing a \"bots.example.tasks = TaskBeanName\" line for you bot?");
            return new ArrayList<>();
        }
        return Stream.of(tasks.split(","))
                .map(appContext::getBean)
                .toList();
    }

    private List<GatewayIntent> getGatewayIntents(String gatewayIntents) {
        gatewayIntents = gatewayIntents.replace(" ", "").toUpperCase().replace("GatewayIntent.", "");
        if (gatewayIntents.isEmpty()) {
            logger.info("no Gateway Intents defined");
            return new ArrayList<>();
        }
        return Stream.of(gatewayIntents.split(","))
                .map(GatewayIntent::valueOf)
                .toList();
        //return List.of(GatewayIntent.values());
    }

    //returns the JDA Activity that got configured in the .config file
    private Activity getActivity(BotConfigDataMapper activity) {

        if (activity.getType().endsWith("playing")) {
            return Activity.playing(activity.getValue());
        } else if (activity.getType().endsWith("listening")) {
            return Activity.listening(activity.getValue());
        } else if (activity.getType().endsWith("watching")) {
            return Activity.watching(activity.getValue());
        } else if (activity.getType().endsWith("competing")) {
            return Activity.competing(activity.getValue());
        } else if (activity.getType().endsWith("activity")) {
            return Activity.customStatus(activity.getValue());
        } else {
            logger.warn("no activity set for:" + activity.getName());
            return Activity.customStatus("");
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////            Instance Stuff that can be used at runtime           /////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////

    private final List<DiscordBot> bots = new ArrayList<>(); //maybe make this statically available?
    private final Map<String, Object> botTaskBeans; //maybe make this statically available?
    @Getter
    private static JDA4SpringMain instance;

    /**
     * request the JDA instance for an eventListener
     *
     * @param clazz the eventListener in need of his JDA
     * @return the correct JDA if available
     */
    public List<JDA> getJDAInstances(Class<?> clazz) {
        List<JDA> foundInstances = new ArrayList<>();
        for (DiscordBot bot : bots) {
            for (Object botTask : bot.getBotTasks()) {
                if (botTask.getClass().equals(clazz)) {
                    foundInstances.add(bot.getJda());
                    break;
                }
            }
        }
        if (foundInstances.isEmpty()) {
            logger.error("Class " + clazz.getName() + " attempted to get a JDA instance, no matches found");
            return null;
        }
        return foundInstances;
    }
}
