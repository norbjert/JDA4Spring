package xyz.norbjert.jda4spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import xyz.norbjert.jda4spring.internal.JDA4SpringMain;

@AutoConfiguration
@Import(JDA4SpringMain.class)
public class JDA4SpringAutoConfiguration {
}
