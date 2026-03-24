package me.lianecx.discordlinker.common.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public final class JoinRequirementEvaluator {

    private JoinRequirementEvaluator() {}

    private static final Map<String, Integer> pendingVerifications = new ConcurrentHashMap<>();

    public static void evaluate(String uuid, String username, Consumer<JoinRequirementResult> callback) {
        if(getConnJson() == null || getConnJson().getRequiredRoleToJoin() == null) {
            callback.accept(JoinRequirementResult.allow());
            return;
        }

        getClientManager().hasRequiredRole(uuid, hasRequiredRoleResponse -> {
            switch(hasRequiredRoleResponse) {
                case TRUE:
                    pendingVerifications.remove(uuid);
                    callback.accept(JoinRequirementResult.allow());
                    break;
                case FALSE:
                    callback.accept(JoinRequirementResult.deny(JoinRequirementMessages.MISSING_REQUIRED_ROLE));
                    break;
                case ERROR:
                    callback.accept(JoinRequirementResult.deny(JoinRequirementMessages.ROLE_CHECK_FAILED));
                    break;
                case NOT_CONNECTED:
                    Integer code = pendingVerifications.get(uuid);
                    if(code == null) {
                        code = ThreadLocalRandom.current().nextInt(1000, 10000);
                        pendingVerifications.put(uuid, code);
                        getClientManager().verifyUser(uuid, username, code);
                    }

                    int finalCode = code;
                    getClientManager().getInviteURL(url ->
                            callback.accept(JoinRequirementResult.deny(JoinRequirementMessages.notConnected(finalCode, url)))
                    );

                    // 3 minutes in ticks
                    getScheduler().runDelayedAsync(() -> pendingVerifications.remove(uuid), 20 * 60 * 3);
                    break;
            }
        });
    }

    public static final class JoinRequirementResult {
        private final boolean allowed;
        private final String denyReason;

        private JoinRequirementResult(boolean allowed, String denyReason) {
            this.allowed = allowed;
            this.denyReason = denyReason;
        }

        public static JoinRequirementResult allow() {
            return new JoinRequirementResult(true, null);
        }

        public static JoinRequirementResult deny(String reason) {
            return new JoinRequirementResult(false, reason);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getDenyReason() {
            return denyReason;
        }
    }
}
