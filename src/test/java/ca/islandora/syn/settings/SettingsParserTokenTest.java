package ca.islandora.syn.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Map;

import org.junit.Test;

public class SettingsParserTokenTest {
    @Test(expected = SettingsParserException.class)
    public void testInvalidVersion() throws Exception {
        final String testYaml = String.join("\n",
                "---",
                "version: 2",
                "token:",
                "  value: c00lpazzward");

        try (final StringReader stream = new StringReader(testYaml)) {
            SettingsParser.create(stream).getSiteStaticTokens();
        }
    }

    @Test
    public void testTokenNoParams() throws Exception {
        final String testYaml = String.join("\n",
                "---",
                "version: 1",
                "token:",
                "  value: c00lpazzward");

        try (final StringReader stream = new StringReader(testYaml)) {
            final Map<String, Token> tokens = SettingsParser.create(stream).getSiteStaticTokens();
            final Token token = tokens.get("c00lpazzward");
            assertEquals(1, tokens.size());
            assertEquals("c00lpazzward", token.getValue());
            assertEquals("islandoraAdmin", token.getUser());
            assertEquals(0, token.getRoles().size());
        }
    }

    @Test
    public void testTokenUser() throws Exception {
        final String testYaml = String.join("\n",
                "---",
                "version: 1",
                "token:",
                "  user: denis",
                "  value: c00lpazzward");

        try (final StringReader stream = new StringReader(testYaml)) {
            final Map<String, Token> tokens = SettingsParser.create(stream).getSiteStaticTokens();
            final Token token = tokens.get("c00lpazzward");
            assertEquals(1, tokens.size());
            assertEquals("denis", token.getUser());
        }
    }

    @Test
    public void testTokenRole() throws Exception {
        final String testYaml = String.join("\n",
                "---",
                "version: 1",
                "token:",
                "  roles: role1,role2,role3",
                "  value: c00lpazzward");

        try (final StringReader stream = new StringReader(testYaml)) {
            final Map<String, Token> tokens = SettingsParser.create(stream).getSiteStaticTokens();
            final Token token = tokens.get("c00lpazzward");
            assertEquals(1, tokens.size());
            assertEquals(3, token.getRoles().size());
            assertTrue(token.getRoles().contains("role1"));
            assertTrue(token.getRoles().contains("role2"));
            assertTrue(token.getRoles().contains("role3"));
        }
    }
}
