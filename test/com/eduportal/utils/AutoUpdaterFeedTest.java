package com.eduportal.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
        assertUpdateAssetIsSelected();
        expectIOException("<feed></feed>");
        expectIOException("<feed><entry><title>Release notes</title></entry></feed>");
        System.out.println("Release-feed parsing tests passed.");
    }

    private static void assertUpdateAssetIsSelected() throws Exception {
        String release = "{\"assets\":["
                + "{\"name\":\"EduPortal-update.zip\",\"browser_download_url\":\"https://example.test/old.zip\"},"
                + "{\"name\":\"SMS-EduPortal.exe\",\"uploader\":{\"profile\":{\"id\":12}},"
                + "\"browser_download_url\":\"https://example.test/SMS-EduPortal.exe\","
                + "\"digest\":\"sha256:0123456789abcdef\"}]}";
        Method findAsset = AutoUpdater.class.getDeclaredMethod("findUpdateAsset", String.class);
        findAsset.setAccessible(true);
        Object asset = findAsset.invoke(null, release);
        if (asset == null) throw new AssertionError("The executable release asset was not found.");

        Field downloadUrl = asset.getClass().getDeclaredField("downloadUrl");
        Field sha256 = asset.getClass().getDeclaredField("sha256");
        downloadUrl.setAccessible(true);
        sha256.setAccessible(true);
        assertEquals("https://example.test/SMS-EduPortal.exe", (String) downloadUrl.get(asset));
        assertEquals("sha256:0123456789abcdef", (String) sha256.get(asset));
    }

    private static void expectIOException(String feed) throws Exception {
        boolean rejected = false;
        try {
            AutoUpdater.latestReleaseTagFromFeed(feed);
        } catch (IOException expected) {
            rejected = true;
        }
        if (!rejected) {
            throw new AssertionError("Expected malformed feed to be rejected.");
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
