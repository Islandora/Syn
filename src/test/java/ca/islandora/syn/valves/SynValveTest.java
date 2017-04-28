package ca.islandora.syn.valves;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import ca.islandora.syn.valve.SynValve;

@RunWith(MockitoJUnitRunner.class)
public class SynValveTest {
    private SynValve synValve;
    private File settings;

    @Mock
    private Container container;

    @Mock
    private Realm realm;

    @Mock
    private Context context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private Valve nextValve;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private Host mockHost;

    private static ZoneOffset offset;

    @Before
    public void setUp() throws Exception {
        settings = temporaryFolder.newFile();
        createSettings(settings);

        synValve = new SynValve();
        synValve.setPathname(settings.getAbsolutePath());
        synValve.setContainer(container);
        synValve.setNext(nextValve);

        when(container.getRealm()).thenReturn(realm);
        when(request.getContext()).thenReturn(context);
        when(request.getMethod()).thenReturn("POST");
        offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    }

    @Test
    public void shouldInvokeNextValveWithoutAuth() throws Exception {
        when(realm.findSecurityConstraints(request, request.getContext()))
                .thenReturn(null);

        synValve.start();
        synValve.invoke(request, response);

        verify(nextValve).invoke(request, response);
    }

    @Test
    public void shouldPassAuth() throws Exception {
        final ArgumentCaptor<GenericPrincipal> argument = ArgumentCaptor.forClass(GenericPrincipal.class);
        final String host = "http://test.com";

        final String token = "Bearer " + JWT
                .create()
                .withClaim("uid", 1)
                .withClaim("name", "adminuser")
                .withClaim("url", host)
                .withArrayClaim("roles", new String[] {"role1", "role2", "role3"})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret"));

        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
                .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getHeader("Authorization"))
                .thenReturn(token);

        synValve.start();
        synValve.invoke(request, response);

        final InOrder inOrder = inOrder(request, nextValve);
        inOrder.verify(request).getHeader("Authorization");
        inOrder.verify(request).setUserPrincipal(argument.capture());
        inOrder.verify(request).setAuthType("SYN");
        inOrder.verify(nextValve).invoke(request, response);

