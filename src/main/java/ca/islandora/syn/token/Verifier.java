package ca.islandora.syn.token;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;

public class Verifier {

    private static final Logger log = getLogger(Verifier.class);
    private String token;
    private JWT jwt;

    private Verifier() {
    }

    public static Verifier create(final String token) {
        final Verifier verifier = new Verifier();
        final List<String> requiredClaims = Arrays.asList("sub", "iss", "webid", "roles", "exp", "iat");
        verifier.token = token;
        try {
            verifier.jwt = JWT.decode(token);
            final Map<String, Claim> claims = verifier.jwt.getClaims();
            for (final String claim : requiredClaims) {
                if (claims.get(claim) == null) {
                    log.info("Token missing required claim ({})", claim);
                    throw new InvalidTokenException(
                            String.format("Token missing required claim ({})", claim));
                }
            }
        } catch (final JWTDecodeException exception) {
            log.error("Error decoding token: " + token, exception);
            throw new InvalidTokenException("Error decoding token: " + token, exception);
        }
        return verifier;
    }

    public int getUid() {
        return this.jwt.getClaim("webid").asInt();
    }

    public String getUrl() {
        return this.jwt.getClaim("iss").asString();
    }

    public String getName() {
        return this.jwt.getClaim("sub").asString();
    }

    public List<String> getRoles() {
        return this.jwt.getClaim("roles").asList(String.class);
    }

    public boolean verify(final Algorithm algorithm) {
        final JWTVerifier verifier = JWT.require(algorithm).build();
        try {
            verifier.verify(this.token);
        } catch (final JWTVerificationException exception) {
            return false;
        }

        return true;
    }

}
