package ca.islandora.syn.settings;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.Map;

import org.junit.Test;

public class SettingsParserAnonymousTest {

    @Test
    public void testSiteAnonymousOn() throws Exception {
        final String testYaml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: true",
                "  key: test data");

        try (final StringReader stream = new StringReader(testYaml)) {
            final Map<String, Boolean> anonymous = SettingsParser.create(stream).getSiteAllowAnonymous();
            assertEquals(1, anonymous.size());
            assertEquals(true, anonymous.containsKey("http://test.com"));
            assertEquals(true, anonymous.get("http://test.com"));
        }
    }

    @Test
    public void testSiteMultipleAnonymousTest() throws Exception {
        final String testYaml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: true",
                "  key: test data",
                "site:",
                "  url: http://test2.com",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: false",
                "  key: test data");

        try (final StringReader stream = new StringReader(testYaml)) {
            final Map<String, Boolean> anonymous = SettingsParser.create(stream).getSiteAllowAnonymous();
            assertEquals(2, anonymous.size());
            assertEquals(true, anonymous.containsKey("http://test.com"));
            assertEquals(true, anonymous.get("http://test.com"));
            assertEquals(true, anonymous.containsKey("http://test2.com"));
            assertEquals(false, anonymous.get("http://test2.com"));
        }
    }

    @Test
    public void testDefaultMultipleAnonymousTest() throws Exception {
        final String testYaml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: true",
                "  key: test data",
                "site:",
                "  url: http://test2.com",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: false",
                "  key: test data",
                "site:",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: true",
                "  default: true");

        try (final StringReader stream = new StringReader(testYaml)) {
            final Map<String, Boolean> anonymous = SettingsParser.create(stream).getSiteAllowAnonymous();
            assertEquals(3, anonymous.size());
            assertEquals(true, anonymous.containsKey("http://test.com"));
            assertEquals(true, anonymous.get("http://test.com"));
            assertEquals(true, anonymous.containsKey("http://test2.com"));
            assertEquals(false, anonymous.get("http://test2.com"));
            assertEquals(true, anonymous.containsKey("default"));
            assertEquals(true, anonymous.get("default"));
        }
    }
}
