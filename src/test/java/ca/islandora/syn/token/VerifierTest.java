package ca.islandora.syn.token;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class VerifierTest {

    private static String token;

    private static ZoneOffset offset;

    @Before
    public void setUp() {
        offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    }

    @Test
    public void testClaimsWithoutVerify() {
        token = JWT.create()
                .withArrayClaim("roles", new String[] { "Role1", "Role2" })
                .withClaim("webid", 1)
                .withClaim("sub", "admin")
                .withClaim("iss", "http://test.com")
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.none());
        final Verifier verifier = Verifier.create(token);
        assertEquals(1, verifier.getUid());
        assertEquals("admin", verifier.getName());
        assertEquals("http://test.com", verifier.getUrl());
        final List<String> roles = verifier.getRoles();
        assertEquals(2, roles.size());
        assertEquals("Role1", roles.get(0));
        assertEquals("Role2", roles.get(1));
    }

    @Test
    public void testClaimsMissing() {
        token = JWT.create()
                .withClaim("sub", "admin")
                .withClaim("iss", "http://test.com")
                .sign(Algorithm.none());
        final Verifier verifier = Verifier.create(token);
        assertNull(verifier);
    }

    @Test
    public void testClaimsBad() {
        token = "gibberish";
        final Verifier verifier = Verifier.create(token);
        assertNull(verifier);
    }

    @Test
    public void testClaimsAndVerifyHmac() throws Exception {
        token = JWT.create()
                .withArrayClaim("roles", new String[] { "Role1", "Role2" })
                .withClaim("webid", 1)
                .withClaim("sub", "admin")
                .withClaim("iss", "http://test.com")
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret"));

        final Verifier verifier = Verifier.create(token);
        assertEquals(1, verifier.getUid());
        assertEquals("admin", verifier.getName());
        assertEquals("http://test.com", verifier.getUrl());
        final List<String> roles = verifier.getRoles();
        assertEquals(2, roles.size());
        assertEquals("Role1", roles.get(0));
        assertEquals("Role2", roles.get(1));

        assertTrue(verifier.verify(Algorithm.HMAC256("secret")));
        assertFalse(verifier.verify(Algorithm.HMAC256("wrong secret")));
    }

    @Test
    public void testClaimsAndVerifyRsa() throws Exception {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        final KeyPair pair = keyGen.generateKeyPair();
        final RSAKey privateKey = (RSAKey) pair.getPrivate();
        final RSAKey publicKey = (RSAKey) pair.getPublic();

        token = JWT.create()
                .withArrayClaim("roles", new String[] { "Role1", "Role2" })
                .withClaim("webid", 1)
                .withClaim("sub", "admin")
                .withClaim("iss", "http://test.com")
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(offset)))
                .sign(Algorithm.RSA512(privateKey));

        final Verifier verifier = Verifier.create(token);
        assertEquals(1, verifier.getUid());
        assertEquals("admin", verifier.getName());
        assertEquals("http://test.com", verifier.getUrl());
        final List<String> roles = verifier.getRoles();
        assertEquals(2, roles.size());
        assertEquals("Role1", roles.get(0));
        assertEquals("Role2", roles.get(1));

        assertTrue(verifier.verify(Algorithm.RSA512(publicKey)));
        assertFalse(verifier.verify(Algorithm.RSA512((RSAKey) keyGen.genKeyPair().getPublic())));
    }

    @Test
    public void testClaimsAndVerifyHmacBadIssueDate() throws Exception {
        token = JWT.create()
                .withArrayClaim("roles", new String[] { "Role1", "Role2" })
                .withClaim("webid", 1)
                .withClaim("sub", "admin")
                .withClaim("iss", "http://test.com")
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(offset)))
                .withExpiresAt(Date.from(LocalDateTime.now().minusHours(2).toInstant(offset)))
                .sign(Algorithm.HMAC256("secret"));

        final Verifier verifier = Verifier.create(token);
        assertEquals(1, verifier.getUid());
        assertEquals("admin", verifier.getName());
        assertEquals("http://test.com", verifier.getUrl());
        final List<String> roles = verifier.getRoles();
        assertEquals(2, roles.size());
        assertEquals("Role1", roles.get(0));
        assertEquals("Role2", roles.get(1));
        assertFalse(verifier.verify(Algorithm.HMAC256("secret")));
    }
}
