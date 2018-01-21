package ca.islandora.syn.valve;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.auth0.jwt.algorithms.Algorithm;

import ca.islandora.syn.settings.SettingsParser;
import ca.islandora.syn.settings.Token;
import ca.islandora.syn.token.InvalidTokenException;
import ca.islandora.syn.token.Verifier;
/**
 * The JWT testing filter
 * @author jonathangreen
 * @author whikloj
 *
 */
public class SynFilter implements Filter {

    private static final Logger LOGGER = getLogger(SynFilter.class);

    public static final String UNAUTHORIZED_MSG = "Token authentication failed.";
    public static final String FORBIDDEN_MSG = "Token authentication failed.";

    private static String settingsPath;
    private static Map<String, Algorithm> algorithmMap;
    private static Map<String, Token> staticTokenMap;
    private static Map<String, Boolean> anonymousMap;

    /**
     * Constructor
     */
    public SynFilter() {

    }

    @Override
    public void init(final FilterConfig config) throws ServletException {
        settingsPath = config.getInitParameter("settings-path");
        if (settingsPath == null) {
            throw new ServletException("settings-path init parameter must have location of syn-settings.yml file.");
        }
        // Validate the existence of our database file
        File file = new File(settingsPath);
        if (!file.isAbsolute()) {
            final URL settingsUrl = SynFilter.class.getClassLoader().getResource(settingsPath);
            if (settingsUrl == null) {
                LOGGER.error("Error locating settings file :" + settingsPath);
                throw new ServletException("Cannot find Syn settings from path: " + settingsPath);
            }
            file = new File(settingsUrl.getPath());
        }
        if (!file.exists() || !file.canRead()) {
            LOGGER.error("Unable to load Syn configuration from path: " + settingsPath);
            throw new ServletException("Unable to load Syn configuration from path: " + settingsPath);
        }

        // Load the contents of the database file
        try {
            final SettingsParser parser = SettingsParser.create(new FileReader(file));
            algorithmMap = parser.getSiteAlgorithms();
            staticTokenMap = parser.getSiteStaticTokens();
            anonymousMap = parser.getSiteAllowAnonymous();
        } catch (final Exception e) {
            throw new ServletException("Error parsing Syn configuration", e);
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(final ServletRequest servRequest, final ServletResponse servResponse, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servRequest;
        final HttpServletResponse response = (HttpServletResponse) servResponse;

        String token = request.getHeader("Authorization");
        if (token == null) {
            LOGGER.info("Request did not contain any token.");
            // Only check for anonymous access if there is no token.
            if ((request.getMethod().equalsIgnoreCase("GET") ||
                    request.getMethod().equals("HEAD")) &&
                    allowGetRequests(buildUrlRegex(request))) {
                chain.doFilter(setAnonymousRoles(request), response);
                return;
            }
            response.sendError(SC_UNAUTHORIZED, UNAUTHORIZED_MSG);
            return;
        }

        final String[] tokenParts = token.split(" ");
        if (tokenParts.length != 2 || !tokenParts[0].equalsIgnoreCase("bearer")) {
            LOGGER.info("Token was malformed. Token: " + token);
            response.sendError(SC_UNAUTHORIZED, UNAUTHORIZED_MSG);
            return;
        }

        // strip bearer off of the token
        token = tokenParts[1];

        // check if we have a static token that matches
        if (staticTokenMap.containsKey(token)) {
            LOGGER.info("Site verified using static token.");
            chain.doFilter(setUserRolesFromStaticToken(request, staticTokenMap.get(token)), response);
            return;
        }

        final Verifier verifier;
        try {
            verifier = Verifier.create(token);
        } catch (final InvalidTokenException e) {
            response.sendError(SC_UNAUTHORIZED, FORBIDDEN_MSG);
            return;
        }

        final String url = verifier.getUrl();
        Algorithm algorithm = null;
        if (algorithmMap.containsKey(url)) {
            algorithm = algorithmMap.get(url);
        } else if (algorithmMap.containsKey(null)) {
            algorithm = algorithmMap.get(null);
        }

        if (algorithm == null) {
            LOGGER.info("No key found for site: " + url + ".");
            response.sendError(SC_UNAUTHORIZED, UNAUTHORIZED_MSG);
            return;
        }

        if (verifier.verify(algorithm)) {
            LOGGER.info("Site verified: " + url);
            chain.doFilter(setUserRolesFromToken(request, verifier), response);
            return;
        } else {
            LOGGER.info("Token failed signature verification: " + url);
            response.sendError(SC_FORBIDDEN, FORBIDDEN_MSG);
            return;
        }

    }

    /**
     * Create fake principal with anonymous credentials.
     *
     * @param request
     *        The original incoming request.
     * @return A wrapper with created credentials.
     */
    private HttpServletRequestWrapper setAnonymousRoles(final HttpServletRequest request) {
        final String host = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 ? ":" + request.getServerPort() : "");
        final List<String> roles = Arrays.asList("anonymous", "islandora", host);
        final String name = "anonymous";
        return new SynRequestWrapper(name, roles, request);
    }

    /**
     * Create fake principal with credentials from static token.
     *
     * @param request
     *        The original incoming request.
     * @return A wrapper with created credentials.
     */
    private HttpServletRequestWrapper setUserRolesFromStaticToken(final HttpServletRequest request, final Token token) {
        final List<String> roles = token.getRoles();
        roles.add("islandora");
        final String name = token.getUser();
        return new SynRequestWrapper(name, roles, request);
    }

    /**
     * Create fake principal with credentials from JWT.
     *
     * @param request
     *        The original incoming request.
     * @return A wrapper with created credentials.
     */
    private HttpServletRequestWrapper setUserRolesFromToken(final HttpServletRequest request, final Verifier verifier) {
        final List<String> roles = verifier.getRoles();
        roles.add("islandora");
        roles.add(verifier.getUrl());
        final String name = verifier.getName();
        return new SynRequestWrapper(name, roles, request);
    }

    /**
     * Do the logic of allowing GET/HEAD requests.
     *
     * @param hostRegex
     *        A regular expression built of the hostname.
     * @return whether to allow GET requests without authentication.
     */
    private boolean allowGetRequests(final String hostRegex) {
        // If there is a matching site URI, return its value
        for (final Entry<String, Boolean> site : anonymousMap.entrySet()) {
            if (Pattern.matches(hostRegex, site.getKey())) {
                return site.getValue();
            }
        }
        // Else if there is a default, return its value.
        if (anonymousMap.containsKey("default")) {
            LOGGER.debug(
                    String.format(
                            "Using default anonymous ({}) for GET/HEAD requests",
                            anonymousMap.get("default")));
            return anonymousMap.get("default");
        }
        // Else disallow anonymous.
        return false;
    }

    /**
     * Build a regular expression from the current scheme, hostname and port for
     * matching against the site urls.
     *
     * @param request
     *        The current request.
     * @return The regular expression.
     */
    private static String buildUrlRegex(final HttpServletRequest request) {
        return "^" + request.getScheme() + "://" + Pattern.quote(request.getServerName()) +
                "(?::" + request.getServerPort() + ")" + (request.getServerPort() == 80 ? "?" : "") + "/?$";
    }
}
