package com.jd.oxygent.oxybank.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * URL utility class
 * Converted from oxybank/utils/url_util.py
 */
@Slf4j
public class UrlUtil {
    
    /**
     * Ensure URL has a protocol
     * @param url URL string
     * @return URL with protocol
     */
    public static String ensureUrlProtocol(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null) {
                // Default to 'http' protocol
                return new URI("http", uri.getAuthority(), uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            log.warn("Invalid URL format: {}, trying simple http prefix", url, e);
            // If URI parsing fails, try simple http prefix
            return "http://" + url;
        }
    }
}