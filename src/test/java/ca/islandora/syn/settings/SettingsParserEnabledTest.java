package ca.islandora.syn.settings;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

public class SettingsParserEnabledTest {

    @Test
    public void testConfigDisabledUpper() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"TRUE\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertTrue(settings.getDisabled());
    }

    @Test
    public void testConfigDisabledLower() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"true\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertTrue(settings.getDisabled());
    }

    @Test
    public void testConfigDisabledProper() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"True\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertTrue(settings.getDisabled());
    }

    @Test
    public void testConfigDisabledCrazy() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"TrUe\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertTrue(settings.getDisabled());
    }

    @Test
    public void testConfigEnabledUpper() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"FALSE\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertFalse(settings.getDisabled());
    }

    @Test
    public void testConfigEnabledLower() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"false\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertFalse(settings.getDisabled());
    }

    @Test
    public void testConfigEnabledProper() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"False\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertFalse(settings.getDisabled());
    }


    @Test
    public void testConfigEnabledCrazy() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"FaLsE\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertFalse(settings.getDisabled());
    }


    @Test
    public void testConfigEnabledOther() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"other\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertFalse(settings.getDisabled());
    }


    @Test
    public void testConfigEnabledBlank() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\" disabled=\"\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertFalse(settings.getDisabled());
    }


    @Test
    public void testConfigEnabledMissing() throws Exception {
        final String testXml = String.join("\n"
                , "<config version=\"1\">"
                , "  <site url=\"http://test.com\" algorithm=\"RS384\" path=\"test/path.key\" encoding=\"PEM\"/>"
                , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Config settings = SettingsParser.getSitesObject(stream);
        assertFalse(settings.getDisabled());
    }
}
