package ca.islandora.syn.settings;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;

import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public final class SettingsParser {
    private static Logger log = getLogger(Site.class);

    private enum AlgorithmType {
        INVALID, RSA, HMAC
    }

    private static final int VALID_VERSION = 1;

    private Config loadedSites;

    private final static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    /**
     * Constructor.
     * 
     * @param settings
     *        A Reader object with the configuration in it.
     * @throws Exception
     *         On loading/parsing or version of configuration.
     */
    public SettingsParser(final Reader settings) {
        try {
            loadedSites = mapper.readValue(settings, Config.class);
        } catch (final Exception e) {
            log.error("Error loading settings file.", e);
            throw new SettingsParserException("Error parsing settings file.", e);
        }

        if (loadedSites.getVersion() != VALID_VERSION) {
            log.error("Incorrect config version. Aborting.");
            throw new SettingsParserException("Incorrect config version. Aborting.");
        }
    }

    /**
     * Static creator.
     * 
     * @param settings
     *        A Reader object with the configuration on it.
     * @return The SettingsParser.
     * @throws Exception
     *         On loading/parsing or version of configuration.
     */
    public static SettingsParser create(final Reader settings) {
        return new SettingsParser(settings);
    }

    /**
     * Determine the type of key algorithm.
     * 
     * @param algorithm
     *        The algorithm name.
     * @return an algorithm.
     */
    private static AlgorithmType getSiteAlgorithmType(final String algorithm) {
        if (algorithm.toUpperCase().startsWith("RS")) {
            return AlgorithmType.RSA;
        } else if (algorithm.toUpperCase().startsWith("HS")) {
            return AlgorithmType.HMAC;
        } else {
            return AlgorithmType.INVALID;
        }
    }

    /**
     * Expand the site's key path if it is not absolute.
     * 
     * @param site
     *        The site to act on.
     * @return true if key exists.
     */
    private static boolean validateExpandPath(final Site site) {
        File file = new File(site.getPath());
        if (!file.isAbsolute()) {
            final URL fileUrl = SettingsParser.class.getClassLoader().getResource(site.getPath());
            if (fileUrl == null) {
                log.error("Cannot locate resource " + site.getPath() + " on the classpath.");
                return false;
            }
            file = new File(fileUrl.getPath());
        }
        if (!file.exists() || !file.canRead()) {
            log.error("Path does not exist:" + site.getPath() + ". Site ignored.");
            return false;
        }
        site.setPath(file.getAbsolutePath());
        return true;
    }

    /**
     * Parse a RSA encoded key and return the algorithm for verifying.
     * 
     * @param site
     *        The site to get the key for.
     * @return A RSA algorithm for the site's key.
     */
    private static Algorithm getRsaAlgorithm(final Site site) {
        Reader publicKeyReader = null;
        RSAPublicKey publicKey = null;

        if (site.getKey() != null) {
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

    /**
     * Parse a HMAC encoded key and return the algorithm for verifying.
     * 
     * @param site
     *        The site to get the key for.
     * @return A HMAC algorithm for the site's key.
     */
    private static Algorithm getHmacAlgorithm(final Site site) {
        final byte[] secret;
        byte[] secretRaw = null;

        if (site.getKey() != null) {
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
     * Get site keys from loaded Config.
     * 
     * @return Map of URLs (or null) and parsed keys for verification.
     */
    public Map<String, Algorithm> getSiteAlgorithms() {
        final Map<String, Algorithm> algorithms = new HashMap<>();
        if (loadedSites == null) {
            return algorithms;
        }

        boolean defaultSet = false;

        for (final Site site : loadedSites.getSites()) {
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
                log.error("Invalid algorithm selection: " + site.getAlgorithm() + ". Site ignored.");
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
     * Get static tokens from the loaded Config.
     * 
     * @return Map of token value and Token object.
     */
    public Map<String, Token> getSiteStaticTokens() {
        if (loadedSites == null) {
            return new HashMap<String, Token>();
        }

        final Map<String, Token> tokens = loadedSites.getTokens().stream().filter(x -> !x.getValue().isEmpty())
                .collect(Collectors.toMap(Token::getValue, t -> t));

        return tokens;
    }

    /**
     * Build a list of site urls that allow anonymous GET requests.
     *
     * @return Map of all URLs (or null for default) and boolean if they allow
     *         anonymous.
     */
    public Map<String, Boolean> getSiteAllowAnonymous() {
        if (loadedSites == null) {
            return new HashMap<String, Boolean>();
        }

        final Map<String, Boolean> anonymousAllowed = loadedSites.getSites().stream().filter(s -> !s.getDefault())
                .collect(Collectors.toMap(Site::getUrl, Site::getAnonymous));
        loadedSites.getSites().stream().filter(Site::getDefault).findFirst()
                .ifPresent(s -> anonymousAllowed.put("default", s.getAnonymous()));

        return anonymousAllowed;
    }

    /**
     * Getter for loaded Config object.
     * 
     * @return
     */
    public Config getConfig() {
        return loadedSites;
    }
}
