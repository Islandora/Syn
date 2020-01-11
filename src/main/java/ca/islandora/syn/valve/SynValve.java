package ca.islandora.syn.valve;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;

import com.auth0.jwt.algorithms.Algorithm;

import ca.islandora.syn.settings.Config;
import ca.islandora.syn.settings.SettingsParser;
import ca.islandora.syn.settings.Token;
import ca.islandora.syn.token.Verifier;

public class SynValve extends ValveBase {

    private String pathname = "conf/syn-settings.xml";
    private static final Log log = LogFactory.getLog(SynValve.class);
    private static final List<String> adminRole = new ArrayList<>();
    private static final List<String> userRole = new ArrayList<>();

    static {
        adminRole.add("fedoraAdmin");
        userRole.add("fedoraUser");
    }

    private static final String adminUserRole = "fedoraAdmin";

    private Map<String, Algorithm> algorithmMap = null;
    private Map<String, Token> staticTokenMap = null;
    private Map<String, Boolean> anonymousGetMap = null;
    private String roleHeader = null;
    private boolean isDisabled = false;

    @Override
    public void invoke(final Request request, final Response response)
            throws IOException, ServletException {

        final SecurityConstraint[] constraints = this.container.getRealm()
                .findSecurityConstraints(request, request.getContext());

        if (this.isDisabled || (constraints == null
                && !request.getContext().getPreemptiveAuthentication())
            || !hasAuthConstraint(constraints)) {
            this.getNext().invoke(request, response);
        } else {
            handleAuthentication(request, response);
        }
    }

    /**
     * Does the current context have an auth-constraint
     *
     * @param constraints
     *        security constraints for the current context and request
     * @return boolean if authentication is required.
     */
    private boolean hasAuthConstraint(final SecurityConstraint[] constraints) {
        boolean authConstraint = true;
        for (final SecurityConstraint securityConstraint : constraints) {
            authConstraint &= securityConstraint.getAuthConstraint();
        }
        return authConstraint;
    }

