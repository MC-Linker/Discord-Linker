package me.lianecx.discordlinker.architectury.util;

import me.lianecx.discordlinker.common.util.UrlParser;
import net.minecraft.network.chat.*;

public class URLComponent {

    public static Component buildURLComponent(String message) {
        //? if <1.19 {
        /*MutableComponent root = new TextComponent("");
        *///? } else
        MutableComponent root = Component.empty();

        for(UrlParser.Segment segment : UrlParser.split(message)) {
            if(segment.isUrl()) {
                //? if <1.19 {
                /*root.append(new TextComponent(segment.getContent())
                *///? } else
                root.append(Component.literal(segment.getContent())
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, segment.getContent()))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                //? if <1.19 {
                                /*new TextComponent("Click to open link")
                                *///? } else
                                Component.literal("Click to open link")
                        ))
                    )
                );
            }
            //? if <1.19 {
            /*else root.append(new TextComponent(segment.getContent()));
            *///? } else
            else root.append(Component.literal(segment.getContent()));
        }

        return root;
    }
}
