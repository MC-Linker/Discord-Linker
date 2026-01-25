package me.lianecx.discordlinker.spigot.util;

import me.lianecx.discordlinker.common.util.UrlParser;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;

import java.awt.*;
import java.util.List;

public class URLComponent {

    /**
     * Builds a clickable component from a raw message.
     */
    public static BaseComponent[] buildURLComponent(String message) {
        List<UrlParser.Segment> segments = UrlParser.split(message);
        ComponentBuilder builder = new ComponentBuilder("");

        for (UrlParser.Segment segment : segments) {
            if (segment.isUrl()) {
                TextComponent url = new TextComponent(segment.getContent());
                url.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, segment.getContent()));
                url.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to open link").create()));
                builder.append(url);
            } else builder.append(new TextComponent(segment.getContent()));
        }

        return builder.create();
    }
}
