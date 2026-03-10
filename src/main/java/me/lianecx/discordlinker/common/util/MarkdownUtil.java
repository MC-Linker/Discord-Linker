package me.lianecx.discordlinker.common.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.lianecx.discordlinker.common.util.MinecraftChatColor.*;
import static me.lianecx.discordlinker.common.util.UrlParser.URL_PATTERN;

public class MarkdownUtil {

    private static final Pattern FENCED_CODE_BLOCK_PATTERN =
        Pattern.compile("(?s)```([^\\n]*)\\n(.*?)\\n```");

    private static final Pattern HEADING_PATTERN =
        Pattern.compile("^(#{1,3})\\s+(.+)$");

    private static final Pattern QUOTE_PATTERN =
        Pattern.compile("^(>+)\\s?(.*)$");

    private static final String INLINE_CODE_COLOR =
        GRAY.toString() + UNDERLINE;

    private MarkdownUtil() {}

    /* ------------------------------------------------------- */
    /* Entry point                                              */
    /* ------------------------------------------------------- */

    public static String markdownToColorCodes(String markdown) {
        if(markdown == null || markdown.isEmpty()) return "";

        markdown = markdown.replace("\r\n", "\n").replace('\r', '\n');

        Map<String, String> urlMap = new HashMap<>();
        markdown = extractUrls(markdown, urlMap);

        Map<String, String> codeBlockMap = new HashMap<>();
        markdown = extractCodeBlocks(markdown, codeBlockMap);

        markdown = processBlocks(markdown);

        for(Map.Entry<String, String> e : codeBlockMap.entrySet())
            markdown = markdown.replace(e.getKey(), e.getValue());

        for(Map.Entry<String, String> e : urlMap.entrySet())
            markdown = markdown.replace(e.getKey(), e.getValue());

        return markdown;
    }

    /* ------------------------------------------------------- */
    /* URL extraction                                           */
    /* ------------------------------------------------------- */

