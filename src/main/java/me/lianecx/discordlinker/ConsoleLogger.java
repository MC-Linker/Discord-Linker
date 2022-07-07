package me.lianecx.discordlinker;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

public class ConsoleLogger extends AbstractAppender {
    private boolean isLogging;
    private List<String> loggedData;

    public ConsoleLogger() {
        super("SMP-Plugin", null, null, true);

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
        if (isLogging) {
            loggedData.add(message);
        }
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