package ca.islandora.syn.token;

/**
 * 
 * @author whikloj
 *
 */
public class InvalidTokenException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param message
     *        Exception message
     */
    public InvalidTokenException(final String message) {
        super(message);
    }

    /**
     * Constructor
     * 
     * @param message
     *        Exception message
     * @param e
     *        Wrapped Exception
     */
    public InvalidTokenException(final String message, final Throwable e) {
        super(message, e);
    }
}
