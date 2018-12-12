package ca.islandora.syn.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.xml.sax.SAXException;

import com.auth0.jwt.algorithms.Algorithm;

public final class SettingsParser {
    private static Digester digester = null;
    private static Log log = LogFactory.getLog(Site.class);
    private enum AlgorithmType {INVALID, RSA, HMAC}

    private SettingsParser() { }

    private static Digester getDigester() {
        if (digester == null) {
            digester = new Digester();
            digester.setValidating(false);
            digester.addObjectCreate("config", "ca.islandora.syn.settings.Config");
            digester.addSetProperties("config");
            digester.addObjectCreate("config/site", "ca.islandora.syn.settings.Site");
            digester.addSetProperties("config/site");
            digester.addCallMethod("config/site", "setKey", 0);
            digester.addSetNext("config/site", "addSite", "ca.islandora.syn.settings.Site");
            digester.addObjectCreate("config/token", "ca.islandora.syn.settings.Token");
            digester.addSetProperties("config/token");
            digester.addCallMethod("config/token", "setToken", 0);
            digester.addSetNext("config/token", "addToken", "ca.islandora.syn.settings.Token");
        }
        return digester;
    }


    private static AlgorithmType getSiteAlgorithmType(final String algorithm) {
        if (algorithm.equalsIgnoreCase("RS256")) {
            return AlgorithmType.RSA;
        } else if (algorithm.equalsIgnoreCase("RS384")) {
            return AlgorithmType.RSA;
        } else if (algorithm.equalsIgnoreCase("RS512")) {
            return AlgorithmType.RSA;
        }

        if (algorithm.equalsIgnoreCase("HS256")) {
            return AlgorithmType.HMAC;
        } else if (algorithm.equalsIgnoreCase("HS384")) {
            return AlgorithmType.HMAC;
        } else if (algorithm.equalsIgnoreCase("HS512")) {
            return AlgorithmType.HMAC;
        } else {
            return AlgorithmType.INVALID;
        }
    }

