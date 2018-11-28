package ca.islandora.syn.token;

import java.util.List;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class Verifier {

    private static final Log log = LogFactory.getLog(Verifier.class);
    private String token;
    private JWT jwt;

    private Verifier() { }

    public static Verifier create(final String token) {
        final Verifier verifier = new Verifier();
        verifier.token = token;
        try {
            verifier.jwt = JWT.decode(token);
            if (verifier.jwt.getClaim("uid").isNull()) {
                return null;
            }
            if (verifier.jwt.getClaim("url").isNull()) {
                return null;
            }
            if (verifier.jwt.getClaim("name").isNull()) {
                return null;
            }
            if (verifier.jwt.getClaim("roles").isNull()) {
                return null;
            }
            if (verifier.jwt.getExpiresAt() == null) {
                return null;
            }
            if (verifier.jwt.getIssuedAt() == null) {
                return null;
            }
        } catch (JWTDecodeException exception) {
            log.error("Error decoding token: " + token, exception);
            return null;
        }
        return verifier;
    }

    public int getUid() {
        return this.jwt.getClaim("uid").asInt();
    }

    public String getUrl() {
        return this.jwt.getClaim("url").asString();
    }

    public String getName() {
        return this.jwt.getClaim("name").asString();
    }

    public List<String> getRoles() {
        return this.jwt.getClaim("roles").asList(String.class);
    }

    public boolean verify(final Algorithm algorithm) {
        final JWTVerifier verifier = JWT.require(algorithm).build();
        try {
            verifier.verify(this.token);
        } catch (JWTVerificationException exception) {
            return false;
        }

        return true;
    }
}
