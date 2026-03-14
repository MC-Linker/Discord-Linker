package me.lianecx.discordlinker.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.lianecx.discordlinker.common.util.UrlParser.URL_PATTERN;

public class MarkdownUtil {

    private static final Pattern FENCED_CODE_BLOCK_PATTERN = Pattern.compile("(?s)```[^\\n]*\\n?(.+?)```");

    private MarkdownUtil() {}

    public static String markdownToColorCodes(String markdown) {
        Map<String, String> extractedMap = new HashMap<>();

        // Extract URLs
        Matcher urlMatcher = URL_PATTERN.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        int counter = 0;

        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            String placeholder = "%%URL" + counter++ + "%%";
            extractedMap.put(placeholder, url);
            urlMatcher.appendReplacement(sb, placeholder);
        }
        urlMatcher.appendTail(sb);

        // Extract fenced code blocks
        Matcher codeMatcher = FENCED_CODE_BLOCK_PATTERN.matcher(sb.toString());
        sb = new StringBuffer();
        counter = 0;

        while (codeMatcher.find()) {
            String codeBlock = "&7&n" + codeMatcher.group(1) + "&r";
            String placeholder = "%%CODE" + counter++ + "%%";
            extractedMap.put(placeholder, codeBlock);
            codeMatcher.appendReplacement(sb, placeholder);
        }
        codeMatcher.appendTail(sb);

        markdown = sb.toString();

        // Run markdown replacements safely

        //Format **bold**
        markdown = markdown.replaceAll("(?s)\\*\\*(.+?)\\*\\*", "&l$1&r");
        //Format __underline__
        markdown = markdown.replaceAll("(?s)__(.+?)__", "&n$1&r");
        //Format *italic* and _italic_
        markdown = markdown.replaceAll("(?s)_(.+?)_|\\*(.+?)\\*", "&o$1$2&r");
        //Format ~~strikethrough~~
        markdown = markdown.replaceAll("(?s)~~(.+?)~~", "&m$1&r");
        //Format ??obfuscated??
        markdown = markdown.replaceAll("(?s)\\?\\?(.+?)\\?\\?", "&k$1&r");
        //Format inline `code` blocks
        markdown = markdown.replaceAll("`(.+?)`", "&7&n$1&r");
        //Format ||spoilers||
        markdown = markdown.replaceAll("(?s)\\|\\|(.+?)\\|\\|", "&8$1&r");
        //Format '> quotes'
        markdown = markdown.replaceAll("(?m)^>+ (.+)", "&7| $1&r");
        //Format '# headers'
        markdown = markdown.replaceAll("(?m)^(#{1,3}) (.+)$", "&l$2&r");
        // Format '-# small header'
        markdown = markdown.replaceAll("(?m)^-# (.+)$", "&o$1&r");

        // Restore URLs and code blocks
        for (Map.Entry<String, String> entry : extractedMap.entrySet()) {
            markdown = markdown.replace(entry.getKey(), entry.getValue());
        }

        return markdown;
    }
}
