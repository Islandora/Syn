package ca.islandora.syn.settings;

/**
 * Exception while parsing the settings YAML file.
 * 
 * @author whikloj
 * @since 2018-01-17
 */
public class SettingsParserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param message
     *        Exception message
     */
    public SettingsParserException(final String message) {
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
    public SettingsParserException(final String message, final Throwable e) {
        super(message, e);
    }

}