    /**
     * Do the authentication
     *
     * @param request
     *        the current request
     * @param response
     *        the current response
     * @throws IOException
     * @throws ServletException
     */
    private void handleAuthentication(final Request request, final Response response)
            throws IOException, ServletException {
        final String requestHost = request.getScheme() + "://" + request.getServerName() +
                (request.getServerPort() != 80 ? ":" + request.getServerPort() : "");
        if ((request.getMethod().equalsIgnoreCase("GET") ||
                request.getMethod().equals("HEAD")) &&
                allowGetRequests(requestHost)) {
            // Skip authentication
            setAnonymousRoles(request);
            this.getNext().invoke(request, response);
        } else if (doAuthentication(request)) {
            this.getNext().invoke(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token authentication failed.");
        }
    }

    /**
     * Do the authentication altering the request as necessary
     *
     * @param request
     *        the incoming request
     * @return true if we are authorized, false otherwise.
     */
    private boolean doAuthentication(final Request request) {
        String token = request.getHeader("Authorization");
        if (token == null) {
            log.info("Request did not contain any token.");
            return false;
        }

        final String[] tokenParts = token.split(" ");
        if (tokenParts.length != 2 || !tokenParts[0].equalsIgnoreCase("bearer")) {
            log.info("Token was malformed. Token: " + token);
            return false;
        }

        // strip bearer off of the token
        token = tokenParts[1];

        // check if we have a static token that matches
        if (this.staticTokenMap.containsKey(token)) {
            log.info("Site verified using static token.");
            setUserRolesFromStaticToken(request, this.staticTokenMap.get(token));
            request.setAuthType("SYN");
            return true;
        }

        final Verifier verifier = Verifier.create(token);
        if (verifier == null) {
            log.info("Token rejected for not containing correct claims.");
            return false;
        }

        final String url = verifier.getUrl();
        Algorithm algorithm = null;
        if (algorithmMap.containsKey(url)) {
            algorithm = algorithmMap.get(url);
        } else if (algorithmMap.containsKey(null)) {
            algorithm = algorithmMap.get(null);
        }

        if (algorithm == null) {
            log.info("No key found for site: " + url + ".");
            return false;
        }

        if (verifier.verify(algorithm)) {
            log.info("Site verified: " + url);
            setUserRolesFromToken(request, verifier);
            request.setAuthType("SYN");
            return true;
        } else {
            log.info("Token failed signature verification: " + url);
            return false;
        }
    }

    /**
     * Set principal and header with roles for anoymous
     *
     * @param request
     *        the incoming request
     */
    private void setAnonymousRoles(final Request request) {
        final List<String> roles = new ArrayList<String>();
        roles.add("anonymous");
        roles.add("islandora");
        final String name = "anonymous";
        addToRequest(request, name, roles);
    }

    /**
     * Set principal and header with roles on the request from a static configured
     * token
     *
     * @param request
     *        the incoming request
     * @param token
     *        the static token
     */
    private void setUserRolesFromStaticToken(final Request request, final Token token) {
        final List<String> roles = token.getRoles();
        roles.add("islandora");
        final String name = token.getUser();
        addToRequest(request, name, roles);
    }

    /**
     * Set principal and header with roles on the request from the JWT token
     *
     * @param request
     *        the incoming request
     * @param verifier
     *        the JWT verifier
     */
    private void setUserRolesFromToken(final Request request, final Verifier verifier) {
        final List<String> roles = verifier.getRoles();
        roles.add("islandora");
        roles.add(verifier.getUrl());
        final String name = verifier.getName();
        addToRequest(request, name, roles);
    }


    /**
     * Do the logic of allowing GET/HEAD requests.
     *
     * @param requestURI the site being requested
     * @return whether to allow GET requests without authentication.
     */
    private boolean allowGetRequests(final String requestURI) {
        // If there is a matching site URI, return its value
        if (anonymousGetMap.containsKey(requestURI)) {
            log.debug(
                String.format(
                    "Using site anonymous ({}) for GET/HEAD requests, site {}",
                    anonymousGetMap.get(requestURI),
                    requestURI));
            return anonymousGetMap.get(requestURI);
            // Else if there is a default, return its value.
        } else if (anonymousGetMap.containsKey("default")) {
            log.debug(
                String.format(
                    "Using default anonymous ({}) for GET/HEAD requests, host {}",
                    anonymousGetMap.get("default"),
                    requestURI));
            return anonymousGetMap.get("default");
        }
        // Else disallow anonymous.
        return false;
    }

    /**
     * Add all roles to a pre-configured header and set the single role to either
     * fedoraUser or fedoraAdmin based on whether they have an existing role that
     * matches `adminUserRole`
     *
     * @param request
     *        the incoming request
     * @param username
     *        the username to set on the principal
     * @param roles
     *        the roles to set on the HTTP header
     */
    private void addToRequest(final Request request, final String username, final List<String> roles) {
        final MessageBytes mb = request.getCoyoteRequest().getMimeHeaders().addValue(this.roleHeader);
        mb.setString(String.join(",", roles));
        final List<String> fedoraRole = Arrays
                .asList(roles.stream().anyMatch(t -> t.equalsIgnoreCase(adminUserRole)) ? "fedoraAdmin" : "fedoraUser");
        final GenericPrincipal principal = new GenericPrincipal(username, null, fedoraRole);
        request.setUserPrincipal(principal);
    }

    /**
     * Return the pathname to the syn-settings.xml file.
     *
     * @return the path
     */
    public String getPathname() {
        return pathname;
    }

    /**
     * Set the pathname of the syn-settings.xml file
     *
     * If you add pathname="" to your Valve config with your syn-settings location it
     * should get set here.
     */
    public void setPathname(final String pathname) {
        this.pathname = pathname;
    }

    @Override
    public synchronized void startInternal() throws LifecycleException {
        // Perform normal superclass initialization
        super.startInternal();
        // Validate the existence of our database file
        File file = new File(pathname);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("catalina.base"), pathname);
        }
        if (!file.exists() || !file.canRead()) {
            throw new LifecycleException("Unable to load XML Configuration from Path: " + pathname);
        }

        // Load the contents of the database file
        try {
            final Config sites = SettingsParser.getSites(new FileInputStream(file));
            this.algorithmMap = SettingsParser.getSiteAlgorithms(sites);
            this.staticTokenMap = SettingsParser.getSiteStaticTokens(sites);
            this.anonymousGetMap = SettingsParser.getSiteAllowAnonymous(sites);
            this.roleHeader = sites.getHeader();
            this.isDisabled = sites.getDisabled();
        } catch (final Exception e) {
            throw new LifecycleException("Error parsing XML Configuration", e);
        }
    }
}
