package me.lianecx.discordlinker.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlParser {

    public static final String URL_REGEX =
            "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)";

    // Simple but solid URL regex
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE);

    private UrlParser() {}

    public static List<Segment> split(String message) {
        List<Segment> segments = new ArrayList<>();

        Matcher matcher = URL_PATTERN.matcher(message);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                segments.add(Segment.text(message.substring(lastEnd, matcher.start())));
            }

            String url = matcher.group(1);
            segments.add(Segment.url(url));

            lastEnd = matcher.end();
        }

        if (lastEnd < message.length()) segments.add(Segment.text(message.substring(lastEnd)));

        return segments;
    }

    public static class Segment {
        private final String content;
        private final boolean isUrl;

        private Segment(String content, boolean isUrl) {
            this.content = content;
            this.isUrl = isUrl;
        }

        public static Segment text(String text) {
            return new Segment(text, false);
        }

        public static Segment url(String url) {
            return new Segment(url, true);
        }

        public String getContent() {
            return content;
        }

        public boolean isUrl() {
            return isUrl;
        }
    }
}
