package ca.islandora.jwt.settings;

public class JwtSite {
    private String url = null;
    private String algorithm = null;
    private String key = null;
    private String path = null;
    private String encoding = null;
    private boolean defaultItem = false;

    public String getUrl() {
        return this.url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKey() {
        return this.key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getPath() {
        return this.path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public String getEncoding() {
        return this.encoding;
    }
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean getDefault() {
        return this.defaultItem;
    }
    public void setDefault(boolean defaultItem) {
        this.defaultItem = defaultItem;
    }
}
