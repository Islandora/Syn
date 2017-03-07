package ca.islandora.jwt.token;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.Test;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

public class JwtTokenVerifierTest {

    private static String token;

    @Test
    public void testClaimsWithoutVerify() {
        token = JWT.create()
                .withArrayClaim("roles", new String[]{"Role1", "Role2"})
                .withClaim("uid", 1)
                .withClaim("name", "admin")
                .withClaim("url", "http://test.com")
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.UTC)))
                .sign(Algorithm.none());
        final JwtTokenVerifier verifier = JwtTokenVerifier.create(token);
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
                .withClaim("name", "admin")
                .withClaim("url", "http://test.com")
                .sign(Algorithm.none());
        final JwtTokenVerifier verifier = JwtTokenVerifier.create(token);
        assertNull(verifier);
    }

    @Test
    public void testClaimsBad() {
        token = "gibberish";
        final JwtTokenVerifier verifier = JwtTokenVerifier.create(token);
        assertNull(verifier);
    }

    @Test
    public void testClaimsAndVerifyHmac() throws Exception {
        token = JWT.create()
                .withArrayClaim("roles", new String[]{"Role1", "Role2"})
                .withClaim("uid", 1)
                .withClaim("name", "admin")
                .withClaim("url", "http://test.com")
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.UTC)))
                .sign(Algorithm.HMAC256("secret"));

        final JwtTokenVerifier verifier = JwtTokenVerifier.create(token);
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
                .withArrayClaim("roles", new String[]{"Role1", "Role2"})
                .withClaim("uid", 1)
                .withClaim("name", "admin")
                .withClaim("url", "http://test.com")
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
                .withExpiresAt(Date.from(LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.UTC)))
                .sign(Algorithm.RSA512(privateKey));

        final JwtTokenVerifier verifier = JwtTokenVerifier.create(token);
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
                .withArrayClaim("roles", new String[]{"Role1", "Role2"})
                .withClaim("uid", 1)
                .withClaim("name", "admin")
                .withClaim("url", "http://test.com")
                .withIssuedAt(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
                .withExpiresAt(Date.from(LocalDateTime.now().minusHours(2).toInstant(ZoneOffset.UTC)))
                .sign(Algorithm.HMAC256("secret"));

        final JwtTokenVerifier verifier = JwtTokenVerifier.create(token);
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
