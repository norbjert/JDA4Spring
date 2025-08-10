package xyz.norbjert.jda4spring.internal;

/**
 * a simple record, containing one entry (aka one line) forms the .config file
 * @param name the name set for a bot (f.e. "someUniqueName"),
 * @param type the type of configuration (f.e. "token" or "activity.listening")
 * @param value the assigned value aka right side of the "=", so the actual value set for the configuration (like the api token f.e.)
 */
public record BotConfigProperty(String name, String type, String value){}
