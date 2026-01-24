package me.lianecx.discordlinker.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class URLEncoderUtil {

    public static String encodeURL(String input) {
        try {
            return URLEncoder.encode(input, "utf-8");
        }
        catch(UnsupportedEncodingException e) {
            // Never happens?
            return null;
        }
    }

    public static String decodeURL(String input) {
        try {
            return URLDecoder.decode(input, "utf-8");
        }
        catch(UnsupportedEncodingException e) {
            // Never happens?
            return null;
        }
    }
}