        assertEquals("adminuser", argument.getValue().getName());
        final List<String> roles = Arrays.asList(argument.getValue().getRoles());
        assertEquals(5, roles.size());
        assertTrue(roles.contains("role1"));
        assertTrue(roles.contains("role2"));
        assertTrue(roles.contains("role3"));
        assertTrue(roles.contains("islandora"));
        assertTrue(roles.contains("http://test.com"));
        assertNull(argument.getValue().getPassword());
    }

    @Test
    public void shouldPassAuthToken() throws Exception {
        final ArgumentCaptor<GenericPrincipal> argument = ArgumentCaptor.forClass(GenericPrincipal.class);
        final String token = "Bearer 1337";
        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
                .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getHeader("Authorization"))
                .thenReturn(token);

        synValve.start();
        synValve.invoke(request, response);

        final InOrder inOrder = inOrder(request, nextValve);
        inOrder.verify(request).getHeader("Authorization");
        inOrder.verify(request).setUserPrincipal(argument.capture());
        inOrder.verify(request).setAuthType("SYN");
        inOrder.verify(nextValve).invoke(request, response);

        assertEquals("islandoraAdmin", argument.getValue().getName());
        final List<String> roles = Arrays.asList(argument.getValue().getRoles());
        assertEquals(1, roles.size());
        assertTrue(roles.contains("islandora"));
        assertNull(argument.getValue().getPassword());
    }

    @Test
    public void shouldFailAuthBecauseOfTokenNotSet() throws Exception {
        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
                .thenReturn(new SecurityConstraint[] { securityConstraint });

        synValve.start();
        synValve.invoke(request, response);

        verify(request).getHeader("Authorization");
        verify(response).sendError(401, "Token authentication failed.");
    }

    @Test
    public void shouldFailAuthBecauseOfTokenInvalid1() throws Exception {
        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
                .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getHeader("Authorization"))
                .thenReturn("garbage");

        synValve.start();
        synValve.invoke(request, response);

        verify(request).getHeader("Authorization");
        verify(response).sendError(401, "Token authentication failed.");
    }

    @Test
    public void shouldFailAuthBecauseOfTokenInvalid2() throws Exception {
        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
                .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getHeader("Authorization"))
                .thenReturn("killer bandit foo");

        synValve.start();
        synValve.invoke(request, response);

        verify(request).getHeader("Authorization");
        verify(response).sendError(401, "Token authentication failed.");
    }

    @Test
    public void shouldFailTokenMissingUid() throws Exception {
        final String host = "http://test.com";
        final String token = JWT
                .create()
                .withClaim("name", "adminuser")
                .withClaim("url", host)
                .withArrayClaim("roles", new String[] {"role1", "role2", "role3"})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret"));

        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
                .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        synValve.start();
        synValve.invoke(request, response);

        verify(request).getHeader("Authorization");
        verify(response).sendError(401, "Token authentication failed.");
    }

    @Test
    public void shouldPassAuthDefaultSite() throws Exception {
        final String host = "http://test2.com";
        final String token = JWT
                .create()
                .withClaim("uid", 1)
                .withClaim("name", "normalUser")
                .withClaim("url", host)
                .withArrayClaim("roles", new String[] {})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret2"));

        final ArgumentCaptor<GenericPrincipal> argument = ArgumentCaptor.forClass(GenericPrincipal.class);

        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
                .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        synValve.start();
        synValve.invoke(request, response);

        final InOrder inOrder = inOrder(request, nextValve);
        inOrder.verify(request).getHeader("Authorization");
        inOrder.verify(request).setUserPrincipal(argument.capture());
        inOrder.verify(request).setAuthType("SYN");
        inOrder.verify(nextValve).invoke(request, response);

        assertEquals("normalUser", argument.getValue().getName());
        final List<String> roles = Arrays.asList(argument.getValue().getRoles());
        assertEquals(2, roles.size());
        assertTrue(roles.contains("islandora"));
        assertTrue(roles.contains("http://test2.com"));
        assertNull(argument.getValue().getPassword());
    }

    @Test
    public void shouldFailAuthBecauseNoSiteMatch() throws Exception {
        final String host = "http://test-no-match.com";
        final String token = JWT
                .create()
                .withClaim("uid", 1)
                .withClaim("name", "normalUser")
                .withClaim("url", host)
                .withArrayClaim("roles", new String[] {})
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret"));

        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
                .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        final String testXml = String.join("\n"
                , "<config version='1'>"
                , "  <site url='http://test.com' algorithm='HS256' encoding='plain'>"
                , "secret"
                , "  </site>"
                , "</config>"
        );
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synValve.start();
        synValve.invoke(request, response);

        verify(request).getHeader("Authorization");
        verify(response).sendError(401, "Token authentication failed.");
    }

    @Test
    public void allowAuthWithToken() throws Exception {
        final String host = "http://anon-test.com";
        final String token = JWT
            .create()
            .withClaim("uid", 1)
            .withClaim("name", "normalUser")
            .withClaim("url", host)
            .withArrayClaim("roles", new String[] {})
            .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
            .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
            .sign(Algorithm.HMAC256("secretFool"));

        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
            .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getHeader("Authorization"))
            .thenReturn("Bearer " + token);

        final ArgumentCaptor<GenericPrincipal> argument = ArgumentCaptor.forClass(GenericPrincipal.class);

        final String testXml = String.join("\n"
            , "<config version='1'>"
            , "  <site url='" + host + "' algorithm='HS256' encoding='plain' anonymous='true'>"
            , "secretFool"
            , "  </site>"
            , "</config>"
        );
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synValve.start();
        synValve.invoke(request, response);

        final InOrder inOrder = inOrder(request, nextValve);
        inOrder.verify(request).getHeader("Authorization");
        inOrder.verify(request).setUserPrincipal(argument.capture());
        inOrder.verify(request).setAuthType("SYN");
        inOrder.verify(nextValve).invoke(request, response);

        assertEquals("normalUser", argument.getValue().getName());
        final List<String> roles = Arrays.asList(argument.getValue().getRoles());
        assertEquals(2, roles.size());
        assertTrue(roles.contains("islandora"));
        assertTrue(roles.contains(host));
        assertNull(argument.getValue().getPassword());
    }

    @Test
    public void allowGetWithoutToken() throws Exception {
        final String host = "http://anon-test.com";
        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
            .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getMethod()).thenReturn("GET");
        when(mockHost.toString()).thenReturn(host);
        when(request.getHost()).thenReturn(mockHost);

        final ArgumentCaptor<GenericPrincipal> argument = ArgumentCaptor.forClass(GenericPrincipal.class);

        final String testXml = String.join("\n"
            , "<config version='1'>"
            , "  <site url='" + host + "' algorithm='HS256' encoding='plain' anonymous='true'>"
            , "secretFool"
            , "  </site>"
            , "</config>"
        );
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synValve.start();
        synValve.invoke(request, response);

        final InOrder inOrder = inOrder(request, nextValve);
        inOrder.verify(request).setUserPrincipal(argument.capture());
        inOrder.verify(nextValve).invoke(request, response);

        assertEquals("anonymous", argument.getValue().getName());
        final List<String> roles = Arrays.asList(argument.getValue().getRoles());
        assertEquals(2, roles.size());
        assertTrue(roles.contains("anonymous"));
        assertTrue(roles.contains("islandora"));
        assertNull(argument.getValue().getPassword());
    }

    @Test
    public void overrideDefaultAllow() throws Exception {
        final String host = "http://anon-test.com";
        final String token = JWT
            .create()
            .withClaim("uid", 1)
            .withClaim("name", "normalUser")
            .withClaim("url", host)
            .withArrayClaim("roles", new String[] {})
            .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
            .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
            .sign(Algorithm.HMAC256("secretFool"));
        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
            .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getHeader("Authorization"))
            .thenReturn("Bearer " + token);
        when(request.getMethod()).thenReturn("GET");
        when(mockHost.toString()).thenReturn(host);
        when(request.getHost()).thenReturn(mockHost);

        final ArgumentCaptor<GenericPrincipal> argument = ArgumentCaptor.forClass(GenericPrincipal.class);

        final String testXml = String.join("\n"
            , "<config version='1'>"
            , "  <site url='" + host + "' algorithm='HS256' encoding='plain' anonymous='false'>"
            , "secretFool"
            , "  </site>"
            , "  <site algorithm='RS256' encoding='plain' anonymous='true' default='true'/>"
            , "</config>"
        );
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synValve.start();
        synValve.invoke(request, response);

        final InOrder inOrder = inOrder(request, nextValve);
        inOrder.verify(request).setUserPrincipal(argument.capture());
        inOrder.verify(nextValve).invoke(request, response);

        assertEquals("normalUser", argument.getValue().getName());
        final List<String> roles = Arrays.asList(argument.getValue().getRoles());
        assertEquals(2, roles.size());
        assertTrue(roles.contains("islandora"));
        assertTrue(roles.contains(host));
        assertNull(argument.getValue().getPassword());
    }

    @Test
    public void overrideDefaultDeny() throws Exception {
        final String host = "http://anon-test.com";
        final SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthConstraint(true);
        when(realm.findSecurityConstraints(request, request.getContext()))
            .thenReturn(new SecurityConstraint[] { securityConstraint });
        when(request.getMethod()).thenReturn("GET");
        when(mockHost.toString()).thenReturn(host);
        when(request.getHost()).thenReturn(mockHost);

        final ArgumentCaptor<GenericPrincipal> argument = ArgumentCaptor.forClass(GenericPrincipal.class);

        final String testXml = String.join("\n"
            , "<config version='1'>"
            , "  <site url='" + host + "' algorithm='HS256' encoding='plain' anonymous='true'>"
            , "secretFool"
            , "  </site>"
            , "  <site algorithm='RS256' encoding='plain' anonymous='true' default='false'/>"
            , "</config>"
        );
        Files.write(Paths.get(this.settings.getAbsolutePath()), testXml.getBytes());

        synValve.start();
        synValve.invoke(request, response);

        final InOrder inOrder = inOrder(request, nextValve);
        inOrder.verify(request).setUserPrincipal(argument.capture());
        inOrder.verify(nextValve).invoke(request, response);

        assertEquals("anonymous", argument.getValue().getName());
        final List<String> roles = Arrays.asList(argument.getValue().getRoles());
        assertEquals(2, roles.size());
        assertTrue(roles.contains("anonymous"));
        assertTrue(roles.contains("islandora"));
        assertNull(argument.getValue().getPassword());
    }

    private void createSettings(final File settingsFile) throws Exception {
        final String testXml = String.join("\n"
                , "<config version='1'>"
                , "  <site url='http://test.com' algorithm='HS256' encoding='plain'>"
                , "secret"
                , "  </site>"
                , "  <site algorithm='HS256' encoding='plain' default='true'>"
                , "secret2"
                , "  </site>"
                , "  <token>"
                , "1337"
                , "  </token>"
                , "</config>"
        );
        Files.write(Paths.get(settingsFile.getAbsolutePath()), testXml.getBytes());
    }
}
