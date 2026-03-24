package me.lianecx.discordlinker.spigot.util;

import me.lianecx.discordlinker.common.util.UrlParser;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class URLComponent {

    /**
     * Builds a clickable component from a raw message.
     */
    public static BaseComponent[] buildURLComponent(String message) {
        List<UrlParser.Segment> segments = UrlParser.split(message);
        List<BaseComponent> components = new ArrayList<>();

        for (UrlParser.Segment segment : segments) {
            if (segment.isUrl()) {
                TextComponent url = new TextComponent(segment.getContent());
                url.setColor(ChatColor.BLUE);
                url.setUnderlined(true);
                url.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, segment.getURL()));
                url.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent("Click to open link")}));
                components.add(url);
            } else {
                components.add(new TextComponent(segment.getContent()));
            }
        }

        return components.toArray(new BaseComponent[0]);
    }
}
