package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import me.lianecx.discordlinker.common.events.ChatsMinecraftEvent;
import me.lianecx.discordlinker.common.events.data.ChatEventData;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class ChatLoadCommand implements LinkerMinecraftCompletableCommand {

    public static boolean ENABLED = true;

    private static final Object LOCK = new Object();
    private static final int TICKS_PER_SECOND = 20;
    private static final String CHATLOAD_PERMISSION = "discordlinker.chatload";
    private static final List<String> MESSAGE_SUGGESTIONS = Arrays.asList("50", "100", "250", "500", "1000");
    private static final List<String> DURATION_SUGGESTIONS = Arrays.asList("5", "10", "30", "60", "120");
    private static final List<String> TYPE_SUGGESTIONS = Stream.of(ConnJson.ChatChannel.ChatChannelType.values())
            .map(type -> type.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.toList());
    private static final String AVAILABLE_TYPES = Stream.of(ConnJson.ChatChannel.ChatChannelType.values())
            .map(type -> type.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(", "));

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

        if(args.length != 2 && args.length != 3) {
            sender.sendMessage(MinecraftChatColor.RED + "Usage: /chatload <messages> <duration> [type]");
            return;
        }

        int messages;
        int durationSeconds;
        try {
            messages = Integer.parseInt(args[0]);
            durationSeconds = Integer.parseInt(args[1]);
        }
        catch(NumberFormatException ex) {
            sender.sendMessage(MinecraftChatColor.RED + "Please provide valid integers. Usage: /chatload <messages> <duration> [type]");
            return;
        }

        ConnJson.ChatChannel.ChatChannelType chatType = parseType(args.length == 3 ? args[2] : null);
        if(chatType == null) {
            sender.sendMessage(MinecraftChatColor.RED + "Unknown type. Available: " + AVAILABLE_TYPES);
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

        if(!isChatBridgeReady(chatType)) {
            sender.sendMessage(MinecraftChatColor.RED + "Chat bridge is not active for type '" + chatType.name().toLowerCase(Locale.ROOT) + "'.");
            return;
        }

        synchronized(LOCK) {
            if(activeRun != null) {
                sender.sendMessage(MinecraftChatColor.RED + "A chat load test is already running.");
                return;
            }

            ActiveRun run = new ActiveRun(messages, durationSeconds, chatType);
            activeRun = run;
            run.task = getScheduler().runRepeatingSync(() -> tick(run), 0, 1);
        }

        sender.sendMessage(MinecraftChatColor.GREEN + "Started chat load test: " + messages + " messages over " + durationSeconds + " seconds as type " + chatType.name().toLowerCase(Locale.ROOT) + ".");
        getLogger().info(String.format(Locale.ROOT,
                "[ChatLoad] Started: %d messages over %ds as type '%s' (target %.2f msg/s).",
                messages, durationSeconds, chatType.name().toLowerCase(Locale.ROOT), (double) messages / durationSeconds
        ));
    }

    public static void shutdown() {
        stopActiveRun("shutdown");
    }

    @Override
    public List<String> complete(LinkerCommandSender sender, String[] args) {
        if(!sender.hasPermission(4, CHATLOAD_PERMISSION)) return new ArrayList<>();
        if(args.length == 0) return MESSAGE_SUGGESTIONS;
        if(args.length == 1) return filterByPrefix(MESSAGE_SUGGESTIONS, args[0]);
        if(args.length == 2) return filterByPrefix(DURATION_SUGGESTIONS, args[1]);
        if(args.length == 3) return filterByPrefix(TYPE_SUGGESTIONS, args[2].toLowerCase(Locale.ROOT));
        return new ArrayList<>();
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
        LinkerPlayer player = run.players[playerIndex];
        String sender = senderForType(run.chatType, player.getName());
        String message = generateRandomMessage(run.chatType, player.getName());

        if(run.chatType == ConnJson.ChatChannel.ChatChannelType.CHAT)
            getMinecraftEventBus().emit(new ChatEventData(message, player));
        else
            ChatsMinecraftEvent.sendChatAsync(message, run.chatType, sender);
    }

    private static String generateRandomMessage(ConnJson.ChatChannel.ChatChannelType type, String playerName) {
        switch(type) {
            case START:
            case CLOSE:
                return "";
            case JOIN:
                return playerName + " joined the game";
            case QUIT:
                return playerName + " left the game";
            case DEATH:
                return playerName + " fell from a high place";
            case ADVANCEMENT:
                return "minecraft:story/mine_diamond";
            case PLAYER_COMMAND:
                return "/home";
            case CONSOLE_COMMAND:
            case BLOCK_COMMAND:
                return "say chatload test command";
            case CONSOLE:
                return "[Server thread/INFO]: synthetic log line from /chatload\n";
            case CHAT:
            default:
                return generateRandomChatSentence();
        }
    }

    private static String generateRandomChatSentence() {
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

    private static String senderForType(ConnJson.ChatChannel.ChatChannelType type, String playerName) {
        switch(type) {
            case CONSOLE:
            case CONSOLE_COMMAND:
                return "Server";
            case BLOCK_COMMAND:
                return "CommandBlock";
            case START:
            case CLOSE:
                return null;
            default:
                return playerName;
        }
    }

    private static ConnJson.ChatChannel.ChatChannelType parseType(String raw) {
        if(raw == null || raw.isEmpty()) return ConnJson.ChatChannel.ChatChannelType.CHAT;
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return ConnJson.ChatChannel.ChatChannelType.valueOf(normalized);
        }
        catch(IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isChatBridgeReady(ConnJson.ChatChannel.ChatChannelType type) {
        ConnJson conn = getConnJson();
        return getClientManager().isConnected() &&
                conn != null &&
                conn.hasChatChannelType(type);
    }

    private static List<String> filterByPrefix(List<String> source, String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(value -> value.startsWith(normalized))
                .collect(Collectors.toList());
    }

    private static final class ActiveRun {
        private final int totalMessages;
        private final int totalTicks;
        private final long startedAtNanos;
        private final LinkerPlayer[] players;
        private final ConnJson.ChatChannel.ChatChannelType chatType;

        private LinkerScheduler.LinkerSchedulerRepeatingTask task;
        private int tick;
        private int sent;

        private ActiveRun(int totalMessages, int durationSeconds, ConnJson.ChatChannel.ChatChannelType chatType) {
            this.totalMessages = totalMessages;
            this.totalTicks = durationSeconds * TICKS_PER_SECOND;
            this.startedAtNanos = System.nanoTime();
            this.chatType = chatType;
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
