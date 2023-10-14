package xyz.norbjert.jda4spring.internal;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

/**
 * handles the initialisation process of the individual DiscordBots and configures them with their tasks, activities and API tokens (and soon gateway intents)
 * ToDo: maybe get rid of JDAInstanceManager and JDAInstanceTaskMapper
 */
@Component
public class JDA4SpringMain {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ApplicationContext appContext;
    private final String configFileLocation;

    private final List<DiscordBot> bots = new ArrayList<>();

    public JDA4SpringMain(
            ApplicationContext appContext,
            @Value("${jda4spring.configFileLocation:src/main/resources/application.properties}") String configFileLocation) {
        this.appContext = appContext;
        this.configFileLocation = configFileLocation;

        try {

            for (BotConfigDataMapper data : getBotConfigData().stream().filter(t -> t.getType().equals("token")).toList()) {

                //todo: allow for gateway intent definition in .config file; also improve Activity configuration
                DiscordBot bot = new DiscordBot(
                        getBotConfigData().stream().filter(t -> t.getName().equals(data.getName())).filter(t -> t.getType().equals("token")).toList().get(0).getValue().replace(" ", ""),
                        getEventListenersForBotAsBotTasks(getBotConfigData().stream().filter(t -> t.getName().equals(data.getName())).filter(t -> t.getType().equals("tasks")).toList().get(0).getValue()),
                        Activity.playing(getBotConfigData().stream().filter(t -> t.getName().equals(data.getName())).filter(t -> t.getType().equals("activity")).toList().get(0).getValue()),
                        GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT
                );
                bots.add(bot);
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
        }
        return re;
    }

    private List<Object> getEventListenersForBotAsBotTasks(String tasks) {
        tasks = tasks.replace(" ", "");
        if (tasks.equals("")) {
            logger.warn("couldnt find any tasks for bot");
            return new ArrayList<>();
        }
        return Stream.of(tasks.split(","))
                .map(appContext::getBean)
                .toList();
    }
}
