package me.lianecx.discordlinker.common.network.protocol.payloads;

import java.util.Arrays;

public class InvalidPayloadException extends RuntimeException {

    public InvalidPayloadException(Object[] objects) {
        super("Invalid payload: " + Arrays.toString(objects));
    }
}
