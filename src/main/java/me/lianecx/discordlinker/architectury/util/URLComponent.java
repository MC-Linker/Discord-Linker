package me.lianecx.discordlinker.architectury.util;

//? if >=1.21
//import java.net.URI;

import me.lianecx.discordlinker.common.util.UrlParser;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
//? if <1.19
//import net.minecraft.network.chat.TextComponent;


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

                        //? if <1.21 {
                        .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, segment.getURL()))
                            .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                //? if <1.19 {
                                /*new TextComponent("Click to open link")
                                 *///? } else
                                Component.literal("Click to open link")
                            ))
                            .withUnderlined(true)
                        )
                        //? } else {
                        /*.withStyle(style -> style
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(segment.getURL())))
                            .withHoverEvent(new HoverEvent.ShowText(
                                //? if <1.19 {
                                /^new TextComponent("Click to open link")
                                ^///? } else
                                Component.literal("Click to open link")
                            ))
                            .withUnderlined(true)
                        )
                        *///? }
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
