package ca.islandora.syn.valves;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import ca.islandora.syn.valve.SynFilter;

@RunWith(MockitoJUnitRunner.class)
public class SynFilterTest {

    private SynFilter synFilter;

    private File settings;

    @Mock
    private FilterChain chain;

    @Mock
    private FilterConfig config;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static ZoneOffset offset;

    private ArgumentCaptor<HttpServletRequest> requestCaptor;
    private ArgumentCaptor<HttpServletResponse> responseCaptor;

    @Before
    public void setUp() throws Exception {
        settings = temporaryFolder.newFile();
        createSettings(settings);

        when(config.getInitParameter("settings-path")).thenReturn(settings.getAbsolutePath());

        synFilter = createFilter();

        when(request.getScheme()).thenReturn("http");
        when(request.getServerPort()).thenReturn(80);

        offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());

        requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        responseCaptor = ArgumentCaptor.forClass(HttpServletResponse.class);
    }

    private SynFilter createFilter() throws ServletException {
        final SynFilter synFilter = new SynFilter();
        synFilter.init(config);
        return synFilter;
    }

    @Test(expected = ServletException.class)
    public void missingInitParameter() throws Exception {
        when(config.getInitParameter("settings-path")).thenReturn(null);
        synFilter = createFilter();
    }

    @Test(expected = ServletException.class)
    public void absoluteSettingsDoesNotExist() throws Exception {
        when(config.getInitParameter("settings-path")).thenReturn("/tmp/fileIsFake");
        synFilter = createFilter();
    }

    @Test(expected = ServletException.class)
    public void relativeSettingsDoesNotExist() throws Exception {
        when(config.getInitParameter("settings-path")).thenReturn("fileIsFake");
        synFilter = createFilter();
    }

    // For some reason files on the classpath are not found???
    @Ignore
    @Test
    public void loadRelativeSettings() throws Exception {
        when(config.getInitParameter("settings-path")).thenReturn("exampleSettings.yaml");
        synFilter = createFilter();
        final String host = "http://test.com";
        final String username = "bob";
        final List<String> finalRoles = Arrays.asList("islandora", host);

        final String token = "Bearer " + JWT
                .create()
                .withClaim("webid", 1)
                .withClaim("sub", username)
                .withClaim("iss", host)
                .withArrayClaim("roles", new String[] {})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("bobPassword"));

        when(request.getHeader("Authorization")).thenReturn(token);

        synFilter.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertEquals(username, requestCaptor.getValue().getUserPrincipal().getName());

        for (final String role : finalRoles) {
            assertTrue(requestCaptor.getValue().isUserInRole(role));
        }

    }

    @Test(expected = ServletException.class)
    public void settingsParseFail() throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: bad",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS256",
                "  encoding: plain",
                "  anonymous: false",
                "  key: secret");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();
    }

    @Test
    public void shouldPassAuth() throws Exception {
        final String host = "http://test.com";
        final String username = "adminuser";
        final String[] roles = new String[] { "role1", "role2", "role3" };
        final ArrayList<String> finalRoles = new ArrayList<String>(Arrays.asList(roles));
        finalRoles.add("islandora");
        finalRoles.add(host);

        final String token = "Bearer " + JWT
                .create()
                .withClaim("webid", 1)
                .withClaim("sub", username)
                .withClaim("iss", host)
                .withArrayClaim("roles", roles)
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret"));

        when(request.getHeader("Authorization")).thenReturn(token);

        synFilter.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertEquals(username, requestCaptor.getValue().getUserPrincipal().getName());

        for (final String role : finalRoles) {
            assertTrue(requestCaptor.getValue().isUserInRole(role));
        }

    }

    @Test
    public void shouldPassAuthToken() throws Exception {
        final String defaultUser = "islandoraAdmin";
        final String token = "Bearer 1337";

        when(request.getHeader("Authorization"))
                .thenReturn(token);

        synFilter.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertEquals(defaultUser, requestCaptor.getValue().getUserPrincipal().getName());
        assertTrue(requestCaptor.getValue().isUserInRole("islandora"));
    }

    @Test
    public void shouldFailAuthBecauseOfTokenNotSet() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getServerName()).thenReturn("test.com");

        synFilter.doFilter(request, response, chain);

        verify(request).getHeader("Authorization");
        verify(response).sendError(SC_UNAUTHORIZED, SynFilter.UNAUTHORIZED_MSG);
    }

    @Test
    public void shouldFailAuthBecauseOfTokenInvalid1() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("garbage");

        synFilter.doFilter(request, response, chain);

        verify(request).getHeader("Authorization");
        verify(response).sendError(SC_UNAUTHORIZED, SynFilter.UNAUTHORIZED_MSG);
    }

    @Test
    public void shouldFailAuthBecauseOfTokenInvalid2() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("killer bandit foo");

        synFilter.doFilter(request, response, chain);

        verify(request).getHeader("Authorization");
        verify(response).sendError(SC_UNAUTHORIZED, SynFilter.UNAUTHORIZED_MSG);
    }

    @Test
    public void shouldFailTokenMissingUid() throws Exception {
        final String host = "http://test.com";
        final String username = "adminuser";
        final String[] roles = new String[] { "role1", "role2", "role3" };
        final ArrayList<String> finalRoles = new ArrayList<String>(Arrays.asList(roles));
        finalRoles.add("islandora");
        finalRoles.add(host);

        final String token = JWT
                .create()
                .withClaim("sub", username)
                .withClaim("iss", host)
                .withArrayClaim("roles", roles)
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret"));

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        synFilter.doFilter(request, response, chain);

        verify(request).getHeader("Authorization");
        verify(response).sendError(SC_UNAUTHORIZED, SynFilter.UNAUTHORIZED_MSG);
    }

    @Test
    public void shouldPassAuthDefaultSite() throws Exception {
        final String host = "http://test2.com";
        final String username = "normalUser";
        final List<String> finalRoles = Arrays.asList("islandora", host);

        final String token = JWT
                .create()
                .withClaim("webid", 1)
                .withClaim("sub", username)
                .withClaim("iss", host)
                .withArrayClaim("roles", new String[] {})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret2"));

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        synFilter.doFilter(request, response, chain);
        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        verify(request).getHeader("Authorization");
        assertEquals(username, requestCaptor.getValue().getUserPrincipal().getName());

        for (final String role : finalRoles) {
            assertTrue(requestCaptor.getValue().isUserInRole(role));
        }
    }

    @Test
    public void shouldFailAuthBecauseNoSiteMatch() throws Exception {
        final String host = "http://test-no-match.com";
        final String username = "normalUser";
        final String token = JWT
                .create()
                .withClaim("webid", 1)
                .withClaim("sub", username)
                .withClaim("iss", host)
                .withArrayClaim("roles", new String[] {})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret"));

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS256",
                "  encoding: plain",
                "  anonymous: false",
                "  key: secret");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        // Recreate because the settings file has changed so we need to run init again.
        synFilter = createFilter();
        synFilter.doFilter(request, response, chain);

        verify(request).getHeader("Authorization");
        verify(response).sendError(SC_UNAUTHORIZED, SynFilter.UNAUTHORIZED_MSG);
    }

    @Test
    public void allowAuthWithToken() throws Exception {
        final String host = "http://anon-test.com";
        final String username = "Bob";
        final List<String> finalRoles = Arrays.asList("islandora", host);
        final String token = JWT
                .create()
                .withClaim("webid", 1)
                .withClaim("sub", username)
                .withClaim("iss", host)
                .withArrayClaim("roles", new String[] {})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secretFool"));

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: " + host,
                "  algorithm: HS256",
                "  encoding: plain",
                "  anonymous: false",
                "  key: secretFool");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();

        synFilter.doFilter(request, response, chain);
        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertEquals(username, requestCaptor.getValue().getUserPrincipal().getName());
        verify(request).getHeader("Authorization");

        for (final String role : finalRoles) {
            assertTrue(requestCaptor.getValue().isUserInRole(role));
        }
    }

    @Test
    public void allowGetWithoutToken() throws Exception {
        final String servername = "anon-test.com";
        final String host = "http://" + servername;
        final String username = "anonymous";
        final List<String> finalRoles = Arrays.asList("islandora", "anonymous", host);

        when(request.getMethod()).thenReturn("GET");
        when(request.getServerName()).thenReturn(servername);

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: " + host,
                "  algorithm: HS256",
                "  encoding: plain",
                "  anonymous: true",
                "  key: secretFool");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();
        synFilter.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertEquals(username, requestCaptor.getValue().getUserPrincipal().getName());
        for (final String role : finalRoles) {
            assertTrue(requestCaptor.getValue().isUserInRole(role));
        }
    }

    @Test
    public void allowHeadWithoutToken() throws Exception {
        final String servername = "anon-test.com";
        final String host = "http://" + servername;
        final String username = "anonymous";
        final List<String> finalRoles = Arrays.asList("islandora", "anonymous", host);

        when(request.getMethod()).thenReturn("HEAD");
        when(request.getServerName()).thenReturn(servername);

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: " + host,
                "  algorithm: HS256",
                "  encoding: plain",
                "  anonymous: true",
                "  key: secretFool");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();
        synFilter.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertEquals(username, requestCaptor.getValue().getUserPrincipal().getName());
        for (final String role : finalRoles) {
            assertTrue(requestCaptor.getValue().isUserInRole(role));
        }
    }

    @Test
    public void disallowGetWithoutToken() throws Exception {
        final String servername = "anon-test.com";
        final String nohost = "http://other-site.com";

        when(request.getMethod()).thenReturn("GET");
        when(request.getServerName()).thenReturn(servername);

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: " + nohost,
                "  algorithm: HS256",
                "  encoding: plain",
                "  key: secretFool");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();
        synFilter.doFilter(request, response, chain);

        verify(request).getHeader("Authorization");
        verify(response).sendError(SC_UNAUTHORIZED, SynFilter.UNAUTHORIZED_MSG);
    }

    @Test
    public void overrideDefaultAllow() throws Exception {
        final String servername = "anon-test.com";
        final String host = "http://" + servername;
        final String username = "normalUser123";
        final List<String> finalRoles = Arrays.asList("islandora", host);
        final String token = JWT
                .create()
                .withClaim("webid", 1)
                .withClaim("sub", username)
                .withClaim("iss", host)
                .withArrayClaim("roles", new String[] {})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secretFool"));

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: " + host,
                "  algorithm: HS256",
                "  encoding: plain",
                "  anonymous: false",
                "  key: secretFool",
                "site:",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: true",
                "  default: true");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();
        synFilter.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertEquals(username, requestCaptor.getValue().getUserPrincipal().getName());
        for (final String role : finalRoles) {
            assertTrue(requestCaptor.getValue().isUserInRole(role));
        }
    }

    @Test
    public void overrideDefaultAllowAndFail() throws Exception {
        final String servername = "anon-test.com";
        final String host = "http://" + servername;
        final String username = "normalUser123";
        final String token = JWT
                .create()
                .withClaim("webid", 1)
                .withClaim("sub", username)
                .withClaim("iss", host)
                .withArrayClaim("roles", new String[] {})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("whatIsIt"));

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: " + host,
                "  algorithm: HS256",
                "  encoding: plain",
                "  anonymous: false",
                "  key: secretFool",
                "site:",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: true",
                "  default: true");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();
        synFilter.doFilter(request, response, chain);

        verify(request).getHeader("Authorization");
        verify(response).sendError(SC_FORBIDDEN, SynFilter.FORBIDDEN_MSG);
    }

    @Test
    public void overrideDefaultDeny() throws Exception {
        final String servername = "anon-test.com";
        final String host = "http://" + servername;
        final String username = "anonymous";
        final List<String> finalRoles = Arrays.asList("islandora", "anonymous", host);

        when(request.getMethod()).thenReturn("GET");
        when(request.getServerName()).thenReturn(servername);

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: " + host,
                "  algorithm: HS256",
                "  encoding: plain",
                "  anonymous: true",
                "  key: secretFool",
                "site:",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: false",
                "  default: true");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();
        synFilter.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertEquals(username, requestCaptor.getValue().getUserPrincipal().getName());
        for (final String role : finalRoles) {
            assertTrue(requestCaptor.getValue().isUserInRole(role));
        }
    }

    @Test
    public void defaultAndSiteAllowed() throws Exception {
        final String servername = "anon-test.com";
        final String host = "http://" + servername;
        final String username = "anonymous";
        final List<String> finalRoles = Arrays.asList("islandora", "anonymous", host);

        when(request.getMethod()).thenReturn("GET");
        when(request.getServerName()).thenReturn(servername);

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  algorithm: RS256",
                "  encoding: plain",
                "  anonymous: true",
                "  default: true");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();
        synFilter.doFilter(request, response, chain);

        verify(chain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        assertEquals(username, requestCaptor.getValue().getUserPrincipal().getName());
        for (final String role : finalRoles) {
            assertTrue(requestCaptor.getValue().isUserInRole(role));
        }
    }

    @Test
    public void shouldFailSignatureVerification() throws Exception {
        final String host = "http://test.com";
        final String token = JWT
                .create()
                .withClaim("webid", 1)
                .withClaim("sub", "normalUser")
                .withClaim("iss", host)
                .withArrayClaim("roles", new String[] {})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret"));

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token + "s");

        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: " + host,
                "  algorithm: HS256",
                "  encoding: plain");
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synFilter = createFilter();
        synFilter.doFilter(request, response, chain);

        verify(request).getHeader("Authorization");
        verify(response).sendError(SC_UNAUTHORIZED, SynFilter.UNAUTHORIZED_MSG);
    }

    private void createSettings(final File settingsFile) throws Exception {
        final String testXml = String.join("\n",
                "---",
                "version: 1",
                "site:",
                "  url: http://test.com",
                "  algorithm: HS256",
                "  encoding: plain",
                "  key: secret",
                "site:",
                "  algorithm: HS256",
                "  encoding: plain",
                "  default: true",
                "  key: secret2",
                "token:",
                "  value: 1337");
        Files.write(Paths.get(settingsFile.getAbsolutePath()), testXml.getBytes());
    }

}
