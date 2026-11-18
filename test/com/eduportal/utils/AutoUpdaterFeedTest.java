package com.eduportal.utils;

import java.io.IOException;

public final class AutoUpdaterFeedTest {
    public static void main(String[] args) throws Exception {
        String feed = "<feed><entry>"
                + "<link rel=\"alternate\" type=\"text/html\" "
                + "href=\"https://github.com/muntasirtahsin/SMS_EduPortal/releases/tag/v1.0.4\"/>"
                + "<title>EduPortal v1.0.4</title>"
                + "</entry><entry>"
                + "<link rel=\"alternate\" type=\"text/html\" "
                + "href=\"https://github.com/muntasirtahsin/SMS_EduPortal/releases/tag/v1.0.3\"/>"
                + "<title>EduPortal v1.0.3</title>"
                + "</entry></feed>";

        assertEquals("v1.0.4", AutoUpdater.latestReleaseTagFromFeed(feed));
        expectIOException("<feed></feed>");
        expectIOException("<feed><entry><title>Release notes</title></entry></feed>");
        System.out.println("Release-feed parsing tests passed.");
    }

    private static void expectIOException(String feed) throws Exception {
        try {
            AutoUpdater.latestReleaseTagFromFeed(feed);
            throw new AssertionError("Expected malformed feed to be rejected.");
        } catch (IOException expected) {
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
