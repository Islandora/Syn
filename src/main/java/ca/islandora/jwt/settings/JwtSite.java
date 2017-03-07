package ca.islandora.jwt.settings;

class JwtSite {
    private String url = null;
    private String algorithm = null;
    private String key = null;
    private String path = null;
    private String encoding = null;
    private boolean defaultItem = false;

    String getUrl() {
        return this.url;
    }
    void setUrl(final String url) {
        this.url = url;
    }

    String getAlgorithm() {
        return this.algorithm;
    }
    void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    String getKey() {
        return this.key;
    }
    void setKey(final String key) {
        this.key = key;
    }

    String getPath() {
        return this.path;
    }
    void setPath(final String path) {
        this.path = path;
    }

    String getEncoding() {
        return this.encoding;
    }
    void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    boolean getDefault() {
        return this.defaultItem;
    }
    void setDefault(final boolean defaultItem) {
        this.defaultItem = defaultItem;
    }
}
