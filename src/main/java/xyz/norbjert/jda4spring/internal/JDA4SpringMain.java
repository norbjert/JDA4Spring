package xyz.norbjert.jda4spring.internal;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import xyz.norbjert.jda4spring.annotations.BotTask;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Handles the initialization process of individual Discord bot accounts configured in the application.
 * It discovers bot configurations from various property sources (application.properties, jda4spring.properties/yml/yaml)
 * and an optional custom config file, then sets up JDA instances accordingly.
 */
@Component
@PropertySources({
        @PropertySource(value = "classpath:jda4spring.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "classpath:jda4spring.yml", ignoreResourceNotFound = true, factory = YamlPropertySourceFactory.class),
        @PropertySource(value = "classpath:jda4spring.yaml", ignoreResourceNotFound = true, factory = YamlPropertySourceFactory.class)
})
public class JDA4SpringMain {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ApplicationContext appContext;
    private final ConfigurableEnvironment environment;

    @Value("${jda4spring.configfile:#{null}}")
    private String externalConfigFilePath;

    private static final List<DiscordBot> bots = new ArrayList<>();
    private static JDA4SpringMain instance;
    private final Map<String, Object> botTaskBeans;

    /**
     * Constructor for initializing JDA4Spring. This bean is responsible for:
     * - Discovering bot configurations from Spring's {@link ConfigurableEnvironment} and an optional external file.
     * - Creating and configuring JDA instances for each discovered bot.
     * - Mapping bot tasks (classes annotated with {@link BotTask}) to their respective bots.
     *
     * @param appContext  The Spring Boot application context.
     * @param environment Spring's configurable environment, providing access to application properties.
     */
    public JDA4SpringMain(
            ApplicationContext appContext,
            ConfigurableEnvironment environment) {

        logger.info("JDA4Spring initialization started...");

        JDA4SpringMain.instance = this;
        this.appContext = appContext;
        this.environment = environment;
        this.botTaskBeans = appContext.getBeansWithAnnotation(BotTask.class);
        bots.clear();

        try {
            for (Map.Entry<String, Map<String, String>> entry : getBotConfigs().entrySet()) {
                String botName = entry.getKey();
                Map<String, String> config = entry.getValue();

                String apiToken = config.get("token");
                if (apiToken == null) {
                    throw new IllegalArgumentException("API token not found for bot: '" + botName + "'");
                }
                if (apiToken.trim().isEmpty()) {
                    logger.warn("API token for bot '{}' is empty. Skipping initialization for this bot.", botName);
                    continue;
                }

                bots.add(new DiscordBot(apiToken, getBotTasks(config), getActivity(botName, config), getGatewayIntents(config)));
            }

        } catch (LoginException e) {
            logger.error("Failed to log into a Discord bot account. Please check API tokens.", e);
            System.exit(-1);
        } catch (InterruptedException e) {
            logger.error("JDA initialization interrupted.", e);
            Thread.currentThread().interrupt();
            System.exit(-1);
        } catch (IllegalArgumentException e) {
            logger.error("Configuration Error: {}", e.getMessage(), e);
            System.exit(-1);
        }
    }

    /**
     * @return the Discord bot accounts that were started from the discovered configuration
     */
    public static List<DiscordBot> getBots() {
        return bots;
    }

    /**
     * @return the most recently constructed {@code JDA4SpringMain} bean
     */
    public static JDA4SpringMain getInstance() {
        return instance;
    }

    /**
     * @return all beans annotated with {@link BotTask}, keyed by bean name
     */
    public Map<String, Object> getBotTaskBeans() {
        return botTaskBeans;
    }

    /**
     * Consolidates bot configuration data from Spring's {@link ConfigurableEnvironment}
     * and an optional external configuration file. Properties from the external
     * file take precedence and override those from the environment if keys clash.
     *
     * @return the configuration of each discovered bot, keyed by bot name, then by config type
     */
    private Map<String, Map<String, String>> getBotConfigs() {
        // LinkedHashMap keeps the order the properties were defined in
        Map<String, Map<String, String>> configs = new LinkedHashMap<>();

        // 1. Load properties from Spring's ConfigurableEnvironment
        // Iterate through all property sources known to Spring (application.*, jda4spring.* etc.)
        environment.getPropertySources().forEach(ps -> {
            if (ps instanceof EnumerablePropertySource<?> eps) {
                for (String key : eps.getPropertyNames()) {
                    if (key.startsWith("bots.")) {
                        // read back via the environment so placeholders get resolved
                        put(configs, key, environment.getProperty(key));
                    }
                }
            }
        });

        // 2. Load and override/supplement with properties from the external config file if specified
        if (externalConfigFilePath != null && !externalConfigFilePath.isBlank()) {
            loadExternalConfig(externalConfigFilePath.trim(), configs);
        }

        if (configs.isEmpty()) {
            logger.warn("No bot configurations found. Please ensure your 'bots.*' properties are correctly defined in application.properties, jda4spring.properties/yml/yaml, or your specified jda4spring.configfile.");
        }
        return configs;
    }

    /**
     * Records a single {@code bots.NAME.TYPE = value} property. Anything else is ignored.
     *
     * @param configs the map to populate
     * @param key     the raw property key
     * @param value   the raw property value
     */
    private static void put(Map<String, Map<String, String>> configs, String key, String value) {
        if (value == null || !key.startsWith("bots.")) {
            return;
        }
        // limit 3 so a type may itself contain dots, e.g. "activity.listening"
        String[] parts = key.split("\\.", 3);
        if (parts.length < 3) {
            return;
        }
        configs.computeIfAbsent(parts[1], k -> new LinkedHashMap<>()).put(parts[2], value.trim());
    }

    private void loadExternalConfig(String path, Map<String, Map<String, String>> configs) {
        Resource resource = path.startsWith("classpath:")
                ? new ClassPathResource(path.substring("classpath:".length()))
                : new FileSystemResource(path);

        if (!resource.exists()) {
            logger.error("External config file not found: '{}'", path);
            throw new RuntimeException("External config file not found: " + path);
        }

        try {
            Properties props;
            if (path.endsWith(".yml") || path.endsWith(".yaml")) {
                YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
                yaml.setResources(resource);
                props = yaml.getObject();
            } else {
                props = new Properties();
                try (InputStream is = resource.getInputStream()) {
                    props.load(is);
                }
            }
            if (props == null) {
                return;
            }
            for (String key : props.stringPropertyNames()) {
                put(configs, key, environment.resolvePlaceholders(props.getProperty(key)));
            }
        } catch (IOException e) {
            logger.error("Error reading external config file: '{}'", path, e);
            throw new RuntimeException("Error reading external config file: " + path, e);
        }
    }

    /**
     * Resolves the {@link BotTask} beans a bot should listen with.
     *
     * @param config the configuration of a single bot.
     * @return the bot's task beans, or an empty list if none are configured.
     */
    private List<Object> getBotTasks(Map<String, String> config) {
        try {
            String tasksString = config.getOrDefault("tasks", "");
            if (tasksString.trim().isEmpty()) {
                logger.warn("No tasks defined for bot. Make sure to specify 'bots.<botName>.tasks = TaskBeanName1,TaskBeanName2' in your config.");
                return new ArrayList<>();
            }
            return Stream.of(tasksString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(appContext::getBean)
                    .toList();
        } catch (Exception e) {
            logger.error("Error retrieving bot tasks: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves the {@link Activity} for a specific bot from its configuration.
     *
     * @param botName the name of the bot, for logging.
     * @param config  the configuration of a single bot.
     * @return The configured {@link Activity}, or {@code null} if not specified.
     */
    private Activity getActivity(String botName, Map<String, String> config) {
        Map.Entry<String, String> activityConfig = config.entrySet().stream()
                .filter(e -> e.getKey().startsWith("activity")) // Matches activity, activity.playing, activityPlaying etc.
                .findFirst()
                .orElse(null);

        if (activityConfig == null || activityConfig.getValue().trim().isEmpty()) {
            logger.info("No activity set for bot: {}", botName);
            return null;
        }

        String value = activityConfig.getValue();
        // "activity", "activity.listening" and "activityListening" all reduce to their suffix
        String type = activityConfig.getKey().toLowerCase().replaceFirst("^activity[._]?", "");

        try {
            return switch (type) {
                case "playing" -> Activity.playing(value);
                case "listening" -> Activity.listening(value);
                case "watching" -> Activity.watching(value);
                case "competing" -> Activity.competing(value);
                case "" -> Activity.customStatus(value);
                default -> {
                    logger.warn("Unknown activity type '{}' for bot '{}'. Using custom status.", activityConfig.getKey(), botName);
                    yield Activity.customStatus(value);
                }
            };
        } catch (Exception e) {
            // Discord and JDA rejects e.g. names over 128 chars — warn and start the bot without an activity
            logger.warn("Error getting activity for bot {}: {}", botName, e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the list of {@link GatewayIntent}s for a specific bot from its configuration.
     *
     * @param config the configuration of a single bot.
     * @return A list of configured {@link GatewayIntent}s, or default intents if not specified or invalid.
     */
    private List<GatewayIntent> getGatewayIntents(Map<String, String> config) {
        String gatewayIntentsString = config.getOrDefault("intents", "");

        if (gatewayIntentsString.trim().isEmpty()) {
            logger.info("No Gateway Intents defined for bot. Using default intents.");
            return new ArrayList<>(GatewayIntent.DEFAULT);
        }

        try {
            return Stream.of(gatewayIntentsString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toUpperCase().replace("GATEWAYINTENT.", "")) // Normalize input
                    .map(GatewayIntent::valueOf)
                    .toList();
        } catch (Exception e) {
            logger.error("Invalid Gateway Intent specified: {}. Using default intents.", e.getMessage());
            return new ArrayList<>(GatewayIntent.DEFAULT);
        }
    }

    /**
     * Requests the JDA instances associated with a specific event listener class.
     * This allows other parts of the application to get the JDA instance related to their tasks.
     *
     * @param clazz The class of the event listener (BotTask) for which JDA instances are requested.
     * @return A list of {@link JDA} instances that are configured to use the given listener class, empty if none found.
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
            logger.warn("Class {} requested JDA instance(s), but no associated JDA bot was found.", clazz.getName());
        }
        return foundInstances;
    }
}
