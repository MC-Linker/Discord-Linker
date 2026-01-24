package me.lianecx.discordlinker.common.network.protocol.payloads;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.util.JsonUtil;
import org.jetbrains.annotations.Contract;

public class SyncedRolePayload implements DiscordEventPayload {
    public final ConnJson.SyncedRole role;

    public SyncedRolePayload(ConnJson.SyncedRole role) {
        this.role = role;
    }

    @Contract("_ -> new")
    public static SyncedRolePayload decode(Object[] objects) throws InvalidPayloadException {
        JsonObject payload = JsonUtil.getJsonObjectFromObjects(objects);
        if(payload == null) throw new InvalidPayloadException(objects);

        ConnJson.SyncedRole role = JsonUtil.fromJson(payload, ConnJson.SyncedRole.class);
        if(role == null) throw new InvalidPayloadException(objects);
        return new SyncedRolePayload(role);
    }
}