    private static String extractUrls(String input, Map<String, String> map) {
        Matcher matcher = URL_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        int counter = 0;

        while(matcher.find()) {
            String url = matcher.group();
            String placeholder = "%%URL" + counter++ + "%%";
            map.put(placeholder, url);
            matcher.appendReplacement(sb, placeholder);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /* ------------------------------------------------------- */
    /* Code block extraction                                    */
    /* ------------------------------------------------------- */

    private static String extractCodeBlocks(String input, Map<String, String> map) {
        Matcher matcher = FENCED_CODE_BLOCK_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        int counter = 0;

        while(matcher.find()) {
            String code = matcher.group(2);
            String placeholder = "%%CODEBLOCK" + counter++ + "%%";

            map.put(
                placeholder,
                INLINE_CODE_COLOR + code + RESET
            );

            matcher.appendReplacement(
                sb,
                Matcher.quoteReplacement(placeholder)
            );
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /* ------------------------------------------------------- */
    /* Block processing                                         */
    /* ------------------------------------------------------- */

    private static String processBlocks(String markdown) {
        String[] lines = markdown.split("\\n", -1);
        List<String> rendered = new ArrayList<>();

        boolean multilineQuote = false;

        for(String line : lines) {
            String current = line;

            if(current.startsWith(">>> ")) {
                multilineQuote = true;
                current = current.substring(4);
                rendered.add(GRAY + "| " + parseInline(current) + RESET);
                continue;
            }

            if(multilineQuote) {
                rendered.add(GRAY + "| " + parseInline(current) + RESET);
                continue;
            }

            Matcher headingMatcher = HEADING_PATTERN.matcher(current);
            if(headingMatcher.matches()) {
                String content = headingMatcher.group(2);

                rendered.add(BOLD + parseInline(content) + RESET);

                continue;
            }

            if(current.startsWith("-# ")) {
                rendered.add(
                    DARK_GRAY + ITALIC.toString()
                        + parseInline(current.substring(3))
                        + RESET
                );
                continue;
            }

            Matcher quoteMatcher = QUOTE_PATTERN.matcher(current);
            if(quoteMatcher.matches()) {
                rendered.add(
                    GRAY + "| "
                        + parseInline(quoteMatcher.group(2))
                        + RESET
                );
                continue;
            }

            rendered.add(parseInline(current));
        }

        return String.join("\n", rendered);
    }

    /* ------------------------------------------------------- */
    /* Inline parser                                            */
    /* ------------------------------------------------------- */

    private static String parseInline(String input) {
        List<Token> tokens = tokenize(input);

        StringBuilder out = new StringBuilder(input.length() + 16);
        Deque<MinecraftChatColor> styles = new ArrayDeque<>();

        for(Token token : tokens) {
            if(token.type == TokenType.TEXT) {
                out.append(token.value);
                continue;
            }

            switch(token.value) {
                case "***":
                    toggleStyle(out, styles, BOLD);
                    toggleStyle(out, styles, ITALIC);
                    break;

                case "**":
                    toggleStyle(out, styles, BOLD);
                    break;

                case "*":
                    toggleStyle(out, styles, ITALIC);
                    break;

                case "__":
                    toggleStyle(out, styles, UNDERLINE);
                    break;

                case "~~":
                    toggleStyle(out, styles, STRIKETHROUGH);
                    break;

                case "||":
                    toggleStyle(out, styles, DARK_GRAY);
                    break;

                case "`":
                    out.append(RESET)
                        .append(INLINE_CODE_COLOR)
                        .append(token.extra)
                        .append(RESET);
                    applyStack(out, styles);
                    break;

                default:
                    out.append(token.value);
            }
        }

        if(!styles.isEmpty()) out.append(RESET);
        return out.toString();
    }

    /* ------------------------------------------------------- */
    /* Tokenizer                                                */
    /* ------------------------------------------------------- */

    private static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for(int i = 0; i < input.length(); ) {
            char c = input.charAt(i);

            if(c == '\\' && i + 1 < input.length()) {
                buffer.append(input.charAt(i + 1));
                i += 2;
                continue;
            }

            if("*_~|`".indexOf(c) >= 0) {
                if(buffer.length() > 0) {
                    tokens.add(new Token(TokenType.TEXT, buffer.toString()));
                    buffer.setLength(0);
                }

                int run = countRepeated(input, i, c);

                if(c == '`') {
                    int close = input.indexOf("`", i + 1);
                    if(close != -1) {
                        String code = input.substring(i + 1, close);
                        tokens.add(new Token(TokenType.DELIM, "`", code));
                        i = close + 1;
                        continue;
                    }
                }

                tokens.add(new Token(TokenType.DELIM, input.substring(i, i + run)));

                i += run;
                continue;
            }

            buffer.append(c);
            i++;
        }

        if(buffer.length() > 0) tokens.add(new Token(TokenType.TEXT, buffer.toString()));
        return tokens;
    }

    /* ------------------------------------------------------- */
    /* Style helpers                                            */
    /* ------------------------------------------------------- */

    private static void toggleStyle(StringBuilder out, Deque<MinecraftChatColor> stack, MinecraftChatColor style) {
        if(stack.contains(style)) {
            stack.remove(style);
            out.append(RESET);
            applyStack(out, stack);
        }
        else {
            stack.push(style);
            out.append(style);
        }
    }

    private static void applyStack(StringBuilder out, Deque<MinecraftChatColor> stack) {
        for(MinecraftChatColor style : stack) out.append(style);
    }

    /* ------------------------------------------------------- */
    /* Utilities                                                */
    /* ------------------------------------------------------- */

    private static int countRepeated(String s, int index, char c) {
        int count = 0;
        while(index + count < s.length() && s.charAt(index + count) == c) count++;
        return count;
    }

    /* ------------------------------------------------------- */
    /* Token types                                              */
    /* ------------------------------------------------------- */

    private enum TokenType {
        TEXT,
        DELIM
    }

    private static class Token {
        TokenType type;
        String value;
        String extra;

        Token(TokenType type, String value) {
            this(type, value, null);
        }

        Token(TokenType type, String value, String extra) {
            this.type = type;
            this.value = value;
            this.extra = extra;
        }
    }
}