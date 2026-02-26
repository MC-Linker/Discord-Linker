package me.lianecx.discordlinker.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.lianecx.discordlinker.common.util.UrlParser.URL_PATTERN;

public class MarkdownUtil {

    private MarkdownUtil() {}

    public static String markdownToColorCodes(String markdown) {
        // Extract URLs
        Map<String, String> urlMap = new HashMap<>();
        Matcher urlMatcher = URL_PATTERN.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        int counter = 0;

        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            String placeholder = "%%URL" + counter++ + "%%";
            urlMap.put(placeholder, url);
            urlMatcher.appendReplacement(sb, placeholder);
        }
        urlMatcher.appendTail(sb);

        markdown = sb.toString();

        // Run markdown replacements safely

        //Format **bold**
        markdown = markdown.replaceAll("\\*\\*(.+?)\\*\\*", "&l$1&r");
        //Format __underline__
        markdown = markdown.replaceAll("__(.+?)__", "&n$1&r");
        //Format *italic* and _italic_
        markdown = markdown.replaceAll("_(.+?)_|\\*(.+?)\\*", "&o$1$2&r");
        //Format ~~strikethrough~~
        markdown = markdown.replaceAll("~~(.+?)~~", "&m$1&r");
        //Format ??obfuscated??
        markdown = markdown.replaceAll("\\?\\?(.+?)\\?\\?", "&k$1&r");
        //Format inline and multiline `code` blocks
        markdown = markdown.replaceAll("(?s)```[^\\n]*\\n?(.+)```", "&7&n$1&r");
        markdown = markdown.replaceAll("`(.+?)`", "&7&n$1&r");
        //Format ||spoilers||
        markdown = markdown.replaceAll("\\|\\|(.+?)\\|\\|", "&8$1&r");
        //Format '> quotes'
        markdown = markdown.replaceAll(Pattern.compile("^>+ (.+)", Pattern.MULTILINE).pattern(), "&7| $1&r");

        // Restore URLs
        for (Map.Entry<String, String> entry : urlMap.entrySet()) {
            markdown = markdown.replace(entry.getKey(), entry.getValue());
        }

        return markdown;
    }
}
