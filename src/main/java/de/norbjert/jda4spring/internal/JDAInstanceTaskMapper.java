package de.norbjert.jda4spring.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;

import java.util.List;

@Getter
@AllArgsConstructor
public class JDAInstanceTaskMapper {

    private JDA jda;
    private List<Object> botTasks;

}
