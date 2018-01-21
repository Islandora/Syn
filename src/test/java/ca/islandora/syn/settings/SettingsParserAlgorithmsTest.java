package ca.islandora.syn.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.auth0.jwt.algorithms.Algorithm;

public class SettingsParserAlgorithmsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private void testOneSiteHmacInlineKey(final String algorithm) throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: " + algorithm,
                "  encoding: plain",
                "  key: test data");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
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
    public void testUnsupportedAlgorithm() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: AES128",
                "  encoding: plain",
                "  key: test data");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(0, algorithms.size());
        assertFalse(algorithms.containsKey("http://test.com"));
    }

    @Test(expected = SettingsParserException.class)
    public void testInvalidSitesVersion() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 2",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS384",
                "  encoding: plain",
                "  key: test data");

        final StringReader stream = new StringReader(testXml);
        SettingsParser.create(stream).getSiteAlgorithms();
    }

    @Test
    public void testOneSiteHmacBase64() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS256",
                "  encoding: base64",
                "  key: am9uYXRoYW4gaXMgYXdlc29tZQ==");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(1, algorithms.size());
    }

    @Test
    public void testOneSiteHmacInvalidBase64() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS256",
                "  encoding: base64",
                "  key: this is invalid base64");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testOneSiteHmacInvalidEncoding() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS256",
                "  encoding: badalgorithm",
                "  key: this is invalid base64");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(0, algorithms.size());
    }

    private void testOneSiteHmacFileKey(final String algorithm) throws Exception {
        final File key = temporaryFolder.newFile();
        final String path = key.getAbsolutePath();

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: " + algorithm,
                "  encoding: plain",
                "  path: " + path);

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
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
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS384",
                "  encoding: plain",
                "  path: foo",
                "  key: test data");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testSiteNeitherInlineAndPath() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS384",
                "  encoding: plain");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testSiteInvalidPath() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS384",
                "  encoding: plain",
                "  path: foo");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testSiteNoUrlDefault() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  algorithm: HS256",
                "  encoding: plain",
                "  default: true",
                "  key: test data");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(1, algorithms.size());
    }

    @Test
    public void testSiteNoUrl() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  algorithm: HS256",
                "  encoding: plain",
                "  key: test data");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(0, algorithms.size());
    }

    private void testOneSiteRsaInlineKey(final String algorithm) throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: " + algorithm,
                "  encoding: PEM",
                "  key: |",
                "    -----BEGIN PUBLIC KEY-----",
                "    MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEVO4MNlZG+iGYhoJd/cBpfMd9",
                "    YnKsntF+zhQs8lCbBabgY8kNoXVIEeOm4WPJ+W53gLDAIg6BNrZqxk9z1TLD6Dmz",
                "    t176OLYkNoTI9LNf6z4wuBenrlQ/H5UnYl6h5QoOdVpNAgEjkDcdTSOE1lqFLIle",
                "    KOT4nEF7MBGyOSP3KQIDAQAB",
                "    -----END PUBLIC KEY-----");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
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

        final String pemPublicKey = String.join("\n", "-----BEGIN PUBLIC KEY-----",
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEVO4MNlZG+iGYhoJd/cBpfMd9",
                "YnKsntF+zhQs8lCbBabgY8kNoXVIEeOm4WPJ+W53gLDAIg6BNrZqxk9z1TLD6Dmz",
                "t176OLYkNoTI9LNf6z4wuBenrlQ/H5UnYl6h5QoOdVpNAgEjkDcdTSOE1lqFLIle", "KOT4nEF7MBGyOSP3KQIDAQAB",
                "-----END PUBLIC KEY-----");

        Files.write(Paths.get(path), pemPublicKey.getBytes());

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: " + algorithm,
                "  encoding: PEM",
                "  path: " + path);

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
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
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: RS256",
                "  encoding: PEM",
                "  key: |",
                "    -----BEGIN PUBLIC KEY-----");

        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(0, algorithms.size());
    }

    @Test
    public void testMultipleDefaults() throws Exception {
        final File keyFile = temporaryFolder.newFile();
        final String path = keyFile.getAbsolutePath();

        final String pemPublicKey = String.join("\n", "-----BEGIN PUBLIC KEY-----",
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEVO4MNlZG+iGYhoJd/cBpfMd9",
                "YnKsntF+zhQs8lCbBabgY8kNoXVIEeOm4WPJ+W53gLDAIg6BNrZqxk9z1TLD6Dmz",
                "t176OLYkNoTI9LNf6z4wuBenrlQ/H5UnYl6h5QoOdVpNAgEjkDcdTSOE1lqFLIle", "KOT4nEF7MBGyOSP3KQIDAQAB",
                "-----END PUBLIC KEY-----");

        Files.write(Paths.get(path), pemPublicKey.getBytes());

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  algorithm: RS384",
                "  path: " + path,
                "  encoding: PEM",
                "  default: true",
                "site:",
                "  algorithm: HS256",
                "  path: " + path,
                "  encoding: plain",
                "  default: true");
        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(1, algorithms.size());
    }

    @Test
    public void testInvalidAlgorithm() throws Exception {
        final File keyFile = temporaryFolder.newFile();
        final String path = keyFile.getAbsolutePath();

        final String pemPublicKey = String.join("\n", "-----BEGIN PUBLIC KEY-----",
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEVO4MNlZG+iGYhoJd/cBpfMd9",
                "YnKsntF+zhQs8lCbBabgY8kNoXVIEeOm4WPJ+W53gLDAIg6BNrZqxk9z1TLD6Dmz",
                "t176OLYkNoTI9LNf6z4wuBenrlQ/H5UnYl6h5QoOdVpNAgEjkDcdTSOE1lqFLIle", "KOT4nEF7MBGyOSP3KQIDAQAB",
                "-----END PUBLIC KEY-----");

        Files.write(Paths.get(path), pemPublicKey.getBytes());

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  algorithm: RSA384",
                "  path: " + path,
                "  encoding: PEM",
                "  default: true");
        final StringReader stream = new StringReader(testXml);
        final Map<String, Algorithm> algorithms = SettingsParser.create(stream).getSiteAlgorithms();
        assertEquals(0, algorithms.size());
    }
}
