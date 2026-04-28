package me.lianecx.discordlinker.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlParser {

    private static final String URL_BODY = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+";

    public static final String URL_REGEX = "(" + URL_BODY + ")";
    public static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String MD_URL_REGEX = "\\[([^]]+)\\]\\(" + URL_REGEX + "\\)";

    public static final String URL_OR_MD_URL_REGEX = MD_URL_REGEX + "|" + URL_REGEX;

    public static final Pattern URL_OR_MD_URL_PATTERN = Pattern.compile(URL_OR_MD_URL_REGEX, Pattern.CASE_INSENSITIVE);

    private UrlParser() {}

    public static List<Segment> split(String message) {
        List<Segment> segments = new ArrayList<>();

        Matcher matcher = URL_OR_MD_URL_PATTERN.matcher(message);

        int lastEnd = 0;
        while(matcher.find()) {
            if(matcher.start() > lastEnd) segments.add(Segment.text(message.substring(lastEnd, matcher.start())));

            String markdownLabel = matcher.group(1);
            String markdownUrl = matcher.group(2);
            String plainUrl = matcher.group(3);

            if(markdownUrl != null) segments.add(Segment.url(markdownLabel, markdownUrl));
            else if(plainUrl != null) segments.add(Segment.url(plainUrl));

            lastEnd = matcher.end();
        }

        if(lastEnd < message.length()) segments.add(Segment.text(message.substring(lastEnd)));

        return segments;
    }

    public static class Segment {
        private final String content;
        private final String url;
        private final boolean isUrl;

        private Segment(String content, String url, boolean isUrl) {
            this.content = content;
            this.url = url;
            this.isUrl = isUrl;
        }

        public static Segment text(String text) {
            return new Segment(text, null, false);
        }

        public static Segment url(String url) {
            return new Segment(url, url, true);
        }

        public static Segment url(String text, String targetUrl) {
            return new Segment(text, targetUrl, true);
        }

        public String getContent() {
            return content;
        }

        public String getURL() {
            return url;
        }

        public boolean isUrl() {
            return isUrl;
        }
    }
}
