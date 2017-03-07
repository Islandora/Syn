package ca.islandora.jwt.settings;

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

    public void testOneSiteHmacInlineKey(String algorithm) throws Exception {
        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='" + algorithm + "' encoding='plain'>"
                , "   test data"
                , "  </site>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
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
        String testXml = String.join("\n"
                , "<sites version='2'>"
                , "  <site url='http://test.com' algorithm='HS384' encoding='plain'>"
                , "   test data"
                , "  </site>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testOneSiteHmacBase64() throws Exception {
        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS256' encoding='base64'>"
                , "   am9uYXRoYW4gaXMgYXdlc29tZQ=="
                , "  </site>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(1, algorithms.size());
    }

    @Test
    public void testOneSiteHmacInvalidBase64() throws Exception {
        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS256' encoding='base64'>"
                , "   this is invalid base64"
                , "  </site>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testOneSiteHmacInvalidEncoding() throws Exception {
        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS256' encoding='badalgorithm'>"
                , "   this is invalid base64"
                , "  </site>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    public void testOneSiteHmacFileKey(String algorithm) throws Exception {
        File key = temporaryFolder.newFile();
        String path = key.getAbsolutePath();

        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='" + algorithm + "' encoding='plain' path='" + path + "'/>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
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
        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS384' encoding='plain' path='foo'>"
                , "   test data"
                , "  </site>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testSiteNeitherInlineAndPath() throws Exception {
        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS384' encoding='plain'/>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testSiteInvalidPath() throws Exception {
        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='HS384' encoding='plain' path='foo'/>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testSiteNoUrlDefault() throws Exception {
        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site algorithm='HS256' encoding='plain' default='true'>"
                , "   test data"
                , "  </site>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(1, algorithms.size());
    }

    public void testOneSiteRsaInlineKey(String algorithm) throws Exception {
        String testXml = String.join("\n"
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

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
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

    public void testOneSiteRsaFileKey(String algorithm) throws Exception {
        File keyFile = temporaryFolder.newFile();
        String path = keyFile.getAbsolutePath();

        String pemPublicKey = String.join("\n"
                , "-----BEGIN PUBLIC KEY-----"
                , "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEVO4MNlZG+iGYhoJd/cBpfMd9"
                , "YnKsntF+zhQs8lCbBabgY8kNoXVIEeOm4WPJ+W53gLDAIg6BNrZqxk9z1TLD6Dmz"
                , "t176OLYkNoTI9LNf6z4wuBenrlQ/H5UnYl6h5QoOdVpNAgEjkDcdTSOE1lqFLIle"
                , "KOT4nEF7MBGyOSP3KQIDAQAB"
                , "-----END PUBLIC KEY-----"
        );

        Files.write(Paths.get(path), pemPublicKey.getBytes());

        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='" + algorithm + "' encoding='PEM' path='" + path + "'/>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
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
        String testXml = String.join("\n"
                , "<sites version='1'>"
                , "  <site url='http://test.com' algorithm='RS256' encoding='PEM'>"
                , "-----BEGIN PUBLIC KEY-----"
                , "  </site>"
                , "</sites>"
        );

        InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        Map<String,Algorithm> algorithms = SettingsParser.getSiteAlgorithms(stream);
        assertEquals(0, algorithms.size());
    }
}
