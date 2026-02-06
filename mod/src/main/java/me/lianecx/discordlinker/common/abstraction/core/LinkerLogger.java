package me.lianecx.discordlinker.common.abstraction.core;

public abstract class LinkerLogger {
    protected boolean debug;

    public void setDebug(boolean enabled) {
        this.debug = enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public abstract void info(String message);

    public abstract void warn(String message);

    public abstract void error(String message);

    public abstract void debug(String message);
}
