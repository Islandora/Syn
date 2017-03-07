package ca.islandora.syn.settings;

public class Site {
    private String url = null;
    private String algorithm = null;
    private String key = null;
    private String path = null;
    private String encoding = null;
    private boolean defaultItem = false;

    public String getUrl() {
        return this.url;
    }
    public void setUrl(final String url) {
        this.url = url;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }
    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKey() {
        return this.key;
    }
    public void setKey(final String key) {
        this.key = key;
    }

    public String getPath() {
        return this.path;
    }
    public void setPath(final String path) {
        this.path = path;
    }

    public String getEncoding() {
        return this.encoding;
    }
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public boolean getDefault() {
        return this.defaultItem;
    }
    public void setDefault(final boolean defaultItem) {
        this.defaultItem = defaultItem;
    }
}
