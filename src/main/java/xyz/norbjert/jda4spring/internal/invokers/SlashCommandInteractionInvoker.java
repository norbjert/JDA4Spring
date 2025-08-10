package xyz.norbjert.jda4spring.internal.invokers;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the invocation of methods annotated with {@code @SlashCommand}.
 * It intelligently resolves method parameters based on the {@link SlashCommandInteractionEvent}
 * and the expected types from Discord's slash command options, allowing for flexible method signatures.
 */
public class SlashCommandInteractionInvoker {

    private static final Logger logger = LoggerFactory.getLogger(SlashCommandInteractionInvoker.class);

    private SlashCommandInteractionInvoker() {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * Invokes the given {@code slashMethod} on the {@code declaringInstance}
     * by intelligently mapping parameters from the {@code event}.
     * This method supports:
     * <ul>
     *     <li>Injecting the {@link SlashCommandInteractionEvent} itself.</li>
     *     <li>Injecting a {@code List<String>} which will contain all string options from the command.</li>
     *     <li>Injecting individual slash command options mapped by their parameter name to the Discord option name.</li>
     * </ul>
     *
     * @param slashMethod The {@link Method} annotated with {@code @SlashCommand}.
     * @param declaringInstance The object instance on which the method is declared.
     * @param event The {@link SlashCommandInteractionEvent} containing the command details and options.
     * @throws RuntimeException if the method invocation fails or a required parameter cannot be resolved/converted.
     */
    public static void invokeSlashMethod(Method slashMethod, Object declaringInstance, SlashCommandInteractionEvent event) {
        Parameter[] methodParameters = slashMethod.getParameters();
        List<Object> args = new ArrayList<>(methodParameters.length);

        for (Parameter parameter : methodParameters) {
            Class<?> paramType = parameter.getType();
            String paramName = parameter.getName(); // Requires compilation with -parameters flag

            if (SlashCommandInteractionEvent.class.isAssignableFrom(paramType)) {
                args.add(event);
            } else if (List.class.isAssignableFrom(paramType)) {
                // Check if it's List<String> specifically (or other List types if needed later)
                if (parameter.getParameterizedType() instanceof ParameterizedType parameterizedType) {
                    if (parameterizedType.getActualTypeArguments().length == 1 &&
                            parameterizedType.getActualTypeArguments()[0].equals(String.class)) {
                        // This is a List<String>, collect all string options
                        List<String> allStringOptions = event.getOptions().stream()
                                .filter(opt -> opt.getType() == OptionType.STRING)
                                .map(OptionMapping::getAsString)
                                .collect(Collectors.toList());
                        args.add(allStringOptions);
                    } else {
                        logger.warn("Unsupported Parameterized List type '{}' for method parameter '{}' in {}.{}(). Injected null.",
                                parameterizedType.getTypeName(), paramName, slashMethod.getDeclaringClass().getName(), slashMethod.getName());
                        args.add(null);
                    }
                } else {
                    logger.warn("Raw List type (without generics) for method parameter '{}' in {}.{}(). Injected null.",
                            paramName, slashMethod.getDeclaringClass().getName(), slashMethod.getName());
                    args.add(null);
                }
            } else {
                // Attempt to resolve based on individual Discord slash command options by name
                OptionMapping option = event.getOption(paramName);

                if (option == null) {
                    // Option not provided by Discord. If the method parameter is primitive, this will cause issues.
                    // For object types, null will be injected.
                    if (paramType.isPrimitive()) {
                        throw new IllegalArgumentException(
                                "Required primitive option '" + paramName + "' not provided for method " +
                                        slashMethod.getDeclaringClass().getName() + "." + slashMethod.getName() + "()."
                        );
                    }
                    logger.warn("Slash command option '{}' not found for method parameter '{}' in {}.{}(). Injected null.",
                            paramName, paramName, slashMethod.getDeclaringClass().getName(), slashMethod.getName());
                    args.add(null);
                } else {
                    Object value = convertOptionMappingValue(option, paramType, slashMethod);
                    args.add(value);
                }
            }
        }
        MethodInvoker.invoke(slashMethod, declaringInstance, args.toArray());
    }

    /**
     * Converts an {@link OptionMapping} value to the specified target Java type.
     * This handles common conversions for Discord's {@link OptionType}s.
     *
     * @param option The {@link OptionMapping} to convert.
     * @param targetType The desired Java {@link Class} of the parameter.
     * @param method The method for which the parameter is being resolved (for logging context).
     * @return The converted value, or throws an {@link IllegalArgumentException} if conversion is not possible.
     */
    private static Object convertOptionMappingValue(OptionMapping option, Class<?> targetType, Method method) {
        try {
            switch (option.getType()) {
                case STRING:
                    return option.getAsString();
                case INTEGER:
                    if (targetType.equals(long.class) || targetType.equals(Long.class)) {
                        return option.getAsLong();
                    } else if (targetType.equals(int.class) || targetType.equals(Integer.class)) {
                        return (int) option.getAsLong(); // JDA stores integer as long
                    }
                    break;
                case BOOLEAN:
                    return option.getAsBoolean();
                case USER:
                    return option.getAsUser();
                case CHANNEL:
                    return option.getAsChannel();
                case ROLE:
                    return option.getAsRole();
                case MENTIONABLE:
                    return option.getAsMentionable();
                case NUMBER: // Double/Float
                    if (targetType.equals(double.class) || targetType.equals(Double.class)) {
                        return option.getAsDouble();
                    } else if (targetType.equals(float.class) || targetType.equals(Float.class)) {
                        return (float) option.getAsDouble();
                    }
                    break;
                case ATTACHMENT:
                    return option.getAsAttachment();
            }
        } catch (Exception e) {
            logger.error("Error converting OptionMapping '{}' (type: {}) to target type '{}' for method {}.{}(). Error: {}",
                    option.getName(), option.getType(), targetType.getName(),
                    method.getDeclaringClass().getName(), method.getName(), e.getMessage(), e);
            throw new IllegalArgumentException("Failed to convert option '" + option.getName() + "' to " + targetType.getName(), e);
        }

        // If we reach here, it means the targetType wasn't matched with the OptionType's typical conversions.
        logger.warn("Unsupported conversion from OptionType '{}' to target type '{}' for option '{}' in method {}.{}().",
                option.getType(), targetType.getName(), option.getName(),
                method.getDeclaringClass().getName(), method.getName());
        throw new IllegalArgumentException(
                "Cannot convert OptionMapping of type " + option.getType() +
                        " to target type " + targetType.getName() + " for option " + option.getName()
        );
    }
}