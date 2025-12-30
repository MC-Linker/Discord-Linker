package me.lianecx.discordlinker.abstraction;

public interface LinkerCommand {
    /**
     * The name of the command (e.g., "msg" or "discord").
     */
    String getName();

    /**
     * Execute the command.
     *
     * @param sender The source of the command (player or console)
     * @param args   Command arguments
     */
    void execute(LinkerCommandSender sender, String[] args);

    /**
     * Optional: check permission
     */
    default boolean hasPermission(LinkerCommandSender sender) {
        return true;
    }
}
