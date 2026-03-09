package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import me.lianecx.discordlinker.common.events.data.ChatEventData;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class ChatLoadCommand implements LinkerMinecraftCommand {

    public static boolean ENABLED = true;

    private static final Object LOCK = new Object();
    private static final int TICKS_PER_SECOND = 20;
    private static final String CHATLOAD_PERMISSION = "discordlinker.chatload";

    private static final String[] FAKE_PLAYER_NAMES = {
            "AlexBuilder", "MinerMax", "SkyCrafter", "RedstoneRex",
            "PixelKnight", "FarmFox", "QuartzQueen", "EnderPilot"
    };

    private static final String[] OPENERS = {
            "just", "finally", "quick update", "heads up", "btw"
    };
    private static final String[] ACTIONS = {
            "found", "finished", "fixed", "moved", "sorted", "crafted", "mined", "upgraded"
    };
    private static final String[] OBJECTS = {
            "a stack of diamonds", "the iron farm", "the nether tunnel",
            "our villager hall", "the storage room", "a new beacon",
            "the mob grinder", "the wheat fields"
    };
    private static final String[] LOCATIONS = {
            "near spawn", "in the nether", "under the base",
            "by the village", "next to the portal", "at the mines",
            "in the end", "behind the mountain"
    };
    private static final String[] REQUESTS = {
            "need a hand with obsidian?", "anyone trading rockets?",
            "who wants to run the dragon again?", "can someone sleep?",
            "is anyone selling emeralds?", "got spare blaze rods?",
            "anyone got extra food?", "can someone check the mob farm?"
    };
    private static final String[] SHORT_UPDATES = {
            "server feels smooth now",
            "rain just started again",
            "chunks are loading fine",
            "xp farm is packed",
            "sorting system is fast today",
            "shops are open at spawn",
            "crops are ready to harvest",
            "portal link is fixed now"
    };

    private static ActiveRun activeRun;

    @Override
    public void execute(LinkerCommandSender sender, String[] args) {
        if(!ENABLED) {
            sender.sendMessage(MinecraftChatColor.YELLOW + "Chat load testing is disabled.");
            return;
        }

        if(!sender.hasPermission(4, CHATLOAD_PERMISSION)) {
            sender.sendMessage(MinecraftChatColor.RED + "You do not have permission to use this command!");
            return;
        }

        if(args.length != 2) {
            sender.sendMessage(MinecraftChatColor.RED + "Usage: /chatload <messages> <duration>");
            return;
        }

        int messages;
        int durationSeconds;
        try {
            messages = Integer.parseInt(args[0]);
            durationSeconds = Integer.parseInt(args[1]);
        }
        catch(NumberFormatException ex) {
            sender.sendMessage(MinecraftChatColor.RED + "Please provide valid integers. Usage: /chatload <messages> <duration>");
            return;
        }

        if(messages < 1 || durationSeconds < 1) {
            sender.sendMessage(MinecraftChatColor.RED + "Both <messages> and <duration> must be at least 1.");
            return;
        }

        if(durationSeconds > Integer.MAX_VALUE / TICKS_PER_SECOND) {
            sender.sendMessage(MinecraftChatColor.RED + "Duration is too large.");
            return;
        }

        if(!isChatBridgeReady()) {
            sender.sendMessage(MinecraftChatColor.RED + "Chat bridge is not active. Connect the plugin and configure a chat channel first.");
            return;
        }

        synchronized(LOCK) {
            if(activeRun != null) {
                sender.sendMessage(MinecraftChatColor.RED + "A chat load test is already running.");
                return;
            }

            ActiveRun run = new ActiveRun(messages, durationSeconds);
            activeRun = run;
            run.task = getScheduler().runRepeatingSync(() -> tick(run), 0, 1);
        }

        sender.sendMessage(MinecraftChatColor.GREEN + "Started chat load test: " + messages + " messages over " + durationSeconds + " seconds.");
        getLogger().info(String.format(Locale.ROOT,
                "[ChatLoad] Started: %d messages over %ds (target %.2f msg/s).",
                messages, durationSeconds, (double) messages / durationSeconds
        ));
    }

    public static void shutdown() {
        stopActiveRun("shutdown");
    }

    private static void stopActiveRun(String reason) {
        ActiveRun runToStop;
        synchronized(LOCK) {
            runToStop = activeRun;
            activeRun = null;
        }

        if(runToStop == null) return;
        if(runToStop.task != null) runToStop.task.cancel();

        getLogger().info(String.format(Locale.ROOT,
                "[ChatLoad] Stopped active load test due to %s. Sent %d/%d messages.",
                reason, runToStop.sent, runToStop.totalMessages
        ));
    }

    private static void tick(ActiveRun run) {
        synchronized(LOCK) {
            if(activeRun != run) {
                if(run.task != null) run.task.cancel();
                return;
            }

            try {
                run.tick++;
                int targetSent = (int) (((long) run.totalMessages * run.tick) / run.totalTicks);
                int toSend = targetSent - run.sent;

                for(int i = 0; i < toSend; i++) {
                    emitFakeChatMessage(run);
                    run.sent++;
                }

                if(run.tick % TICKS_PER_SECOND == 0 || run.tick == run.totalTicks) {
                    double elapsedSeconds = Math.max((System.nanoTime() - run.startedAtNanos) / 1_000_000_000d, 0.001d);
                    double currentMps = run.sent / elapsedSeconds;
                    getLogger().info(String.format(Locale.ROOT,
                            "[ChatLoad] Progress: sent %d/%d (%.2f msg/s).",
                            run.sent, run.totalMessages, currentMps
                    ));
                }

                if(run.tick >= run.totalTicks) {
                    finishRun(run);
                }
            }
            catch(Exception ex) {
                getLogger().error("[ChatLoad] Failed while generating messages: " + ex.getMessage());
                finishRun(run);
            }
        }
    }

    private static void finishRun(ActiveRun run) {
        if(activeRun != run) return;

        activeRun = null;
        if(run.task != null) run.task.cancel();

        double elapsedSeconds = Math.max((System.nanoTime() - run.startedAtNanos) / 1_000_000_000d, 0.001d);
        double averageMps = run.sent / elapsedSeconds;
        getLogger().info(String.format(Locale.ROOT,
                "[ChatLoad] Complete: sent %d/%d in %.2fs (avg %.2f msg/s).",
                run.sent, run.totalMessages, elapsedSeconds, averageMps
        ));
    }

    private static void emitFakeChatMessage(ActiveRun run) {
        int playerIndex = run.sent % run.players.length;
        String message = generateRandomMessage();
        getMinecraftEventBus().emit(new ChatEventData(message, run.players[playerIndex]));
    }

    private static String generateRandomMessage() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int variant = random.nextInt(4);
        switch(variant) {
            case 0:
                return OPENERS[random.nextInt(OPENERS.length)] + " " +
                        ACTIONS[random.nextInt(ACTIONS.length)] + " " +
                        OBJECTS[random.nextInt(OBJECTS.length)] + " " +
                        LOCATIONS[random.nextInt(LOCATIONS.length)];
            case 1:
                return REQUESTS[random.nextInt(REQUESTS.length)];
            case 2:
                return SHORT_UPDATES[random.nextInt(SHORT_UPDATES.length)];
            default:
                return "anyone around " + LOCATIONS[random.nextInt(LOCATIONS.length)] + "?";
        }
    }

    private static boolean isChatBridgeReady() {
        ConnJson conn = getConnJson();
        return getClientManager().isConnected() &&
                conn != null &&
                conn.hasChatChannelType(ConnJson.ChatChannel.ChatChannelType.CHAT);
    }

    private static final class ActiveRun {
        private final int totalMessages;
        private final int totalTicks;
        private final long startedAtNanos;
        private final LinkerPlayer[] players;

        private LinkerScheduler.LinkerSchedulerRepeatingTask task;
        private int tick;
        private int sent;

        private ActiveRun(int totalMessages, int durationSeconds) {
            this.totalMessages = totalMessages;
            this.totalTicks = durationSeconds * TICKS_PER_SECOND;
            this.startedAtNanos = System.nanoTime();
            this.players = new LinkerPlayer[FAKE_PLAYER_NAMES.length];
            for(int i = 0; i < FAKE_PLAYER_NAMES.length; i++)
                this.players[i] = new SyntheticPlayer(FAKE_PLAYER_NAMES[i]);
        }
    }

    private static final class SyntheticPlayer extends LinkerPlayer {
        private SyntheticPlayer(String name) {
            super(UUID.nameUUIDFromBytes(("chatload:" + name).getBytes(StandardCharsets.UTF_8)).toString(), name);
        }

        @Override
        public void sendMessageWithClickableURLs(String message) {}

        @Override
        public void kick(String reason) {}

        @Override
        public String getNBTAsString() {
            return "{}";
        }

        @Override
        public void sendMessage(String message) {}

        @Override
        public boolean hasPermission(int defaultLevel, String permission) {
            return false;
        }
    }
}
