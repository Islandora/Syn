package ca.islandora.syn.settings;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

public class SettingsParserDigestTest {

    @Test
    public void testOneSitePath() throws Exception {
        final String testXml = String.join("\n"
            , "<config version=\"12\">"
            , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
            , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertEquals(12, settings.getVersion());
        assertEquals(1, settings.getSites().size());

        final Site site = settings.getSites().get(0);
        assertEquals("RS384", site.getAlgorithm());
        assertEquals("http://test.com", site.getUrl());
        assertEquals("test/path.key", site.getPath());
        assertEquals("PEM", site.getEncoding());
        assertEquals("", site.getKey());
        assertFalse(site.getDefault());
    }

    @Test
    public void testOneSiteKey() throws Exception {
        final String testXml = String.join("\n"
                , "<config>"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" encoding=\"PEM\" default=\"true\">"
                , "multiline"
                , "key"
                , "  </site>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertEquals(-1, settings.getVersion());
        assertEquals(1, settings.getSites().size());

        final Site site = settings.getSites().get(0);
        assertEquals("RS384", site.getAlgorithm());
        assertEquals("http://test.com", site.getUrl());
        assertNull(site.getPath());
        assertEquals("PEM", site.getEncoding());
        assertEquals("multiline\nkey", site.getKey());
        assertTrue(site.getDefault());
    }

    @Test
    public void testTwoSites() throws Exception {
        final String testXml = String.join("\n"
                , "<config>"
                , "  <site/>"
                , "  <site/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertEquals(2, settings.getSites().size());
    }

    @Test
    public void testOneSiteUnexpectedAttribute() throws Exception {
        final String testXml = String.join("\n"
                , "<config>"
                , "  <site unexpected=\"woh\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
    }

    @Test
    public void testOneSiteUnexpectedTag() throws Exception {
        final String testXml = String.join("\n"
                , "<config>"
                , "  <islandora/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
    }

    @Test
    public void testValidAnonymousTrue() throws Exception {
        final String testXml = "<config>\n" +
            "  <site url=\"http://test.com\" algorithm=\"RS384\" encoding=\"PEM\" default=\"true\" " +
            "anonymous=\"true\" />\n" +
            "</config>";

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        final Site sites = settings.getSites().get(0);
        assertTrue("Did not set anonymous property", sites.getAnonymous());
    }

    @Test
    public void testValidAnonymousFalse() throws Exception {
        final String testXml = "<config>\n" +
            "  <site url=\"http://test.com\" algorithm=\"RS384\" encoding=\"PEM\" default=\"true\" " +
            "anonymous=\"false\" />\n" +
            "</config>";

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        final Site sites = settings.getSites().get(0);
        assertFalse("Did not set anonymous property", sites.getAnonymous());
    }

    @Test
    public void testInvalidAnonymous() throws Exception {
        final String testXml = "<config>\n" +
            "  <site url=\"http://test.com\" algorithm=\"RS384\" encoding=\"PEM\" default=\"true\" " +
            "anonymous=\"whatever\" />\n" +
            "</config>";

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        final Site sites = settings.getSites().get(0);
        assertFalse("Did not set anonymous property", sites.getAnonymous());
    }
}
