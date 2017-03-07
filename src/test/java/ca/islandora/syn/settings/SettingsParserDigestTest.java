package ca.islandora.syn.settings;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SettingsParserDigestTest {

    @Test
    public void testOneSitePath() throws Exception {
        final String testXml = String.join("\n"
            , "<sites version=\"12\">"
            , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
            , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Sites settings = SettingsParser.getSitesObject(stream);
        assertEquals(12, settings.getVersion());
        assertEquals(1, settings.getSites().size());

        final Site site = (Site) settings.getSites().get(0);
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
                , "<sites>"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" encoding=\"PEM\" default=\"true\">"
                , "multiline"
                , "key"
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Sites settings = SettingsParser.getSitesObject(stream);
        assertEquals(-1, settings.getVersion());
        assertEquals(1, settings.getSites().size());

        final Site site = (Site) settings.getSites().get(0);
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
                , "<sites>"
                , "  <site/>"
                , "  <site/>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Sites settings = SettingsParser.getSitesObject(stream);
        assertEquals(2, settings.getSites().size());
    }

    @Test
    public void testOneSiteUnexpectedAttribute() throws Exception {
        final String testXml = String.join("\n"
                , "<sites>"
                , "  <site unexpected=\"woh\"/>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Sites settings = SettingsParser.getSitesObject(stream);
    }

    @Test
    public void testOneSiteUnexpectedTag() throws Exception {
        final String testXml = String.join("\n"
                , "<sites>"
                , "  <islandora/>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Sites settings = SettingsParser.getSitesObject(stream);
    }
}
