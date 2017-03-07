package ca.islandora.syn.settings;

import com.auth0.jwt.algorithms.Algorithm;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import static org.junit.Assert.assertEquals;

public class SettingsParserAlgorithmsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private void testOneSiteHmacInlineKey(final String algorithm) throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='" + algorithm + "' encoding='plain'>"
                , "   test data"
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(1, algorithms.size());
        assertEquals(true, algorithms.containsKey("http://test.com"));
        assertEquals(algorithm, algorithms.get("http://test.com").getName());
    }

    @Test
    public void testOneSiteAllHmacInlineKey() throws Exception {
        testOneSiteHmacInlineKey("HS256");
        testOneSiteHmacInlineKey("HS384");
        testOneSiteHmacInlineKey("HS512");
    }

    @Test
    public void testInvalidSitesVersion() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='2'>"
                , "  <site url='http://test.com' algorithm='HS384' encoding='plain'>"
                , "   test data"
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testOneSiteHmacBase64() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS256' encoding='base64'>"
                , "   am9uYXRoYW4gaXMgYXdlc29tZQ=="
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(1, algorithms.size());
    }

    @Test
    public void testOneSiteHmacInvalidBase64() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS256' encoding='base64'>"
                , "   this is invalid base64"
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testOneSiteHmacInvalidEncoding() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS256' encoding='badalgorithm'>"
                , "   this is invalid base64"
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    private void testOneSiteHmacFileKey(final String algorithm) throws Exception {
        final File key = temporaryFolder.newFile();
        final String path = key.getAbsolutePath();

        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='" + algorithm + "' encoding='plain' path='" + path + "'/>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(1, algorithms.size());
        assertEquals(true, algorithms.containsKey("http://test.com"));
        assertEquals(algorithm, algorithms.get("http://test.com").getName());
    }

    @Test
    public void testOneSiteAllHmacFileKey() throws Exception {
        testOneSiteHmacFileKey("HS256");
        testOneSiteHmacFileKey("HS384");
        testOneSiteHmacFileKey("HS512");
    }

    @Test
    public void testSiteBothInlineAndPath() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS384' encoding='plain' path='foo'>"
                , "   test data"
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testSiteNeitherInlineAndPath() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS384' encoding='plain'/>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testSiteInvalidPath() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS384' encoding='plain' path='foo'/>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testSiteNoUrlDefault() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site algorithm='HS256' encoding='plain' default='true'>"
                , "   test data"
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(1, algorithms.size());
    }

    private void testOneSiteRsaInlineKey(final String algorithm) throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='" + algorithm + "' encoding='PEM'>"
                , "-----BEGIN PUBLIC KEY-----"
                , "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEVO4MNlZG+iGYhoJd/cBpfMd9"
                , "YnKsntF+zhQs8lCbBabgY8kNoXVIEeOm4WPJ+W53gLDAIg6BNrZqxk9z1TLD6Dmz"
                , "t176OLYkNoTI9LNf6z4wuBenrlQ/H5UnYl6h5QoOdVpNAgEjkDcdTSOE1lqFLIle"
                , "KOT4nEF7MBGyOSP3KQIDAQAB"
                , "-----END PUBLIC KEY-----"
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(1, algorithms.size());
        assertEquals(true, algorithms.containsKey("http://test.com"));
        assertEquals(algorithm, algorithms.get("http://test.com").getName());
    }

    @Test
    public void testOneSiteAllRsaInlineKey() throws Exception {
        testOneSiteRsaInlineKey("RS256");
        testOneSiteRsaInlineKey("RS384");
        testOneSiteRsaInlineKey("RS512");
    }

    private void testOneSiteRsaFileKey(final String algorithm) throws Exception {
        final File keyFile = temporaryFolder.newFile();
        final String path = keyFile.getAbsolutePath();

        final String pemPublicKey = String.join("\n"
                , "-----BEGIN PUBLIC KEY-----"
                , "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEVO4MNlZG+iGYhoJd/cBpfMd9"
                , "YnKsntF+zhQs8lCbBabgY8kNoXVIEeOm4WPJ+W53gLDAIg6BNrZqxk9z1TLD6Dmz"
                , "t176OLYkNoTI9LNf6z4wuBenrlQ/H5UnYl6h5QoOdVpNAgEjkDcdTSOE1lqFLIle"
                , "KOT4nEF7MBGyOSP3KQIDAQAB"
                , "-----END PUBLIC KEY-----"
        );

        Files.write(Paths.get(path), pemPublicKey.getBytes());

        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='" + algorithm + "' encoding='PEM' path='" + path + "'/>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(1, algorithms.size());
        assertEquals(true, algorithms.containsKey("http://test.com"));
        assertEquals(algorithm, algorithms.get("http://test.com").getName());
    }

    @Test
    public void testOneSiteAllRsaFileKey() throws Exception {
        testOneSiteRsaFileKey("RS256");
        testOneSiteRsaFileKey("RS384");
        testOneSiteRsaFileKey("RS512");
    }

    @Test
    public void testOneSiteAllRsaInvalidEncoding() throws Exception {
        final String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='RS256' encoding='PEM'>"
                , "-----BEGIN PUBLIC KEY-----"
                , "  </site>"
                , "</sites>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }
}
