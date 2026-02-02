package me.lianecx.discordlinker.common.network.protocol.payloads;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.util.JsonUtil;
import org.jetbrains.annotations.Contract;

public class SyncedRoleMemberPayload implements DiscordEventPayload {
    public ConnJson.SyncedRole role;
    public String uuid;

    public SyncedRoleMemberPayload(ConnJson.SyncedRole role, String uuid) {
        this.role = role;
        this.uuid = uuid;
    }

    @Contract("_ -> new")
    public static SyncedRoleMemberPayload decode(Object[] objects) throws InvalidPayloadException {
        JsonObject payload = JsonUtil.parseJsonObject(objects);
        if(payload == null) throw new InvalidPayloadException(objects);

        ConnJson.SyncedRole role = JsonUtil.fromJson(payload, ConnJson.SyncedRole.class);
        String uuid = payload.get("uuid").getAsString();
        return new SyncedRoleMemberPayload(role, uuid);
    }
}