    private static boolean validateExpandPath(final Site site) {
        File file = new File(site.getPath());
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("catalina.base"), site.getPath());
        }
        if (!file.exists() || !file.canRead()) {
            log.error("Path does not exist:" + site.getPath() + ". Site ignored.");
            return false;
        }
        site.setPath(file.getAbsolutePath());
        return true;
    }

    private static Algorithm getRsaAlgorithm(final Site site) {
        Reader publicKeyReader = null;
        RSAPublicKey publicKey = null;

        if (!site.getKey().equalsIgnoreCase("")) {
            publicKeyReader = new StringReader(site.getKey());
        } else if (site.getPath() != null) {
            try {
                publicKeyReader = new FileReader(site.getPath());
            } catch (final FileNotFoundException e) {
                log.error("Private key file not found.");
            }
        }

        if (publicKeyReader == null) {
            return null;
        }

        if (site.getEncoding().equalsIgnoreCase("pem")) {
            try {
                final PemReader pemReader = new PemReader(publicKeyReader);
                final KeyFactory factory = KeyFactory.getInstance("RSA");
                final PemObject pemObject = pemReader.readPemObject();
                final X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pemObject.getContent());
                publicKey = (RSAPublicKey) factory.generatePublic(pubKeySpec);
                pemReader.close();
                publicKeyReader.close();
            } catch (final Exception e) {
                log.error("Error loading public key.");
                return null;
            }
        }

        if (publicKey == null) {
            return null;
        }

        if (site.getAlgorithm().equalsIgnoreCase("RS256")) {
            return Algorithm.RSA256(publicKey);
        } else if (site.getAlgorithm().equalsIgnoreCase("RS384")) {
            return Algorithm.RSA384(publicKey);
        } else if (site.getAlgorithm().equalsIgnoreCase("RS512")) {
            return Algorithm.RSA512(publicKey);
        } else {
            return null;
        }
    }

    private static Algorithm getHmacAlgorithm(final Site site) {
        final byte[] secret;
        byte[] secretRaw = null;

        if (!site.getKey().equalsIgnoreCase("")) {
            secretRaw = site.getKey().trim().getBytes();
        } else if (site.getPath() != null) {
            try {
                secretRaw = Files.readAllBytes(Paths.get(site.getPath()));
            } catch (final IOException e) {
                log.error("Unable to get secret from file.", e);
            }
        }

        if (secretRaw == null) {
            return null;
        }

        if (site.getEncoding().equalsIgnoreCase("base64")) {
            try {
                secret = Base64.getDecoder().decode(secretRaw);
            } catch (final Exception e) {
                log.error("Base64 decode error. Skipping site.", e);
                return null;
            }
        } else if (site.getEncoding().equalsIgnoreCase("plain")) {
            secret = secretRaw;
        } else {
            return null;
        }

        if (site.getAlgorithm().equalsIgnoreCase("HS256")) {
            return Algorithm.HMAC256(secret);
        } else if (site.getAlgorithm().equalsIgnoreCase("HS384")) {
            return Algorithm.HMAC384(secret);
        } else if (site.getAlgorithm().equalsIgnoreCase("HS512")) {
            return Algorithm.HMAC512(secret);
        } else {
            return null;
        }
    }

    /**
     * Parse a configuration file and return a Config object
     *
     * @param settings
     *        The configuration file stream
     * @return the config object
     */
    public static Config getSites(final InputStream settings) {
        final Config sites;

        try {
            sites = getSitesObject(settings);
        } catch (final Exception e) {
            log.error("Error loading settings file.", e);
            return null;
        }

        if (sites.getVersion() != 1) {
            log.error("Incorrect XML version. Aborting.");
            return null;
        }

        return sites;
    }

    /**
     * Get algorithms for sites
     *
     * @param sites
     *        configuration with sites
     * @return map of site url (or null for default) and algorithm
     */
    public static Map<String, Algorithm> getSiteAlgorithms(final Config sites) {
        final Map<String, Algorithm> algorithms = new HashMap<>();
        if (sites == null) {
            return algorithms;
        }

        boolean defaultSet = false;

        for (final Site site : sites.getSites()) {
            final boolean pathDefined = site.getPath() != null && !site.getPath().equalsIgnoreCase("");
            final boolean keyDefined = site.getKey() != null && !site.getKey().equalsIgnoreCase("");

            // Check that we don't have both a key and a path defined
            if (pathDefined == keyDefined) {
                log.error("Only one of path or key must be defined.");
                continue;
            }

            if (site.getPath() != null) {
                if (!validateExpandPath(site)) {
                    continue;
                }
            }

            // Check that the algorithm type is valid.
            final AlgorithmType algorithmType = getSiteAlgorithmType(site.getAlgorithm());
            final Algorithm algorithm;
            if (algorithmType == AlgorithmType.HMAC) {
                algorithm = getHmacAlgorithm(site);
            } else if (algorithmType == AlgorithmType.RSA) {
                algorithm = getRsaAlgorithm(site);
            } else {
                log.error("Invalid algorithm selection: " + site.getAlgorithm() + ". Site ignored." );
                continue;
            }

            if ((site.getUrl() == null || site.getUrl().equalsIgnoreCase("")) && !site.getDefault()) {
                log.error("Site URL must be defined for non-default sites.");
                continue;
            }

            if (site.getDefault()) {
                if (defaultSet) {
                    log.error("Multiple default sites specified in configuration.");
                    continue;
                }
                defaultSet = true;
            }

            if (algorithm != null) {
                final String name = site.getDefault() ? null : site.getUrl();
                algorithms.put(name, algorithm);
            }
        }

        return algorithms;
    }

    /**
     * Get the site static tokens from a set of sites in the configuration
     *
     * @param sites
     *        the configured sites
     * @return map of token string and token object
     */
    public static Map<String, Token> getSiteStaticTokens(final Config sites) {
        if (sites == null) {
            return new HashMap<String, Token>();
        }

        final Map<String, Token> tokens = sites.getTokens().stream().filter(x -> !x.getToken().isEmpty())
            .collect(Collectors.toMap(Token::getToken, t -> t));

        return tokens;
    }

    /**
     * Build a list of site urls that allow anonymous GET requests.
     *
     * @param sites
     *        a config object with the site information
     * @return list of site urls.
     */
    public static Map<String, Boolean> getSiteAllowAnonymous(final Config sites) {
        if (sites == null) {
            return new HashMap<String, Boolean>();
        }

        final Map<String, Boolean> anonymousAllowed = sites.getSites().stream().filter(s -> !s.getDefault())
            .collect(Collectors.toMap(Site::getUrl, Site::getAnonymous));
        sites.getSites().stream().filter(Site::getDefault).findFirst()
            .ifPresent(s -> anonymousAllowed.put("default", s.getAnonymous()));

        return anonymousAllowed;
    }

    static Config getSitesObject(final InputStream settings)
            throws IOException, SAXException {
        return (Config) getDigester().parse(settings);
    }
}
