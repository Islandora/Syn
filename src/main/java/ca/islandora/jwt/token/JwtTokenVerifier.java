package ca.islandora.jwt.token;

import java.util.List;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class JwtTokenVerifier {

	private static final Log log = LogFactory.getLog(JwtTokenVerifier.class);
	private String token;
	private JWT jwt;

	private JwtTokenVerifier() {}

	public static JwtTokenVerifier create(String token) {
		JwtTokenVerifier tokenVerifier = new JwtTokenVerifier();
		tokenVerifier.token = token;
		try {
            tokenVerifier.jwt = JWT.decode(token);
            if(tokenVerifier.jwt.getClaim("uid").isNull()) {
                return null;
            }

            if(tokenVerifier.jwt.getClaim("url").isNull()) {
                return null;
            }

            if(tokenVerifier.jwt.getClaim("name").isNull()) {
                return null;
            }

            if(tokenVerifier.jwt.getClaim("roles").isNull()) {
                return null;
            }

            if(tokenVerifier.jwt.getExpiresAt() == null) {
                return null;
            }

            if(tokenVerifier.jwt.getIssuedAt() == null) {
                return null;
            }
        }
        catch(JWTDecodeException exception) {
		    log.error("Error decoding token: " + token, exception);
		    return null;
        }
		return tokenVerifier;
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

    public boolean verify(Algorithm algorithm) {
	    JWTVerifier verifier = JWT.require(algorithm).build();
	    try {
            verifier.verify(this.token);
        }
        catch (JWTVerificationException exception) {
            return false;
        }

        return true;
    }
}
