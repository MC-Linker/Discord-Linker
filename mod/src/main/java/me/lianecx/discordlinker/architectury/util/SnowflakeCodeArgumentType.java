package me.lianecx.discordlinker.architectury.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

public class SnowflakeCodeArgumentType implements ArgumentType<String> {

    private static final SimpleCommandExceptionType INVALID_FORMAT = new SimpleCommandExceptionType(() -> "Expected <snowflake>:<5-letter-code>");

    public static SnowflakeCodeArgumentType snowflakeCode() {
        return new SnowflakeCodeArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        StringBuilder sb = new StringBuilder();
        while(reader.canRead() && reader.peek() != ' ') {
            sb.append(reader.read());
        }
        String value = sb.toString();

        if(!value.matches("\\d+:[A-Za-z\\d]{5}")) {
            reader.setCursor(start);
            throw INVALID_FORMAT.createWithContext(reader);
        }

        return value;
    }
}
