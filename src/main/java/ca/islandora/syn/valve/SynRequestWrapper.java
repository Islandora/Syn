package ca.islandora.syn.valve;

import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Request wrapper to use JWT provided name and roles.
 * 
 * @author whikloj
 * @since 2018-01-16
 */
public class SynRequestWrapper extends HttpServletRequestWrapper {
    private final String user;
    private List<String> roles;
    private final HttpServletRequest realRequest;

    /**
     * Constructor
     *
     * @param user
     *        The username for the Principal.
     * @param request
     *        The original request.
     */
    public SynRequestWrapper(final String user, final HttpServletRequest request) {
        super(request);
        this.user = user;
        this.realRequest = request;
    }

    /**
     * Constructor
     *
     * @param user
     *        The username for the Principal.
     * @param roles
     *        List of roles to include the Principal in.
     * @param request
     *        The original request.
     */
    public SynRequestWrapper(final String user, final List<String> roles, final HttpServletRequest request) {
        this(user, request);
        this.roles = roles;
    }

    @Override
    public boolean isUserInRole(final String role) {
        if (roles == null) {
            return this.realRequest.isUserInRole(role);
        }
        return roles.contains(role);
    }

    @Override
    public Principal getUserPrincipal() {
        if (this.user == null) {
            return realRequest.getUserPrincipal();
        }

        // make an anonymous implementation to just return our user
        return new Principal() {
            @Override
            public String getName() {
                return user;
            }
        };
    }
}
