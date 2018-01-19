package ca.islandora.syn.settings;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.junit.Test;

public class SettingsParserDigestTest {

    @Test
    public void testOneSitePath() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: RS384",
                "  path: test/path.key",
                "  encoding: PEM");

        final StringReader stream = new StringReader(testXml);
        final Config settings = SettingsParser.create(stream).getConfig();
        assertEquals(1, settings.getVersion());
        assertEquals(1, settings.getSites().size());

        final Site site = settings.getSites().get(0);
        assertEquals("RS384", site.getAlgorithm());
        assertEquals("http://test.com", site.getUrl());
        assertEquals("test/path.key", site.getPath());
        assertEquals("PEM", site.getEncoding());
        assertNull(site.getKey());
        assertFalse(site.getDefault());
    }

    @Test
    public void testOneSiteKey() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: RS384",
                "  encoding: PEM",
                "  default: true",
                "  key: |",
                "    multiline",
                "    key");

        final StringReader stream = new StringReader(testXml);
        final Config settings = SettingsParser.create(stream).getConfig();
        assertEquals(1, settings.getVersion());
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
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "site:");

        final StringReader stream = new StringReader(testXml);
        final Config settings = SettingsParser.create(stream).getConfig();
        assertEquals(2, settings.getSites().size());
    }

    @Test(expected = SettingsParserException.class)
    public void testOneSiteUnexpectedAttribute() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  unexpected: woh");

        final StringReader stream = new StringReader(testXml);
        final Config settings = SettingsParser.create(stream).getConfig();
        assertEquals(1, settings.getSites().size());
    }

    @Test
    public void testValidAnonymousTrue() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: RS384",
                "  encoding: PEM",
                "  default: true",
                "  anonymous: true");

        final StringReader stream = new StringReader(testXml);
        final Config settings = SettingsParser.create(stream).getConfig();
        final Site sites = settings.getSites().get(0);
        assertTrue("Did not set anonymous property", sites.getAnonymous());
    }

    @Test
    public void testValidAnonymousFalse() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: RS384",
                "  encoding: PEM",
                "  default: true",
                "  anonymous: false");

        final StringReader stream = new StringReader(testXml);
        final Config settings = SettingsParser.create(stream).getConfig();
        final Site sites = settings.getSites().get(0);
        assertFalse("Did not set anonymous property", sites.getAnonymous());
    }

    @Test(expected = SettingsParserException.class)
    public void testInvalidAnonymous() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: RS384",
                "  encoding: PEM",
                "  default: true",
                "  anonymous: whatever");

        final StringReader stream = new StringReader(testXml);
        final Config settings = SettingsParser.create(stream).getConfig();
        final Site sites = settings.getSites().get(0);
        assertFalse("Did not set anonymous property", sites.getAnonymous());
    }
}
