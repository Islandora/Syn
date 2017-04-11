package ca.islandora.syn.valve;

import ca.islandora.syn.settings.SettingsParser;
import ca.islandora.syn.settings.Token;
import ca.islandora.syn.token.Verifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;

public class SynValve extends ValveBase {

    private String pathname = "conf/syn-settings.xml";
    private static final Log log = LogFactory.getLog(SynValve.class);
    private Map<String, Algorithm> algorithmMap = null;
    private Map<String, Token> staticTokenMap = null;

    @Override
    public void invoke(final Request request, final Response response)
            throws IOException, ServletException {

        final SecurityConstraint[] constraints = this.container.getRealm()
                .findSecurityConstraints(request, request.getContext());

        if ((constraints == null
                && !request.getContext().getPreemptiveAuthentication())
                || !hasAuthConstraint(constraints)) {
            this.getNext().invoke(request, response);
        } else {
            handleAuthentication(request, response);
        }
    }

    private boolean hasAuthConstraint(final SecurityConstraint[] constraints) {
        boolean authConstraint = true;
        for (SecurityConstraint securityConstraint : constraints) {
            authConstraint &= securityConstraint.getAuthConstraint();
        }
        return authConstraint;
    }

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

    private void setUserRolesFromStaticToken(final Request request, final Token token) {
        final List<String> roles = token.getRoles();
        roles.add("islandora");
        final String name = token.getUser();
        final GenericPrincipal principal = new GenericPrincipal(name, null, roles);
        request.setUserPrincipal(principal);
    }

    private void setUserRolesFromToken(final Request request, final Verifier verifier) {
        final List<String> roles = verifier.getRoles();
        roles.add("islandora");
        roles.add(verifier.getUrl());
        final String name = verifier.getName();
        final GenericPrincipal principal = new GenericPrincipal(name, null, roles);
        request.setUserPrincipal(principal);
    }

    private void handleAuthentication(final Request request, final Response response)
            throws IOException, ServletException {
        if (doAuthentication(request)) {
            this.getNext().invoke(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token authentication failed.");
        }
    }

    public String getPathname() {
        return pathname;
    }
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
            this.algorithmMap = SettingsParser.getSiteAlgorithms(new FileInputStream(file));
            this.staticTokenMap = SettingsParser.getSiteStaticTokens(new FileInputStream(file));
        } catch (Exception e) {
            throw new LifecycleException("Error parsing XML Configuration", e);
        }
    }
}
