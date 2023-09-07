package me.lianecx.discordlinker.utilities;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.ArrayList;
import java.util.List;

@Plugin(name = "Log4JAppender", category = "Core", elementType = "appender", printObject = true)
public class ConsoleLogger extends AbstractAppender {
    private boolean isLogging;
    private final List<String> loggedData;

    public ConsoleLogger() {
        super("SMP-Plugin", null, PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss} %level]: %msg").build(), true, null);

        isLogging = false;
        loggedData = new ArrayList<>();
    }

    public void startLogging() {
        isLogging = true;
    }

    public void stopLogging() {
        isLogging = false;
    }

    public void clearData() {
        loggedData.clear();
    }

    public List<String> getData() {
        return new ArrayList<>(loggedData);
    }

    public void log(String message) {
        if(isLogging) loggedData.add(message);
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public void append(LogEvent event) {
        log(event.getMessage().getFormattedMessage());
    }
}