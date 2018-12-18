package ca.islandora.syn.settings;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private int version = -1;
    private String header = "";
    private final List<Site> sites = new ArrayList<>();
    private final List<Token> tokens = new ArrayList<>();

    public void addSite(final Site site) {
        sites.add(site);
    }
    public List<Site> getSites() {
        return sites;
    }

    public int getVersion() {
        return this.version;
    }
    public void setVersion(final int version) {
        this.version = version;
    }

    public void addToken(final Token token) {
        tokens.add(token);
    }
    public List<Token> getTokens() {
        return tokens;
    }

    public void setHeader(final String header) {
        this.header = header;
    }

    public String getHeader() {
        return this.header;
    }
}
