package ca.islandora.syn.settings;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SettingsParserTokenTest {
    @Test
    public void testInvalidVersion() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='2'>"
                , "  <token>"
                , "   c00lpazzward"
                , "  </token>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Token> tokens = SettingsParser.getSiteStaticTokens(stream);
        assertEquals(0, tokens.size());
    }

    @Test
    public void testTokenNoParams() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <token>"
                , "   c00lpazzward"
                , "  </token>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Token> tokens = SettingsParser.getSiteStaticTokens(stream);
        final Token token = tokens.get("c00lpazzward");
        assertEquals(1, tokens.size());
        assertEquals("c00lpazzward", token.getToken());
        assertEquals("islandoraAdmin", token.getUser());
        assertEquals(0, token.getRoles().size());
    }

    @Test
    public void testTokenUser() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <token user='denis'>"
                , "   c00lpazzward"
                , "  </token>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Token> tokens = SettingsParser.getSiteStaticTokens(stream);
        final Token token = tokens.get("c00lpazzward");
        assertEquals(1, tokens.size());
        assertEquals("denis", token.getUser());
    }

    @Test
    public void testTokenRole() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <token roles='role1,role2,role3'>"
                , "   c00lpazzward"
                , "  </token>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Token> tokens = SettingsParser.getSiteStaticTokens(stream);
        final Token token = tokens.get("c00lpazzward");
        assertEquals(1, tokens.size());
        assertEquals(3, token.getRoles().size());
        assertTrue(token.getRoles().contains("role1"));
        assertTrue(token.getRoles().contains("role2"));
        assertTrue(token.getRoles().contains("role3"));
    }
}
