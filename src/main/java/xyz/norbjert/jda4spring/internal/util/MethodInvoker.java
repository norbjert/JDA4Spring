package xyz.norbjert.jda4spring.internal.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class for robust method invocation using reflection.
 * Handles method accessibility and standard reflection-related exceptions,
 * providing a centralized way to invoke methods with dynamic arguments.
 */
public class MethodInvoker {
    private static final Logger logger = LoggerFactory.getLogger(MethodInvoker.class);

    private MethodInvoker() {
        logger.error("static class, not to be instanced");
        throw new RuntimeException("SlashCommandDataFactory is a static class and cannot be instanced");
    }

    /**
     * Invokes a given method on a target object with provided arguments.
     * This method attempts to make the method accessible if it's not public
     * and logs/re-throws specific exceptions during invocation.
     *
     * @param method The {@link Method} to be invoked.
     * @param target The object instance on which the method should be invoked.
     * @param args An array of arguments to pass to the method. Can be empty if the method takes no arguments.
     * @throws RuntimeException If the method cannot be accessed, or if an exception occurs during its invocation.
     */
    public static void invoke(Method method, Object target, Object... args) {
        if (!method.trySetAccessible()) {
            logger.error("Method '{}' in class '{}' is not public and Java language access control cannot be suppressed. Cannot invoke.",
                    method.getName(), method.getDeclaringClass().getName());
            throw new RuntimeException("Method " + method.getName() + " is not accessible.");
        }

        try {
            method.invoke(target, args);
        } catch (IllegalAccessException ex) {
            logger.error("IllegalAccessException attempting to invoke method '{}' in class '{}': {}",
                    method.getName(), method.getDeclaringClass().getName(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to access method for invocation.", ex);
        } catch (InvocationTargetException ex) {
            logger.error("Exception thrown by invoked method '{}' in class '{}': {}",
                    method.getName(), method.getDeclaringClass().getName(), ex.getTargetException().getMessage(), ex.getTargetException());
            // Unwrap and re-throw the original exception thrown by the target method
            throw new RuntimeException("Method invocation failed.", ex.getTargetException());
        } catch (IllegalArgumentException ex) {
            logger.error("IllegalArgumentException for method '{}' in class '{}'. Check parameter types and count. Error: {}",
                    method.getName(), method.getDeclaringClass().getName(), ex.getMessage(), ex);
            throw new RuntimeException("Argument mismatch for method invocation.", ex);
        }
    }
}