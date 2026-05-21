package xyz.norbjert.jda4spring.internal;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
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

    @Getter
    private static final List<DiscordBot> bots = new ArrayList<>();
    @Getter
    private final Map<String, Object> botTaskBeans;
    @Getter
    private static JDA4SpringMain instance;

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
            ConfigurableEnvironment environment) { // Corrected type in constructor signature

        logger.info("JDA4Spring initialization started...");

        JDA4SpringMain.instance = this;
        this.appContext = appContext;
        this.environment = environment;
        this.botTaskBeans = appContext.getBeansWithAnnotation(BotTask.class);

        try {
            // Get consolidated bot config data from all sources
            List<BotConfigProperty> botConfigData = getBotConfigData();

            // Group configuration entries by bot name
            Map<String, List<BotConfigProperty>> botsGroupedByName = new HashMap<>();
            for (BotConfigProperty entry : botConfigData) {
                botsGroupedByName.computeIfAbsent(entry.name(), k -> new ArrayList<>()).add(entry);
            }

            // Initialize each bot account
            for (Map.Entry<String, List<BotConfigProperty>> entry : botsGroupedByName.entrySet()) {
                String botName = entry.getKey();
                List<BotConfigProperty> allEntriesForThisBot = entry.getValue();

                String apiToken = allEntriesForThisBot.stream()
                        .filter(t -> "token".equals(t.type())) // Ensure an exact match for "token"
                        .findFirst()
                        .map(BotConfigProperty::value)
                        .orElseThrow(() -> new IllegalArgumentException("API token not found for bot: '" + botName + "'"));

                if (apiToken.trim().isEmpty()) {
                    logger.warn("API token for bot '{}' is empty. Skipping initialization for this bot.", botName);
                    continue;
                }

                List<Object> botTasks = getEventListenersForBotAsBotTasks(allEntriesForThisBot);
                Activity activity = getActivity(allEntriesForThisBot);
                List<GatewayIntent> gatewayIntents = getGatewayIntents(allEntriesForThisBot);

                DiscordBot newDiscordBotAccountInstance = new DiscordBot(apiToken, botTasks, activity, gatewayIntents);
                bots.add(newDiscordBotAccountInstance);
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
     * Consolidates bot configuration data from Spring's {@link ConfigurableEnvironment}
     * and an optional external configuration file. Properties from the external
     * file take precedence and override those from the environment if keys clash.
     *
     * @return A list of {@link BotConfigProperty} representing all bot configurations.
     */
    private List<BotConfigProperty> getBotConfigData() {
        // Use LinkedHashMap to maintain insertion order if properties are defined in a specific order
        Map<String, String> consolidatedRawProperties = new LinkedHashMap<>();
        Set<String> discoveredBotNames = new HashSet<>();

        // 1. Load properties from Spring's ConfigurableEnvironment
        // Iterate through all property sources known to Spring (application.*, jda4spring.* etc.)
        environment.getPropertySources().forEach(ps -> {
            // Check if the property source is enumerable (i.e., we can get its property names)
            if (ps instanceof EnumerablePropertySource<?> eps) {
                for (String key : eps.getPropertyNames()) {
                    if (key.startsWith("bots.")) {
                        // Get value from environment to ensure proper resolution (e.g., placeholders)
                        String value = environment.getProperty(key);
                        if (value != null) { // Include empty values; they might indicate an intention to clear
                            consolidatedRawProperties.put(key, value.trim());
                            // Extract bot name from "bots.NAME.type"
                            String[] parts = key.split("\\.");
                            if (parts.length >= 3) {
                                discoveredBotNames.add(parts[1]);
                            }
                        }
                    }
                }
            }
        });

        // 2. Load and override/supplement with properties from the external config file if specified
        if (externalConfigFilePath != null && !externalConfigFilePath.isBlank()) {
            loadExternalConfig(externalConfigFilePath.trim(), consolidatedRawProperties, discoveredBotNames);
        }

        // 3. Convert consolidated raw properties into BotConfigDataMapper objects
        List<BotConfigProperty> result = getBotConfigProperties(discoveredBotNames, consolidatedRawProperties);

        if (result.isEmpty()) {
            logger.warn("No bot configurations found. Please ensure your 'bots.*' properties are correctly defined in application.properties, jda4spring.properties/yml/yaml, or your specified jda4spring.configfile.");
        }
        return result;
    }

    @NotNull
    private static List<BotConfigProperty> getBotConfigProperties(Set<String> discoveredBotNames, Map<String, String> consolidatedRawProperties) {
        List<BotConfigProperty> result = new ArrayList<>();
        for (String botName : discoveredBotNames) {
            // Collect all properties for this specific bot from the consolidated map
            for (Map.Entry<String, String> entry : consolidatedRawProperties.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("bots." + botName + ".")) {
                    String type = key.substring(("bots." + botName + ".").length());
                    String value = entry.getValue();
                    result.add(new BotConfigProperty(botName, type, value));
                }
            }
        }
        return result;
    }

    private void loadExternalConfig(String path, Map<String, String> targetMap, Set<String> botNames) {
        Resource resource = path.startsWith("classpath:")
                ? new ClassPathResource(path.substring("classpath:".length()))
                : new FileSystemResource(path);

        if (!resource.exists()) {
            logger.error("External config file not found: '{}'", path);
            throw new RuntimeException("External config file not found: " + path);
        }

        try {
            if (path.endsWith(".yml") || path.endsWith(".yaml")) {
                parseYamlResource(resource, targetMap, botNames);
            } else {
                try (InputStream is = resource.getInputStream()) {
                    parsePropertiesFromInputStream(is, targetMap, botNames);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading external config file: '{}'", path, e);
            throw new RuntimeException("Error reading external config file: " + path, e);
        }
    }

    private void parseYamlResource(Resource resource, Map<String, String> targetMap, Set<String> botNames) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(resource);
        Properties props = yaml.getObject();
        if (props == null) return;
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("bots.")) {
                String value = props.getProperty(key);
                if (value != null) {
                    targetMap.put(key, environment.resolvePlaceholders(value).trim());
                    String[] parts = key.split("\\.");
                    if (parts.length >= 3) botNames.add(parts[1]);
                }
            }
        }
    }

    /**
     * Parses key-value pairs from an {@link InputStream} into a target map.
     * Handles simple "key = value" format, line by line.
     *
     * @param is The {@link InputStream} to read from.
     * @param targetMap The map to populate.
     * @param botNames A set to collect discovered bot names.
     */
    private void parsePropertiesFromInputStream(InputStream is, Map<String, String> targetMap, Set<String> botNames) {
        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) { // Specify encoding
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) { // Ignore comments and empty lines
                    continue;
                }
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) { // Check for a valid key-value pair
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();
                    targetMap.put(key, environment.resolvePlaceholders(value));

                    if (key.startsWith("bots.")) {
                        String[] parts = key.split("\\.");
                        if (parts.length >= 3) { // Ensure it's like bots.NAME.TYPE
                            botNames.add(parts[1]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves event listeners for a specific bot as a list of {@link BotTask} beans.
     *
     * @param allEntriesForCurrentBotAccount A list of {@link BotConfigProperty} entries for the current bot.
     * @return A list of {@link Object} instances representing the bot's tasks.
     */
    private List<Object> getEventListenersForBotAsBotTasks(List<BotConfigProperty> allEntriesForCurrentBotAccount) {
        try {
            String tasksString = allEntriesForCurrentBotAccount.stream()
                    .filter(t -> "tasks".equals(t.type()))
                    .findFirst()
                    .map(BotConfigProperty::value)
                    .orElse("");

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
     * @param allEntriesForCurrentBotAccount A list of {@link BotConfigProperty} entries for the current bot.
     * @return The configured {@link Activity}, or {@code null} if not specified.
     */
    private Activity getActivity(List<BotConfigProperty> allEntriesForCurrentBotAccount) {
        try {
            BotConfigProperty activityConfig = allEntriesForCurrentBotAccount.stream()
                    .filter(t -> t.type().startsWith("activity")) // Matches activity, activityPlaying etc.
                    .findFirst()
                    .orElse(null);

            if (activityConfig == null || activityConfig.value().trim().isEmpty()) {
                logger.info("No activity set for bot: {}", allEntriesForCurrentBotAccount.get(0).name());
                return null;
            }

            String activityType = activityConfig.type();
            String activityValue = activityConfig.value();

            if (activityType.endsWith("playing")) {
                return Activity.playing(activityValue);
            } else if (activityType.endsWith("listening")) {
                return Activity.listening(activityValue);
            } else if (activityType.endsWith("watching")) {
                return Activity.watching(activityValue);
            } else if (activityType.endsWith("competing")) {
                return Activity.competing(activityValue);
            } else if (activityType.endsWith("activity")) { // Default activity type if suffix is just "activity"
                return Activity.customStatus(activityValue);
            } else {
                logger.warn("Unknown activity type '{}' for bot '{}'. Using custom status.", activityType, activityConfig.name());
                return Activity.customStatus(activityValue);
            }
        } catch (Exception e) {
            logger.warn("Error getting activity for bot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the list of {@link GatewayIntent}s for a specific bot from its configuration.
     *
     * @param allEntriesForCurrentBotAccount A list of {@link BotConfigProperty} entries for the current bot.
     * @return A list of configured {@link GatewayIntent}s, or default intents if not specified or invalid.
     */
    private List<GatewayIntent> getGatewayIntents(List<BotConfigProperty> allEntriesForCurrentBotAccount) {
        try {
            String gatewayIntentsString = allEntriesForCurrentBotAccount.stream()
                    .filter(t -> "intents".equals(t.type()))
                    .findFirst()
                    .map(BotConfigProperty::value)
                    .orElse("");

            if (gatewayIntentsString.trim().isEmpty()) {
                logger.info("No Gateway Intents defined for bot. Using default intents.");
                return new ArrayList<>(GatewayIntent.DEFAULT);
            }

            return Stream.of(gatewayIntentsString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toUpperCase().replace("GATEWAYINTENT.", "")) // Normalize input
                    .map(GatewayIntent::valueOf)
                    .toList();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid Gateway Intent specified: {}. Using default intents.", e.getMessage());
            return new ArrayList<>(GatewayIntent.DEFAULT);
        } catch (Exception e) {
            logger.error("Error getting gateway intents: {}", e.getMessage());
            return new ArrayList<>(GatewayIntent.DEFAULT);
        }
    }

    /**
     * Requests the JDA instances associated with a specific event listener class.
     * This allows other parts of the application to get the JDA instance related to their tasks.
     *
     * @param clazz The class of the event listener (BotTask) for which JDA instances are requested.
     * @return A list of {@link JDA} instances that are configured to use the given listener class, or {@code null} if none found.
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
            return null;
        }
        return foundInstances;
    }
}
