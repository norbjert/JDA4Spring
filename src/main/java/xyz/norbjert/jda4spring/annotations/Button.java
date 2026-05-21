package xyz.norbjert.jda4spring.annotations;

import java.lang.annotation.*;

/**
 *  Allows for the creation of embed buttons on a discord bot.
 *  Buttons are currently experimental. If you want to implement the calling logic yourself,
 *  you can use the @ButtonHandler annotation.
 *  When creating a button in an embed, you are required to pass it an ID.
 *  Use that ID in the @Button annotation; once there is an event for that button ID, it will be invoked.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Button {

    /**
     *  the ID of the button
     *  @return the ID of the button
     */
    String value();

}
